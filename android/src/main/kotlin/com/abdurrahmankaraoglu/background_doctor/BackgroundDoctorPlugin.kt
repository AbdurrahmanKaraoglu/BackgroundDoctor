package com.abdurrahmankaraoglu.background_doctor

import android.app.Activity
import com.abdurrahmankaraoglu.backgrounddoctor.core.AppExitInfo
import com.abdurrahmankaraoglu.backgrounddoctor.core.BackgroundDoctor
import com.abdurrahmankaraoglu.backgrounddoctor.core.DoctorAction
import com.abdurrahmankaraoglu.backgrounddoctor.core.HealthReport
import com.abdurrahmankaraoglu.backgrounddoctor.core.KnownOemIntents
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thin Pigeon bridge over the Flutter-independent core library. */
class BackgroundDoctorPlugin : FlutterPlugin, ActivityAware, BackgroundDoctorHostApi {
    private var doctor: BackgroundDoctor? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        doctor = BackgroundDoctor(binding.applicationContext)
        BackgroundDoctorHostApi.setUp(binding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        BackgroundDoctorHostApi.setUp(binding.binaryMessenger, null)
        doctor = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    private fun requireDoctor(): BackgroundDoctor =
        doctor ?: throw FlutterError("not-attached", "BackgroundDoctor plugin isn't attached to an engine.")

    // Reading system properties and traces blocks, so both run off the main thread.
    override suspend fun getHealthReport(): HealthReportMessage {
        val doctor = requireDoctor()
        return withContext(Dispatchers.IO) { doctor.healthReport() }.toMessage()
    }

    override fun openSettings(action: String, channelId: String?): LaunchResultMessage {
        val doctorAction = DoctorAction.fromWireName(action)
            ?: throw FlutterError("unknown-action", "Unknown settings action: $action")
        val result = requireDoctor().open(doctorAction, channelId, activity)
        return LaunchResultMessage(result.opened, result.usedFallback, result.target, result.attempts, result.error)
    }

    override suspend fun getExitReasons(
        maxCount: Long,
        onlyUnseen: Boolean,
        markSeen: Boolean,
        includeTraces: Boolean,
        traceMaxBytes: Long,
    ): ExitReasonsMessage {
        val doctor = requireDoctor()
        val result = withContext(Dispatchers.IO) {
            doctor.exitReasons(
                maxCount.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                onlyUnseen,
                markSeen,
                includeTraces,
                traceMaxBytes.coerceIn(0, MAX_TRACE_BYTES).toInt(),
            )
        }
        return ExitReasonsMessage(result.supported, result.exits.map { it.toMessage() }, result.lastSeenMillis)
    }

    override fun setProcessStateSummary(summary: String): Boolean = try {
        requireDoctor().setProcessStateSummary(summary)
    } catch (e: IllegalArgumentException) {
        throw FlutterError("summary-too-long", e.message)
    }

    override fun markExitReasonsSeen() {
        requireDoctor().markExitReasonsSeen()
    }

    override fun getKnownOemIntents(): OemIntentTableMessage = OemIntentTableMessage(
        KnownOemIntents.VERSION,
        KnownOemIntents.all.map {
            OemIntentMessage(
                id = it.id,
                brands = it.brands.toList(),
                kind = it.kind.wireName,
                packageName = it.packageName,
                className = it.className,
                action = it.action,
                source = it.source,
                verification = it.verification.wireName,
                lastChecked = it.lastChecked,
                notes = it.notes,
            )
        },
    )

    private companion object {
        /** Upper bound for a single trace excerpt sent over the channel. */
        const val MAX_TRACE_BYTES = 256L * 1024
    }
}

private fun HealthReport.toMessage() = HealthReportMessage(
    generatedAtMillis = generatedAtMillis,
    device = DeviceInfoMessage(
        platform = "android",
        manufacturer = device.manufacturer,
        brand = device.brand,
        model = device.model,
        sdkInt = device.sdkInt.toLong(),
        osVersion = device.osVersion,
        oemUi = device.oemUi.wireName,
        oemUiVersion = device.oemUiVersion,
    ),
    checks = checks.map { HealthCheckMessage(it.id.wireName, it.status.wireName, it.value, it.detail, it.action?.wireName) },
)

private fun AppExitInfo.toMessage() = ExitInfoMessage(
    reasonCode = reasonCode.toLong(),
    reason = reason.wireName,
    category = reason.category.wireName,
    status = status.toLong(),
    importance = importance.toLong(),
    timestampMillis = timestampMillis,
    pid = pid.toLong(),
    processName = processName,
    exitDescription = description,
    pssKb = pssKb,
    rssKb = rssKb,
    stateSummary = stateSummary,
    traceExcerpt = traceExcerpt,
    traceTruncated = traceTruncated,
)
