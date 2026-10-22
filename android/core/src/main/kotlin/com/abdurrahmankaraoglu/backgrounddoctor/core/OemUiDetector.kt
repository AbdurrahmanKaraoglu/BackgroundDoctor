package com.abdurrahmankaraoglu.backgrounddoctor.core

/**
 * Best-effort vendor skin detection from system properties. [prop] returns the value of a
 * property or `null`/empty when it isn't set.
 */
object OemUiDetector {
    fun detect(manufacturer: String, brand: String, prop: (String) -> String?): Pair<OemUi, String?> {
        fun p(name: String): String? = prop(name)?.trim()?.takeIf { it.isNotEmpty() }
        val maker = manufacturer.lowercase()
        val brandLower = brand.lowercase()

        p("ro.mi.os.version.name")?.let { return OemUi.HYPER_OS to it }
        p("ro.miui.ui.version.name")?.let { return OemUi.MIUI to it }
        p("ro.build.version.magic")?.let { return OemUi.MAGIC_OS to it }
        p("hw_sc.build.platform.version")?.let { if (maker == "huawei") return OemUi.HARMONY_OS to it }
        p("ro.build.version.emui")?.let { return OemUi.EMUI to it }
        p("ro.build.version.realmeui")?.let { return OemUi.REALME_UI to it }
        p("ro.oxygen.version")?.let { return OemUi.OXYGEN_OS to it }
        (p("ro.build.version.oplusrom") ?: p("ro.build.version.opporom"))?.let {
            return when {
                maker == "oneplus" -> OemUi.OXYGEN_OS to it
                maker == "realme" || brandLower == "realme" -> OemUi.REALME_UI to it
                else -> OemUi.COLOR_OS to it
            }
        }
        p("ro.vivo.os.version")?.let { version ->
            val name = p("ro.vivo.os.name").orEmpty()
            return (if (name.contains("origin", ignoreCase = true)) OemUi.ORIGIN_OS else OemUi.FUNTOUCH_OS) to version
        }
        p("ro.build.version.oneui")?.let { return OemUi.ONE_UI to oneUiVersion(it) }
        p("ro.build.display.id")?.let { if (it.contains("flyme", ignoreCase = true)) return OemUi.FLYME to it }

        return when (maker) {
            "google" -> OemUi.STOCK to null
            "samsung" -> OemUi.ONE_UI to null
            "xiaomi" -> OemUi.MIUI to null
            "huawei" -> OemUi.EMUI to null
            else -> OemUi.UNKNOWN to null
        }
    }

    /** `ro.build.version.oneui` holds e.g. `60100` for One UI 6.1. */
    internal fun oneUiVersion(raw: String): String {
        val n = raw.toIntOrNull() ?: return raw
        return "${n / 10000}.${(n / 100) % 100}"
    }
}
