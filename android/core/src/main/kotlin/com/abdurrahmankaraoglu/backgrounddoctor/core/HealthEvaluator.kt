package com.abdurrahmankaraoglu.backgrounddoctor.core

/**
 * Raw readings taken from the system. A `null` field means the API doesn't exist on this
 * Android version or the value couldn't be read.
 */
data class DeviceSnapshot(
    val device: DeviceInfo,
    val ignoringBatteryOptimizations: Boolean?,
    val declaresRequestIgnoreBatteryOptimizations: Boolean,
    val standbyBucket: Int?,
    val backgroundRestricted: Boolean?,
    val restrictBackgroundStatus: Int?,
    val canScheduleExactAlarms: Boolean?,
    val declaresExactAlarm: Boolean,
    val notificationsEnabled: Boolean?,
    val channels: List<ChannelState>?,
    val canUseFullScreenIntent: Boolean?,
    val declaresFullScreenIntent: Boolean,
    val powerSaveMode: Boolean?,
    val deviceIdle: Boolean?,
    val lightDeviceIdle: Boolean?,
)

data class ChannelState(val id: String, val blocked: Boolean)

/** Turns a [DeviceSnapshot] into a [HealthReport]. Pure logic, no Android calls. */
object HealthEvaluator {
    // UsageStatsManager.STANDBY_BUCKET_* values.
    const val BUCKET_EXEMPTED = 5
    const val BUCKET_ACTIVE = 10
    const val BUCKET_WORKING_SET = 20
    const val BUCKET_FREQUENT = 30
    const val BUCKET_RARE = 40
    const val BUCKET_RESTRICTED = 45
    const val BUCKET_NEVER = 50

    // ConnectivityManager.RESTRICT_BACKGROUND_STATUS_* values.
    const val RESTRICT_BACKGROUND_DISABLED = 1
    const val RESTRICT_BACKGROUND_WHITELISTED = 2
    const val RESTRICT_BACKGROUND_ENABLED = 3

    fun evaluate(
        snapshot: DeviceSnapshot,
        oemIntents: List<OemIntent> = KnownOemIntents.forBrand(snapshot.device.manufacturer, snapshot.device.brand),
        nowMillis: Long = System.currentTimeMillis(),
    ): HealthReport = HealthReport(
        generatedAtMillis = nowMillis,
        device = snapshot.device,
        checks = listOf(
            batteryOptimization(snapshot),
            standbyBucket(snapshot.standbyBucket),
            backgroundRestriction(snapshot.backgroundRestricted),
            dataSaver(snapshot.restrictBackgroundStatus),
            exactAlarm(snapshot),
            notificationPermission(snapshot.notificationsEnabled),
            notificationChannels(snapshot),
            fullScreenIntent(snapshot),
            powerSave(snapshot.powerSaveMode),
            deviceIdle(snapshot),
            oemCheck(HealthCheckId.OEM_AUTOSTART, OemIntentKind.AUTOSTART, oemIntents, snapshot.device),
            oemCheck(HealthCheckId.OEM_BATTERY_MANAGER, OemIntentKind.BATTERY, oemIntents, snapshot.device),
        ),
    )

    private fun batteryOptimization(s: DeviceSnapshot): HealthCheck {
        val action = if (s.declaresRequestIgnoreBatteryOptimizations) {
            DoctorAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        } else {
            DoctorAction.BATTERY_OPTIMIZATION_SETTINGS
        }
        return when (s.ignoringBatteryOptimizations) {
            null -> HealthCheck(HealthCheckId.BATTERY_OPTIMIZATION, HealthStatus.UNKNOWN, detail = "PowerManager unavailable.", action = action)
            true -> HealthCheck(HealthCheckId.BATTERY_OPTIMIZATION, HealthStatus.OK, "true", "App is exempt from battery optimization.")
            false -> HealthCheck(
                HealthCheckId.BATTERY_OPTIMIZATION,
                HealthStatus.WARNING,
                "false",
                "Battery optimization applies to this app (the Android default). Doze can delay background work.",
                action,
            )
        }
    }

    fun bucketName(bucket: Int): String = when (bucket) {
        BUCKET_EXEMPTED -> "exempted"
        BUCKET_ACTIVE -> "active"
        BUCKET_WORKING_SET -> "workingSet"
        BUCKET_FREQUENT -> "frequent"
        BUCKET_RARE -> "rare"
        BUCKET_RESTRICTED -> "restricted"
        BUCKET_NEVER -> "never"
        else -> "unknown"
    }

    private fun standbyBucket(bucket: Int?): HealthCheck {
        val id = HealthCheckId.APP_STANDBY_BUCKET
        if (bucket == null) return HealthCheck(id, HealthStatus.UNSUPPORTED, detail = "App Standby buckets need Android 9 (API 28).")
        val value = "${bucketName(bucket)}($bucket)"
        return when {
            bucket <= BUCKET_WORKING_SET -> HealthCheck(id, HealthStatus.OK, value, "Jobs and alarms run with few limits.")
            bucket == BUCKET_FREQUENT -> HealthCheck(id, HealthStatus.WARNING, value, "Jobs and alarms are limited. Bucket rises when the user opens the app.")
            bucket in BUCKET_RARE..BUCKET_NEVER -> HealthCheck(
                id,
                HealthStatus.PROBLEM,
                value,
                "Jobs, alarms and network access are heavily limited until the user opens the app.",
                DoctorAction.APP_DETAILS,
            )
            else -> HealthCheck(id, HealthStatus.UNKNOWN, value, "Unrecognised bucket value.")
        }
    }

    private fun backgroundRestriction(restricted: Boolean?): HealthCheck {
        val id = HealthCheckId.BACKGROUND_RESTRICTION
        return when (restricted) {
            null -> HealthCheck(id, HealthStatus.UNSUPPORTED, detail = "Background restriction needs Android 9 (API 28).")
            false -> HealthCheck(id, HealthStatus.OK, "false", "User hasn't restricted background activity.")
            true -> HealthCheck(
                id,
                HealthStatus.PROBLEM,
                "true",
                "User restricted background activity: no background services, jobs or alarms.",
                DoctorAction.APP_DETAILS,
            )
        }
    }

    private fun dataSaver(status: Int?): HealthCheck {
        val id = HealthCheckId.DATA_SAVER
        return when (status) {
            null -> HealthCheck(id, HealthStatus.UNKNOWN, detail = "ConnectivityManager unavailable.")
            RESTRICT_BACKGROUND_DISABLED -> HealthCheck(id, HealthStatus.OK, "disabled", "Data Saver is off.")
            RESTRICT_BACKGROUND_WHITELISTED -> HealthCheck(id, HealthStatus.OK, "whitelisted", "Data Saver is on, but this app is allowed unrestricted data.")
            RESTRICT_BACKGROUND_ENABLED -> HealthCheck(
                id,
                HealthStatus.PROBLEM,
                "enabled",
                "Data Saver blocks background network for this app.",
                DoctorAction.DATA_SAVER_SETTINGS,
            )
            else -> HealthCheck(id, HealthStatus.UNKNOWN, status.toString(), "Unrecognised status.")
        }
    }

    private fun exactAlarm(s: DeviceSnapshot): HealthCheck {
        val id = HealthCheckId.EXACT_ALARM
        val can = s.canScheduleExactAlarms
            ?: return HealthCheck(id, HealthStatus.UNSUPPORTED, detail = "Exact alarms need no permission before Android 12 (API 31).")
        if (!s.declaresExactAlarm) {
            return HealthCheck(id, HealthStatus.NOT_APPLICABLE, can.toString(), "App doesn't declare SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM.")
        }
        return if (can) {
            HealthCheck(id, HealthStatus.OK, "true", "Exact alarms are allowed.")
        } else {
            HealthCheck(id, HealthStatus.PROBLEM, "false", "Exact alarms are denied; setExact*() will throw.", DoctorAction.EXACT_ALARM_SETTINGS)
        }
    }

    private fun notificationPermission(enabled: Boolean?): HealthCheck {
        val id = HealthCheckId.NOTIFICATION_PERMISSION
        return when (enabled) {
            null -> HealthCheck(id, HealthStatus.UNKNOWN, detail = "NotificationManager unavailable.", action = DoctorAction.NOTIFICATION_SETTINGS)
            true -> HealthCheck(id, HealthStatus.OK, "true", "Notifications are allowed.")
            false -> HealthCheck(
                id,
                HealthStatus.PROBLEM,
                "false",
                "Notifications are blocked. Foreground service notices and alerts won't show.",
                DoctorAction.NOTIFICATION_SETTINGS,
            )
        }
    }

    private fun notificationChannels(s: DeviceSnapshot): HealthCheck {
        val id = HealthCheckId.NOTIFICATION_CHANNELS
        val channels = s.channels
            ?: return HealthCheck(id, HealthStatus.UNSUPPORTED, detail = "Notification channels need Android 8 (API 26).")
        if (channels.isEmpty()) return HealthCheck(id, HealthStatus.NOT_APPLICABLE, "", "App has no notification channels yet.")
        val blocked = channels.filter { it.blocked }.map { it.id }
        return if (blocked.isEmpty()) {
            HealthCheck(id, HealthStatus.OK, "", "All ${channels.size} channel(s) are on.")
        } else {
            HealthCheck(
                id,
                HealthStatus.WARNING,
                blocked.joinToString(","),
                "${blocked.size} of ${channels.size} channel(s) are turned off.",
                DoctorAction.CHANNEL_SETTINGS,
            )
        }
    }

    private fun fullScreenIntent(s: DeviceSnapshot): HealthCheck {
        val id = HealthCheckId.FULL_SCREEN_INTENT
        val can = s.canUseFullScreenIntent
            ?: return HealthCheck(id, HealthStatus.UNSUPPORTED, detail = "Full-screen intent permission exists from Android 14 (API 34).")
        if (!s.declaresFullScreenIntent) {
            return HealthCheck(id, HealthStatus.NOT_APPLICABLE, can.toString(), "App doesn't declare USE_FULL_SCREEN_INTENT.")
        }
        return if (can) {
            HealthCheck(id, HealthStatus.OK, "true", "Full-screen notifications are allowed.")
        } else {
            HealthCheck(id, HealthStatus.WARNING, "false", "Full-screen notifications will show as normal heads-up notifications.", DoctorAction.FULL_SCREEN_INTENT_SETTINGS)
        }
    }

    private fun powerSave(on: Boolean?): HealthCheck {
        val id = HealthCheckId.POWER_SAVE_MODE
        return when (on) {
            null -> HealthCheck(id, HealthStatus.UNKNOWN, detail = "PowerManager unavailable.")
            false -> HealthCheck(id, HealthStatus.OK, "false", "Battery Saver is off.")
            true -> HealthCheck(id, HealthStatus.WARNING, "true", "Battery Saver is on; background work and location are throttled.", DoctorAction.BATTERY_SAVER_SETTINGS)
        }
    }

    private fun deviceIdle(s: DeviceSnapshot): HealthCheck {
        val id = HealthCheckId.DEVICE_IDLE
        return when {
            s.deviceIdle == null -> HealthCheck(id, HealthStatus.UNKNOWN, detail = "PowerManager unavailable.")
            s.deviceIdle -> HealthCheck(id, HealthStatus.WARNING, "deep", "Device is in Doze right now.")
            s.lightDeviceIdle == true -> HealthCheck(id, HealthStatus.WARNING, "light", "Device is in light Doze right now.")
            else -> HealthCheck(id, HealthStatus.OK, "false", "Device isn't in Doze. (It usually isn't while the app is open.)")
        }
    }

    private fun oemCheck(id: HealthCheckId, kind: OemIntentKind, intents: List<OemIntent>, device: DeviceInfo): HealthCheck {
        val action = if (kind == OemIntentKind.AUTOSTART) DoctorAction.OEM_AUTOSTART else DoctorAction.OEM_BATTERY
        val matching = intents.filter { it.kind == kind }
        if (matching.isEmpty()) {
            return HealthCheck(id, HealthStatus.NOT_APPLICABLE, detail = "No known ${kind.wireName} screen for ${device.manufacturer}.")
        }
        return HealthCheck(
            id,
            HealthStatus.UNKNOWN,
            detail = "${device.manufacturer} has a vendor ${kind.wireName} manager whose state can't be read through a public API. Ask the user to check it.",
            action = action,
        )
    }
}
