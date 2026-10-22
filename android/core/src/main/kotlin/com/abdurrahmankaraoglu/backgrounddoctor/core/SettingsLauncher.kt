package com.abdurrahmankaraoglu.backgrounddoctor.core

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Opens settings screens. Every candidate is checked with `resolveActivity` before it is
 * started, and `SecurityException` / `ActivityNotFoundException` move on to the next one.
 * The last resorts are the app details screen and then the main Settings screen.
 */
class SettingsLauncher(
    private val context: Context,
    private val oemIntents: List<OemIntent> = KnownOemIntents.forBrand(Build.MANUFACTURER.orEmpty(), Build.BRAND.orEmpty()),
) {
    internal data class Candidate(val intent: Intent, val label: String, val fallback: Boolean)

    fun open(action: DoctorAction, channelId: String? = null, activity: Activity? = null): LaunchResult {
        val notes = mutableListOf<String>()
        val candidates = candidates(action, channelId, notes) + genericFallbacks()
        return launch(candidates, activity ?: context, notes)
    }

    private val packageUri: Uri get() = Uri.parse("package:${context.packageName}")

    internal fun candidates(action: DoctorAction, channelId: String?, notes: MutableList<String>): List<Candidate> {
        val sdk = Build.VERSION.SDK_INT
        val pkg = context.packageName
        fun primary(intent: Intent, label: String) = Candidate(intent, label, fallback = false)
        val batteryList = Candidate(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, fallback = false)
        val appNotifications = if (sdk >= Build.VERSION_CODES.O) {
            primary(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
                Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            )
        } else {
            // Undocumented extras understood by Android 5-7 Settings.
            primary(
                Intent("android.settings.APP_NOTIFICATION_SETTINGS")
                    .putExtra("app_package", pkg)
                    .putExtra("app_uid", context.applicationInfo.uid),
                "android.settings.APP_NOTIFICATION_SETTINGS",
            )
        }

        return when (action) {
            DoctorAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                if (Permissions.declares(context, REQUEST_IGNORE_PERMISSION)) {
                    listOf(
                        primary(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri), Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS),
                        batteryList.copy(fallback = true),
                    )
                } else {
                    notes += "$REQUEST_IGNORE_PERMISSION isn't declared in the manifest; opened the exemption list instead."
                    listOf(batteryList.copy(fallback = true))
                }
            }
            DoctorAction.BATTERY_OPTIMIZATION_SETTINGS -> listOf(batteryList)
            DoctorAction.APP_DETAILS -> listOf(primary(appDetailsIntent(), Settings.ACTION_APPLICATION_DETAILS_SETTINGS))
            DoctorAction.NOTIFICATION_SETTINGS -> listOf(appNotifications)
            DoctorAction.CHANNEL_SETTINGS -> if (sdk >= Build.VERSION_CODES.O && !channelId.isNullOrEmpty()) {
                listOf(
                    primary(
                        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, pkg)
                            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId),
                        Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,
                    ),
                    appNotifications.copy(fallback = true),
                )
            } else {
                listOf(appNotifications.copy(fallback = true))
            }
            DoctorAction.EXACT_ALARM_SETTINGS -> if (sdk >= Build.VERSION_CODES.S) {
                listOf(primary(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri), Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            } else {
                emptyList()
            }
            DoctorAction.FULL_SCREEN_INTENT_SETTINGS -> if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listOf(primary(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri), Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
            } else {
                emptyList()
            }
            DoctorAction.DATA_SAVER_SETTINGS -> listOf(
                primary(Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS, packageUri), Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS),
            )
            DoctorAction.BATTERY_SAVER_SETTINGS -> listOf(primary(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), Settings.ACTION_BATTERY_SAVER_SETTINGS))
            DoctorAction.OEM_AUTOSTART -> oemCandidates(OemIntentKind.AUTOSTART)
            DoctorAction.OEM_BATTERY -> oemCandidates(OemIntentKind.BATTERY) + batteryList.copy(fallback = true)
        }
    }

    private fun oemCandidates(kind: OemIntentKind): List<Candidate> = oemIntents.filter { it.kind == kind }.map { entry ->
        val intent = if (entry.className != null && entry.packageName != null) {
            Intent().setComponent(ComponentName(entry.packageName, entry.className))
        } else {
            Intent(entry.action).apply { entry.packageName?.let { setPackage(it) } }
        }
        Candidate(intent, entry.target, fallback = false)
    }

    private fun appDetailsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)

    private fun genericFallbacks() = listOf(
        Candidate(appDetailsIntent(), Settings.ACTION_APPLICATION_DETAILS_SETTINGS, fallback = true),
        Candidate(Intent(Settings.ACTION_SETTINGS), Settings.ACTION_SETTINGS, fallback = true),
    )

    internal fun launch(candidates: List<Candidate>, starter: Context, notes: List<String>): LaunchResult {
        val attempts = notes.toMutableList()
        val pm = context.packageManager
        val tried = mutableSetOf<String>()
        for (candidate in candidates) {
            val key = candidate.intent.toUri(0)
            if (!tried.add(key)) continue
            val resolved = try {
                pm.resolveActivity(candidate.intent, PackageManager.MATCH_DEFAULT_ONLY)
            } catch (e: RuntimeException) {
                null
            }
            if (resolved == null) {
                attempts += "${candidate.label}: not found"
                continue
            }
            // Always a separate task: if a settings screen crashes (seen with
            // ACTION_BATTERY_SAVER_SETTINGS on the Android 16 emulator), the system also
            // finishes the activities below it in the same task, which would close the app.
            val intent = Intent(candidate.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                starter.startActivity(intent)
                attempts += "${candidate.label}: opened"
                return LaunchResult(true, candidate.fallback, candidate.label, attempts)
            } catch (e: SecurityException) {
                attempts += "${candidate.label}: SecurityException"
            } catch (e: ActivityNotFoundException) {
                attempts += "${candidate.label}: ActivityNotFoundException"
            } catch (e: RuntimeException) {
                attempts += "${candidate.label}: ${e.javaClass.simpleName}"
            }
        }
        return LaunchResult(false, false, null, attempts, "No settings screen could be opened.")
    }

    companion object {
        const val REQUEST_IGNORE_PERMISSION = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    }
}
