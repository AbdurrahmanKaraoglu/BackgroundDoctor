package com.abdurrahmankaraoglu.backgrounddoctor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HealthEvaluatorTest {
    private val pixel = DeviceInfo("Google", "google", "Pixel 8", 35, "15", OemUi.STOCK, null)
    private val xiaomi = DeviceInfo("Xiaomi", "Redmi", "Note 12", 34, "14", OemUi.HYPER_OS, "OS1.0")

    private fun healthy(device: DeviceInfo = pixel) = DeviceSnapshot(
        device = device,
        ignoringBatteryOptimizations = true,
        declaresRequestIgnoreBatteryOptimizations = false,
        standbyBucket = HealthEvaluator.BUCKET_ACTIVE,
        backgroundRestricted = false,
        restrictBackgroundStatus = HealthEvaluator.RESTRICT_BACKGROUND_DISABLED,
        canScheduleExactAlarms = true,
        declaresExactAlarm = true,
        notificationsEnabled = true,
        channels = listOf(ChannelState("alerts", blocked = false)),
        canUseFullScreenIntent = true,
        declaresFullScreenIntent = true,
        powerSaveMode = false,
        deviceIdle = false,
        lightDeviceIdle = false,
    )

    private fun HealthReport.status(id: HealthCheckId) = check(id)!!.status

    @Test
    fun healthyPixelIsOk() {
        val report = HealthEvaluator.evaluate(healthy(), nowMillis = 42)
        assertEquals(HealthStatus.OK, report.overall)
        assertEquals(42L, report.generatedAtMillis)
        assertEquals(HealthCheckId.entries.size, report.checks.size)
        assertEquals(HealthStatus.NOT_APPLICABLE, report.status(HealthCheckId.OEM_AUTOSTART))
        assertEquals(HealthStatus.NOT_APPLICABLE, report.status(HealthCheckId.OEM_BATTERY_MANAGER))
    }

    @Test
    fun xiaomiAutostartIsUnknownNeverGuessed() {
        val report = HealthEvaluator.evaluate(healthy(xiaomi))
        val autostart = report.check(HealthCheckId.OEM_AUTOSTART)!!
        assertEquals(HealthStatus.UNKNOWN, autostart.status)
        assertEquals(DoctorAction.OEM_AUTOSTART, autostart.action)
        assertNull(autostart.value)
        // Unknown alone doesn't make the report fail.
        assertEquals(HealthStatus.OK, report.overall)
    }

    @Test
    fun batteryOptimizationRecommendsDirectRequestOnlyWhenDeclared() {
        val notDeclared = HealthEvaluator.evaluate(healthy().copy(ignoringBatteryOptimizations = false))
        assertEquals(HealthStatus.WARNING, notDeclared.status(HealthCheckId.BATTERY_OPTIMIZATION))
        assertEquals(DoctorAction.BATTERY_OPTIMIZATION_SETTINGS, notDeclared.check(HealthCheckId.BATTERY_OPTIMIZATION)!!.action)

        val declared = HealthEvaluator.evaluate(
            healthy().copy(ignoringBatteryOptimizations = false, declaresRequestIgnoreBatteryOptimizations = true),
        )
        assertEquals(DoctorAction.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, declared.check(HealthCheckId.BATTERY_OPTIMIZATION)!!.action)
        assertEquals(HealthStatus.WARNING, declared.overall)
    }

    @Test
    fun standbyBuckets() {
        fun status(bucket: Int?) = HealthEvaluator.evaluate(healthy().copy(standbyBucket = bucket)).status(HealthCheckId.APP_STANDBY_BUCKET)
        assertEquals(HealthStatus.OK, status(HealthEvaluator.BUCKET_EXEMPTED))
        assertEquals(HealthStatus.OK, status(HealthEvaluator.BUCKET_WORKING_SET))
        assertEquals(HealthStatus.WARNING, status(HealthEvaluator.BUCKET_FREQUENT))
        assertEquals(HealthStatus.PROBLEM, status(HealthEvaluator.BUCKET_RARE))
        assertEquals(HealthStatus.PROBLEM, status(HealthEvaluator.BUCKET_RESTRICTED))
        assertEquals(HealthStatus.UNKNOWN, status(99))
        assertEquals(HealthStatus.UNSUPPORTED, status(null))
        assertEquals("rare(40)", HealthEvaluator.evaluate(healthy().copy(standbyBucket = 40)).check(HealthCheckId.APP_STANDBY_BUCKET)!!.value)
    }

    @Test
    fun problemsWin() {
        val report = HealthEvaluator.evaluate(
            healthy().copy(
                backgroundRestricted = true,
                restrictBackgroundStatus = HealthEvaluator.RESTRICT_BACKGROUND_ENABLED,
                notificationsEnabled = false,
                powerSaveMode = true,
            ),
        )
        assertEquals(HealthStatus.PROBLEM, report.overall)
        assertEquals(DoctorAction.APP_DETAILS, report.check(HealthCheckId.BACKGROUND_RESTRICTION)!!.action)
        assertEquals(DoctorAction.DATA_SAVER_SETTINGS, report.check(HealthCheckId.DATA_SAVER)!!.action)
        assertEquals(DoctorAction.NOTIFICATION_SETTINGS, report.check(HealthCheckId.NOTIFICATION_PERMISSION)!!.action)
        assertEquals(HealthStatus.WARNING, report.status(HealthCheckId.POWER_SAVE_MODE))
    }

    @Test
    fun dataSaverWhitelistedIsOk() {
        val report = HealthEvaluator.evaluate(healthy().copy(restrictBackgroundStatus = HealthEvaluator.RESTRICT_BACKGROUND_WHITELISTED))
        assertEquals(HealthStatus.OK, report.status(HealthCheckId.DATA_SAVER))
    }

    @Test
    fun exactAlarmAndFullScreenDependOnDeclaration() {
        val denied = healthy().copy(canScheduleExactAlarms = false, canUseFullScreenIntent = false)
        assertEquals(HealthStatus.PROBLEM, HealthEvaluator.evaluate(denied).status(HealthCheckId.EXACT_ALARM))
        assertEquals(HealthStatus.WARNING, HealthEvaluator.evaluate(denied).status(HealthCheckId.FULL_SCREEN_INTENT))

        val undeclared = denied.copy(declaresExactAlarm = false, declaresFullScreenIntent = false)
        assertEquals(HealthStatus.NOT_APPLICABLE, HealthEvaluator.evaluate(undeclared).status(HealthCheckId.EXACT_ALARM))
        assertEquals(HealthStatus.NOT_APPLICABLE, HealthEvaluator.evaluate(undeclared).status(HealthCheckId.FULL_SCREEN_INTENT))

        val oldApi = healthy().copy(canScheduleExactAlarms = null, canUseFullScreenIntent = null)
        assertEquals(HealthStatus.UNSUPPORTED, HealthEvaluator.evaluate(oldApi).status(HealthCheckId.EXACT_ALARM))
        assertEquals(HealthStatus.UNSUPPORTED, HealthEvaluator.evaluate(oldApi).status(HealthCheckId.FULL_SCREEN_INTENT))
    }

    @Test
    fun blockedChannelsAreListed() {
        val report = HealthEvaluator.evaluate(
            healthy().copy(channels = listOf(ChannelState("a", false), ChannelState("b", true), ChannelState("c", true))),
        )
        val check = report.check(HealthCheckId.NOTIFICATION_CHANNELS)!!
        assertEquals(HealthStatus.WARNING, check.status)
        assertEquals("b,c", check.value)
        assertEquals(DoctorAction.CHANNEL_SETTINGS, check.action)
        assertEquals(
            HealthStatus.NOT_APPLICABLE,
            HealthEvaluator.evaluate(healthy().copy(channels = emptyList())).status(HealthCheckId.NOTIFICATION_CHANNELS),
        )
    }

    @Test
    fun unreadableValuesAreUnknown() {
        val report = HealthEvaluator.evaluate(
            healthy().copy(
                ignoringBatteryOptimizations = null,
                notificationsEnabled = null,
                powerSaveMode = null,
                deviceIdle = null,
                restrictBackgroundStatus = null,
            ),
        )
        listOf(
            HealthCheckId.BATTERY_OPTIMIZATION,
            HealthCheckId.NOTIFICATION_PERMISSION,
            HealthCheckId.POWER_SAVE_MODE,
            HealthCheckId.DEVICE_IDLE,
            HealthCheckId.DATA_SAVER,
        ).forEach { assertEquals(it.name, HealthStatus.UNKNOWN, report.status(it)) }
    }

    @Test
    fun doze() {
        assertEquals("deep", HealthEvaluator.evaluate(healthy().copy(deviceIdle = true)).check(HealthCheckId.DEVICE_IDLE)!!.value)
        assertEquals("light", HealthEvaluator.evaluate(healthy().copy(lightDeviceIdle = true)).check(HealthCheckId.DEVICE_IDLE)!!.value)
    }
}
