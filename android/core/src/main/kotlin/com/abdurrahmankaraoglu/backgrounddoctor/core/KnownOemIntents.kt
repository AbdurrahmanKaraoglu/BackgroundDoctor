package com.abdurrahmankaraoglu.backgrounddoctor.core

enum class OemIntentKind(val wireName: String) {
    AUTOSTART("autostart"),
    BATTERY("battery"),
}

/** How far an entry has been checked. Nothing is device-verified yet. */
enum class Verification(val wireName: String) {
    /** Copied from a public source; not tried on a device by this project. */
    COMMUNITY_REPORTED("communityReported"),
    /** Opened on an emulator image of that vendor. */
    EMULATOR_VERIFIED("emulatorVerified"),
    /** Opened on a real device of that vendor. */
    DEVICE_VERIFIED("deviceVerified"),
}

/**
 * One vendor settings screen. Either [className] (explicit component) or [action] is set.
 * Entries are tried in table order and only launched if `resolveActivity` finds them.
 */
data class OemIntent(
    val id: String,
    /** Lower-case manufacturer or brand names this entry applies to. */
    val brands: Set<String>,
    val kind: OemIntentKind,
    val packageName: String?,
    val className: String?,
    val action: String? = null,
    val source: String,
    val verification: Verification = Verification.COMMUNITY_REPORTED,
    val lastChecked: String,
    val notes: String? = null,
) {
    val target: String get() = if (className != null) "$packageName/$className" else action.orEmpty()
}

/** Versioned table of known vendor autostart and battery screens. */
object KnownOemIntents {
    /** Bump when an entry is added, removed or re-verified. */
    const val VERSION = "2026.10.22"

    private const val AUTOSTARTER =
        "https://github.com/judemanutd/AutoStarter/blob/master/autostarter/src/main/java/com/judemanutd/autostarter/AutoStartPermissionHelper.kt"
    private const val CHECKED = "2026-10-22"

    private val XIAOMI = setOf("xiaomi", "redmi", "poco")
    private val HUAWEI = setOf("huawei", "honor")
    private val OPPO = setOf("oppo", "realme")
    private val VIVO = setOf("vivo", "iqoo")
    private const val REALME_NOTE = "realme shares ColorOS components with Oppo; not confirmed for realme UI."
    private const val HONOR_NOTE = "Honor devices made before the 2020 split run EMUI; newer MagicOS builds may differ."

    val all: List<OemIntent> = listOf(
        OemIntent("xiaomi_autostart", XIAOMI, OemIntentKind.AUTOSTART, "com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("letv_autostart", setOf("letv"), OemIntentKind.AUTOSTART, "com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("asus_autostart", setOf("asus"), OemIntentKind.AUTOSTART, "com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("asus_power_saver", setOf("asus"), OemIntentKind.BATTERY, "com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("huawei_startup", HUAWEI, OemIntentKind.AUTOSTART, "com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = HONOR_NOTE),
        OemIntent("huawei_protected_apps", HUAWEI, OemIntentKind.BATTERY, "com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = "Older EMUI only. " + HONOR_NOTE),
        OemIntent("oppo_startup_coloros", OPPO, OemIntentKind.AUTOSTART, "com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = REALME_NOTE),
        OemIntent("oppo_startup_legacy", OPPO, OemIntentKind.AUTOSTART, "com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = REALME_NOTE),
        OemIntent("oppo_startup_app", OPPO, OemIntentKind.AUTOSTART, "com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = REALME_NOTE),
        OemIntent("vivo_whitelist", VIVO, OemIntentKind.AUTOSTART, "com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("vivo_bg_startup", VIVO, OemIntentKind.AUTOSTART, "com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("vivo_bg_startup_legacy", VIVO, OemIntentKind.AUTOSTART, "com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("nokia_power_exceptions", setOf("nokia", "hmd global"), OemIntentKind.BATTERY, "com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity", source = AUTOSTARTER, lastChecked = CHECKED, notes = "Removed from Android 9 onward per dontkillmyapp.com."),
        OemIntent("samsung_battery_legacy", setOf("samsung"), OemIntentKind.BATTERY, "com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("samsung_battery_app_list", setOf("samsung"), OemIntentKind.BATTERY, "com.samsung.android.lool", "com.samsung.android.sm.battery.ui.usage.CheckableAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("samsung_battery", setOf("samsung"), OemIntentKind.BATTERY, "com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("oneplus_chain_launch", setOf("oneplus"), OemIntentKind.AUTOSTART, "com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity", source = AUTOSTARTER, lastChecked = CHECKED),
        OemIntent("oneplus_background_optimize", setOf("oneplus"), OemIntentKind.BATTERY, null, null, action = "com.android.settings.action.BACKGROUND_OPTIMIZE", source = AUTOSTARTER, lastChecked = CHECKED),
    )

    /** Entries whose brand set contains [manufacturer] or [brand] (case-insensitive). */
    fun forBrand(manufacturer: String, brand: String): List<OemIntent> {
        val keys = setOf(manufacturer.trim().lowercase(), brand.trim().lowercase())
        return all.filter { entry -> entry.brands.any { it in keys } }
    }

    /** Distinct package names to declare under `<queries>` for Android 11+ package visibility. */
    val packageNames: Set<String> get() = all.mapNotNull { it.packageName }.toSortedSet()
}
