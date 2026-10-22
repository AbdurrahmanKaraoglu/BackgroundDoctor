package com.abdurrahmankaraoglu.backgrounddoctor.core

import android.app.Application
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsLauncherTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val xiaomi = KnownOemIntents.forBrand("xiaomi", "xiaomi")

    private fun register(intent: Intent, pkg: String = "com.android.settings", cls: String = "Screen") {
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = pkg
                name = cls
            }
        }
        shadowOf(app.packageManager).addResolveInfoForIntent(intent, info)
    }

    private fun appDetails() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))

    @Test
    fun nothingResolvableNeverCrashes() {
        val result = SettingsLauncher(app, xiaomi).open(DoctorAction.OEM_AUTOSTART)
        assertFalse(result.opened)
        assertNull(result.target)
        assertTrue(result.attempts.first(), result.attempts.first().endsWith("AutoStartManagementActivity: not found"))
        assertEquals(Settings.ACTION_SETTINGS + ": not found", result.attempts.last())
    }

    @Test
    fun missingVendorScreenFallsBackToAppDetails() {
        register(appDetails())
        val result = SettingsLauncher(app, xiaomi).open(DoctorAction.OEM_AUTOSTART)
        assertTrue(result.opened)
        assertTrue(result.usedFallback)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, result.target)
        val started = shadowOf(app).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, started.action)
        assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun vendorScreenOpensWhenPresent() {
        val component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
        register(Intent().setComponent(component), component.packageName, component.className)
        val result = SettingsLauncher(app, xiaomi).open(DoctorAction.OEM_AUTOSTART)
        assertTrue(result.opened)
        assertFalse(result.usedFallback)
        assertEquals(component, shadowOf(app).nextStartedActivity.component)
    }

    @Test
    fun securityExceptionMovesToNextCandidate() {
        val launcher = SettingsLauncher(app, xiaomi)
        val first = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        register(first)
        register(appDetails())
        var calls = 0
        val throwing = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent?) {
                calls++
                if (calls == 1) throw SecurityException("not exported")
                super.startActivity(intent)
            }
        }
        val result = launcher.launch(
            listOf(SettingsLauncher.Candidate(first, "battery", false), SettingsLauncher.Candidate(appDetails(), "details", true)),
            throwing,
            emptyList(),
        )
        assertTrue(result.opened)
        assertEquals("details", result.target)
        assertEquals(listOf("battery: SecurityException", "details: opened"), result.attempts)
    }

    @Test
    fun requestIgnoreWithoutPermissionOpensListAndSaysWhy() {
        register(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        val result = SettingsLauncher(app, emptyList()).open(DoctorAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        assertTrue(result.opened)
        assertTrue(result.usedFallback)
        assertTrue(result.attempts.first().contains("isn't declared"))
    }

    @Test
    fun channelSettingsCarryChannelId() {
        register(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS))
        val result = SettingsLauncher(app, emptyList()).open(DoctorAction.CHANNEL_SETTINGS, channelId = "alerts")
        assertTrue(result.opened)
        assertFalse(result.usedFallback)
        assertEquals("alerts", shadowOf(app).nextStartedActivity.getStringExtra(Settings.EXTRA_CHANNEL_ID))
    }
}
