package com.abdurrahmankaraoglu.backgrounddoctor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OemUiDetectorTest {
    private fun detect(maker: String, vararg props: Pair<String, String>) =
        OemUiDetector.detect(maker, maker) { props.toMap()[it] }

    @Test
    fun xiaomi() {
        assertEquals(OemUi.MIUI to "V140", detect("Xiaomi", "ro.miui.ui.version.name" to "V140"))
        assertEquals(OemUi.HYPER_OS to "OS1.0", detect("Xiaomi", "ro.miui.ui.version.name" to "V816", "ro.mi.os.version.name" to "OS1.0"))
        assertEquals(OemUi.MIUI to null, detect("Xiaomi"))
    }

    @Test
    fun huaweiAndHonor() {
        assertEquals(OemUi.EMUI to "EmotionUI_12.0.0", detect("HUAWEI", "ro.build.version.emui" to "EmotionUI_12.0.0"))
        assertEquals(OemUi.HARMONY_OS to "2.0.0", detect("HUAWEI", "hw_sc.build.platform.version" to "2.0.0", "ro.build.version.emui" to "EmotionUI_11"))
        assertEquals(OemUi.MAGIC_OS to "MagicOS_7.0", detect("HONOR", "ro.build.version.magic" to "MagicOS_7.0"))
    }

    @Test
    fun bbkFamily() {
        assertEquals(OemUi.COLOR_OS to "V13.1", detect("OPPO", "ro.build.version.opporom" to "V13.1"))
        assertEquals(OemUi.OXYGEN_OS to "V14.0", detect("OnePlus", "ro.build.version.oplusrom" to "V14.0"))
        assertEquals(OemUi.REALME_UI to "V5.0", detect("realme", "ro.build.version.realmeui" to "V5.0"))
        assertEquals(OemUi.REALME_UI to "V13", detect("realme", "ro.build.version.oplusrom" to "V13"))
        assertEquals(OemUi.FUNTOUCH_OS to "13.0", detect("vivo", "ro.vivo.os.version" to "13.0", "ro.vivo.os.name" to "Funtouch"))
        assertEquals(OemUi.ORIGIN_OS to "4.0", detect("vivo", "ro.vivo.os.version" to "4.0", "ro.vivo.os.name" to "OriginOS"))
    }

    @Test
    fun samsungAndOthers() {
        assertEquals(OemUi.ONE_UI to "6.1", detect("samsung", "ro.build.version.oneui" to "60100"))
        assertEquals(OemUi.ONE_UI to null, detect("samsung"))
        assertEquals(OemUi.FLYME to "Flyme 10.0", detect("Meizu", "ro.build.display.id" to "Flyme 10.0"))
        assertEquals(OemUi.STOCK to null, detect("Google", "ro.build.display.id" to "AP2A"))
        assertEquals(OemUi.UNKNOWN to null, detect("Fairphone", "ro.miui.ui.version.name" to "  "))
    }
}
