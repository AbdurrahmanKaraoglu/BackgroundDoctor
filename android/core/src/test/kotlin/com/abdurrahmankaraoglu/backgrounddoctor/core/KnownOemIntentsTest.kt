package com.abdurrahmankaraoglu.backgrounddoctor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnownOemIntentsTest {
    @Test
    fun idsAreUnique() {
        val ids = KnownOemIntents.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun everyEntryIsWellFormed() {
        val date = Regex("""\d{4}-\d{2}-\d{2}""")
        KnownOemIntents.all.forEach { entry ->
            assertTrue(entry.id, (entry.className != null && entry.packageName != null) || entry.action != null)
            assertTrue(entry.id, entry.source.startsWith("https://"))
            assertTrue(entry.id, date.matches(entry.lastChecked))
            assertTrue(entry.id, entry.brands.isNotEmpty() && entry.brands.all { it == it.lowercase() })
        }
    }

    @Test
    fun brandLookupIsCaseInsensitiveAndUsesBrandToo() {
        assertTrue(KnownOemIntents.forBrand("Xiaomi", "Redmi").any { it.id == "xiaomi_autostart" })
        assertTrue(KnownOemIntents.forBrand("  XIAOMI ", "").isNotEmpty())
        assertTrue(KnownOemIntents.forBrand("HONOR", "HONOR").any { it.packageName == "com.huawei.systemmanager" })
        assertTrue(KnownOemIntents.forBrand("Google", "google").isEmpty())
        assertEquals(
            listOf("samsung_battery_legacy", "samsung_battery_app_list", "samsung_battery"),
            KnownOemIntents.forBrand("samsung", "samsung").map { it.id },
        )
    }

    @Test
    fun nothingIsClaimedAsVerified() {
        // Update this test when an entry is actually checked on a device.
        assertTrue(KnownOemIntents.all.all { it.verification == Verification.COMMUNITY_REPORTED })
    }

    @Test
    fun manifestQueriesCoverEveryPackageAndAction() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        KnownOemIntents.packageNames.forEach { pkg ->
            assertTrue("missing <queries> package $pkg", manifest.contains("<package android:name=\"$pkg\" />"))
        }
        KnownOemIntents.all.mapNotNull { it.action }.forEach { action ->
            assertTrue("missing <queries> action $action", manifest.contains("android:name=\"$action\""))
        }
    }
}
