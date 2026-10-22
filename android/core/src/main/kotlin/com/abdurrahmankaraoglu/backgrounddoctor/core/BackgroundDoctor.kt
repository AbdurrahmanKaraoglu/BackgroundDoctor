package com.abdurrahmankaraoglu.backgrounddoctor.core

import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.ApplicationExitInfo
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi

/**
 * Entry point of the core library. Works in any Android app; it has no Flutter dependency.
 *
 * ```kotlin
 * val doctor = BackgroundDoctor(context)
 * val report = doctor.healthReport()
 * doctor.open(DoctorAction.OEM_AUTOSTART, activity = this)
 * val exits = doctor.exitReasons(maxCount = 5, onlyUnseen = true, markSeen = true)
 * ```
 */
class BackgroundDoctor(context: Context) {
    private val context: Context = context.applicationContext ?: context

    fun deviceInfo(): DeviceInfo {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val (ui, version) = OemUiDetector.detect(manufacturer, brand, SystemProps::get)
        return DeviceInfo(manufacturer, brand, Build.MODEL.orEmpty(), Build.VERSION.SDK_INT, Build.VERSION.RELEASE.orEmpty(), ui, version)
    }

    fun snapshot(): DeviceSnapshot {
        val sdk = Build.VERSION.SDK_INT
        val pkg = context.packageName
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val notifications = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val declared = Permissions.requested(context)

        return DeviceSnapshot(
            device = deviceInfo(),
            ignoringBatteryOptimizations = safe { power?.isIgnoringBatteryOptimizations(pkg) },
            declaresRequestIgnoreBatteryOptimizations = SettingsLauncher.REQUEST_IGNORE_PERMISSION in declared,
            standbyBucket = if (sdk >= Build.VERSION_CODES.P) {
                safe { (context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager)?.appStandbyBucket }
            } else {
                null
            },
            backgroundRestricted = if (sdk >= Build.VERSION_CODES.P) safe { activityManager?.isBackgroundRestricted } else null,
            restrictBackgroundStatus = safe { connectivity?.restrictBackgroundStatus },
            canScheduleExactAlarms = if (sdk >= Build.VERSION_CODES.S) safe { alarms?.canScheduleExactAlarms() } else null,
            declaresExactAlarm = "android.permission.SCHEDULE_EXACT_ALARM" in declared || "android.permission.USE_EXACT_ALARM" in declared,
            notificationsEnabled = safe { notifications?.areNotificationsEnabled() },
            channels = if (sdk >= Build.VERSION_CODES.O) safe { notifications?.let(::readChannels) } else null,
            canUseFullScreenIntent = if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) safe { notifications?.canUseFullScreenIntent() } else null,
            declaresFullScreenIntent = "android.permission.USE_FULL_SCREEN_INTENT" in declared,
            powerSaveMode = safe { power?.isPowerSaveMode },
            deviceIdle = safe { power?.isDeviceIdleMode },
            lightDeviceIdle = if (sdk >= Build.VERSION_CODES.TIRAMISU) safe { power?.isDeviceLightIdleMode } else null,
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readChannels(nm: NotificationManager): List<ChannelState> {
        val blockedGroups = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            nm.notificationChannelGroups.filter { it.isBlocked }.map { it.id }.toSet()
        } else {
            emptySet()
        }
        return nm.notificationChannels.map { channel ->
            ChannelState(channel.id, channel.importance == NotificationManager.IMPORTANCE_NONE || channel.group in blockedGroups)
        }
    }

    fun healthReport(): HealthReport {
        val snapshot = snapshot()
        return HealthEvaluator.evaluate(snapshot, KnownOemIntents.forBrand(snapshot.device.manufacturer, snapshot.device.brand))
    }

    /** Opens the screen for [action] in its own task. [activity] is used to start it when given. */
    fun open(action: DoctorAction, channelId: String? = null, activity: Activity? = null): LaunchResult =
        SettingsLauncher(context).open(action, channelId, activity)

    /**
     * Recent process exits of this app, newest first. Returns `supported = false` below
     * Android 11 (API 30).
     */
    fun exitReasons(
        maxCount: Int = 10,
        onlyUnseen: Boolean = false,
        markSeen: Boolean = false,
        includeTraces: Boolean = false,
        traceMaxBytes: Int = 8 * 1024,
    ): ExitReasonsResult {
        val lastSeen = seenStore().takeIf { it.contains(KEY_LAST_SEEN) }?.getLong(KEY_LAST_SEEN, 0L)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return ExitReasonsResult(false, emptyList(), lastSeen)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return ExitReasonsResult(true, emptyList(), lastSeen)
        val raw = am.getHistoricalProcessExitReasons(context.packageName, 0, maxCount.coerceAtLeast(0))
        val exits = raw.map { toExitInfo(it, includeTraces, traceMaxBytes) }
        if (markSeen) raw.maxOfOrNull { it.timestamp }?.let(::storeLastSeen)
        return ExitReasonsResult(true, ExitTraces.filterUnseen(exits, lastSeen, onlyUnseen), lastSeen)
    }

    /** Marks every exit recorded so far as seen. */
    fun markExitReasonsSeen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        am.getHistoricalProcessExitReasons(context.packageName, 0, 1).firstOrNull()?.let { storeLastSeen(it.timestamp) }
    }

    /**
     * Stores a label that comes back as [AppExitInfo.stateSummary] after this process dies.
     * Returns false below Android 11. Throws [IllegalArgumentException] above 128 UTF-8 bytes.
     */
    fun setProcessStateSummary(summary: String): Boolean {
        val bytes = ExitTraces.encodeStateSummary(summary)
            ?: throw IllegalArgumentException("State summary is longer than ${ExitTraces.MAX_STATE_SUMMARY_BYTES} UTF-8 bytes.")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        am.setProcessStateSummary(bytes)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun toExitInfo(info: ApplicationExitInfo, includeTraces: Boolean, traceMaxBytes: Int): AppExitInfo {
        var excerpt: String? = null
        var truncated = false
        val reason = ExitReason.fromCode(info.reason)
        if (includeTraces && (reason == ExitReason.ANR || reason == ExitReason.CRASH_NATIVE)) {
            safe { info.traceInputStream }?.let { stream ->
                safe { ExitTraces.excerpt(stream, traceMaxBytes, binary = reason == ExitReason.CRASH_NATIVE) }?.let {
                    excerpt = it.first
                    truncated = it.second
                }
            }
        }
        return AppExitInfo(
            reasonCode = info.reason,
            reason = reason,
            status = info.status,
            importance = info.importance,
            timestampMillis = info.timestamp,
            pid = info.pid,
            processName = info.processName.orEmpty(),
            description = info.description,
            pssKb = info.pss,
            rssKb = info.rss,
            stateSummary = info.processStateSummary?.toString(Charsets.UTF_8),
            traceExcerpt = excerpt,
            traceTruncated = truncated,
        )
    }

    private fun seenStore() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun storeLastSeen(timestamp: Long) {
        val store = seenStore()
        if (timestamp > store.getLong(KEY_LAST_SEEN, Long.MIN_VALUE)) store.edit().putLong(KEY_LAST_SEEN, timestamp).apply()
    }

    private companion object {
        const val PREFS = "background_doctor"
        const val KEY_LAST_SEEN = "exit_reasons_last_seen"
    }
}

internal object Permissions {
    fun requested(context: Context): Set<String> = try {
        val pm = context.packageManager
        val info: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        info.requestedPermissions?.toSet().orEmpty()
    } catch (e: Exception) {
        emptySet()
    }

    fun declares(context: Context, permission: String): Boolean = permission in requested(context)
}

/** Reads all system properties once through `getprop`; no hidden API access. */
internal object SystemProps {
    private val props: Map<String, String> by lazy { read() }

    fun get(name: String): String? = props[name]

    private fun read(): Map<String, String> = try {
        val process = ProcessBuilder("getprop").redirectErrorStream(true).start()
        val regex = Regex("""^\[(.+?)]: \[(.*)]$""")
        process.inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull { regex.matchEntire(it.trim()) }.associate { it.groupValues[1] to it.groupValues[2] }
        }.also { process.destroy() }
    } catch (e: Exception) {
        emptyMap()
    }
}

private inline fun <T> safe(block: () -> T?): T? = try {
    block()
} catch (e: Exception) {
    null
}
