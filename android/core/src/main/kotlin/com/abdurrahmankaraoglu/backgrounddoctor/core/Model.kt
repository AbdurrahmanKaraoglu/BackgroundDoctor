package com.abdurrahmankaraoglu.backgrounddoctor.core

/** Result of a single check. `UNKNOWN` means the state can't be read; it is never a guess. */
enum class HealthStatus(val wireName: String) {
    OK("ok"),
    WARNING("warning"),
    PROBLEM("problem"),
    UNKNOWN("unknown"),
    UNSUPPORTED("unsupported"),
    NOT_APPLICABLE("notApplicable"),
}

/** Every check a [HealthReport] may contain. */
enum class HealthCheckId(val wireName: String) {
    BATTERY_OPTIMIZATION("batteryOptimization"),
    APP_STANDBY_BUCKET("appStandbyBucket"),
    BACKGROUND_RESTRICTION("backgroundRestriction"),
    DATA_SAVER("dataSaver"),
    EXACT_ALARM("exactAlarm"),
    NOTIFICATION_PERMISSION("notificationPermission"),
    NOTIFICATION_CHANNELS("notificationChannels"),
    FULL_SCREEN_INTENT("fullScreenIntent"),
    POWER_SAVE_MODE("powerSaveMode"),
    DEVICE_IDLE("deviceIdle"),
    OEM_AUTOSTART("oemAutostart"),
    OEM_BATTERY_MANAGER("oemBatteryManager"),
}

/** A settings screen the library knows how to open. */
enum class DoctorAction(val wireName: String) {
    REQUEST_IGNORE_BATTERY_OPTIMIZATIONS("requestIgnoreBatteryOptimizations"),
    BATTERY_OPTIMIZATION_SETTINGS("batteryOptimizationSettings"),
    APP_DETAILS("appDetails"),
    NOTIFICATION_SETTINGS("notificationSettings"),
    CHANNEL_SETTINGS("channelSettings"),
    EXACT_ALARM_SETTINGS("exactAlarmSettings"),
    FULL_SCREEN_INTENT_SETTINGS("fullScreenIntentSettings"),
    DATA_SAVER_SETTINGS("dataSaverSettings"),
    BATTERY_SAVER_SETTINGS("batterySaverSettings"),
    OEM_AUTOSTART("oemAutostart"),
    OEM_BATTERY("oemBattery"),
    ;

    companion object {
        fun fromWireName(name: String): DoctorAction? = entries.firstOrNull { it.wireName == name }
    }
}

data class HealthCheck(
    val id: HealthCheckId,
    val status: HealthStatus,
    /** Raw value read from the system, e.g. `"40"` for the standby bucket. */
    val value: String? = null,
    /** Short English explanation for logs and developers. */
    val detail: String? = null,
    val action: DoctorAction? = null,
)

/** Vendor skin running on the device. Detection is best effort and based on system properties. */
enum class OemUi(val wireName: String) {
    MIUI("miui"),
    HYPER_OS("hyperOs"),
    EMUI("emui"),
    MAGIC_OS("magicOs"),
    HARMONY_OS("harmonyOs"),
    COLOR_OS("colorOs"),
    REALME_UI("realmeUi"),
    OXYGEN_OS("oxygenOs"),
    FUNTOUCH_OS("funtouchOs"),
    ORIGIN_OS("originOs"),
    ONE_UI("oneUi"),
    FLYME("flyme"),
    STOCK("stock"),
    UNKNOWN("unknown"),
}

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val sdkInt: Int,
    val osVersion: String,
    val oemUi: OemUi,
    val oemUiVersion: String?,
)

data class HealthReport(
    val generatedAtMillis: Long,
    val device: DeviceInfo,
    val checks: List<HealthCheck>,
) {
    /** `PROBLEM` if any check has a problem, else `WARNING` if any warns, else `OK`. */
    val overall: HealthStatus
        get() = when {
            checks.any { it.status == HealthStatus.PROBLEM } -> HealthStatus.PROBLEM
            checks.any { it.status == HealthStatus.WARNING } -> HealthStatus.WARNING
            else -> HealthStatus.OK
        }

    fun check(id: HealthCheckId): HealthCheck? = checks.firstOrNull { it.id == id }
}

/** What happened when a settings screen was requested. */
data class LaunchResult(
    val opened: Boolean,
    /** True when the precise screen wasn't available and a generic one was opened instead. */
    val usedFallback: Boolean,
    /** Component (`package/class`) or action of the screen that opened. */
    val target: String?,
    /** Every candidate that was tried, in order, with the outcome. */
    val attempts: List<String>,
    val error: String? = null,
)
