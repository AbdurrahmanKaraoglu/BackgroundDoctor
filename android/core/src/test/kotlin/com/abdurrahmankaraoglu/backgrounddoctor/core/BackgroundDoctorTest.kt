package com.abdurrahmankaraoglu.backgrounddoctor.core

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivityManager.ApplicationExitInfoBuilder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackgroundDoctorTest {
    private val app: Application = ApplicationProvider.getApplicationContext()
    private val am get() = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private fun addExit(reason: Int, timestamp: Long) {
        val info = ApplicationExitInfoBuilder.newBuilder()
            .setProcessName(app.packageName)
            .setReason(reason)
            .setTimestamp(timestamp)
            .setDescription("test $reason")
            .setPss(1000)
            .setRss(2000)
            .build()
        shadowOf(am).addApplicationExitInfo(info)
    }

    @Test
    fun reportHasEveryCheck() {
        val report = BackgroundDoctor(app).healthReport()
        assertEquals(HealthCheckId.entries.size, report.checks.size)
        assertEquals(34, report.device.sdkInt)
        assertEquals(HealthStatus.NOT_APPLICABLE, report.check(HealthCheckId.EXACT_ALARM)!!.status)
    }

    @Test
    fun exitReasonsAndUnseenFilter() {
        addExit(ApplicationExitInfo.REASON_CRASH, 1_000)
        addExit(ApplicationExitInfo.REASON_ANR, 2_000)
        val doctor = BackgroundDoctor(app)

        val first = doctor.exitReasons(onlyUnseen = true, markSeen = true)
        assertTrue(first.supported)
        assertNull(first.lastSeenMillis)
        assertEquals(setOf(ExitReason.CRASH, ExitReason.ANR), first.exits.map { it.reason }.toSet())
        assertEquals(1000L, first.exits.first { it.reason == ExitReason.CRASH }.pssKb)

        addExit(ApplicationExitInfo.REASON_LOW_MEMORY, 3_000)
        val second = doctor.exitReasons(onlyUnseen = true)
        assertEquals(2_000L, second.lastSeenMillis)
        assertEquals(listOf(ExitReason.LOW_MEMORY), second.exits.map { it.reason })
    }

    @Test
    @Config(sdk = [29])
    fun exitReasonsUnsupportedBelowApi30() {
        val result = BackgroundDoctor(app).exitReasons()
        assertFalse(result.supported)
        assertTrue(result.exits.isEmpty())
        assertFalse(BackgroundDoctor(app).setProcessStateSummary("label"))
    }

    @Test
    fun stateSummaryLimit() {
        assertThrows(IllegalArgumentException::class.java) { BackgroundDoctor(app).setProcessStateSummary("x".repeat(129)) }
        assertTrue(BackgroundDoctor(app).setProcessStateSummary("screen=map"))
    }
}
