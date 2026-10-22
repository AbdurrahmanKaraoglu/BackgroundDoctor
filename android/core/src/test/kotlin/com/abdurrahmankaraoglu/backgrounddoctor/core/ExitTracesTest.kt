package com.abdurrahmankaraoglu.backgrounddoctor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ExitTracesTest {
    @Test
    fun reasonCodesMatchApplicationExitInfo() {
        assertEquals(ExitReason.CRASH, ExitReason.fromCode(4))
        assertEquals(ExitReason.CRASH_NATIVE, ExitReason.fromCode(5))
        assertEquals(ExitReason.ANR, ExitReason.fromCode(6))
        assertEquals(ExitReason.USER_REQUESTED, ExitReason.fromCode(10))
        assertEquals(ExitReason.PACKAGE_UPDATED, ExitReason.fromCode(16))
        assertEquals(ExitReason.UNKNOWN, ExitReason.fromCode(999))
        assertEquals(ExitReason.entries.size, ExitReason.entries.map { it.code }.toSet().size)
        assertEquals(ExitCategory.SYSTEM, ExitReason.LOW_MEMORY.category)
    }

    @Test
    fun textTraceIsCutAtLimit() {
        val (text, truncated) = ExitTraces.excerpt(ByteArrayInputStream("abcdefghij".toByteArray()), 4, binary = false)
        assertEquals("abcd", text)
        assertTrue(truncated)
        val (whole, notTruncated) = ExitTraces.excerpt(ByteArrayInputStream("abc".toByteArray()), 4, binary = false)
        assertEquals("abc", whole)
        assertFalse(notTruncated)
    }

    @Test
    fun binaryTraceKeepsPrintableRuns() {
        val bytes = byteArrayOf(0, 1) + "abort message".toByteArray() + byteArrayOf(0x12, 0) +
            "xy".toByteArray() + byteArrayOf(-1) + "libc.so".toByteArray()
        val (text, _) = ExitTraces.excerpt(ByteArrayInputStream(bytes), 1024, binary = true)
        assertEquals("abort message\nlibc.so", text)
    }

    @Test
    fun stateSummaryLimitIs128Utf8Bytes() {
        assertEquals(128, ExitTraces.encodeStateSummary("a".repeat(128))!!.size)
        assertNull(ExitTraces.encodeStateSummary("a".repeat(129)))
        assertNull(ExitTraces.encodeStateSummary("ş".repeat(65))) // 130 bytes
    }

    @Test
    fun unseenFilter() {
        fun exit(t: Long) = AppExitInfo(4, ExitReason.CRASH, 0, 100, t, 1, "p", null, 0, 0, null, null, false)
        val exits = listOf(exit(30), exit(20), exit(10))
        assertEquals(listOf(30L), ExitTraces.filterUnseen(exits, 20, onlyUnseen = true).map { it.timestampMillis })
        assertEquals(3, ExitTraces.filterUnseen(exits, 20, onlyUnseen = false).size)
        assertEquals(3, ExitTraces.filterUnseen(exits, null, onlyUnseen = true).size)
    }
}
