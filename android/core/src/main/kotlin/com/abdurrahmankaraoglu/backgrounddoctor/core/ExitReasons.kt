package com.abdurrahmankaraoglu.backgrounddoctor.core

import java.io.InputStream

/** `ApplicationExitInfo.REASON_*`, with codes kept so newer values still round-trip. */
enum class ExitReason(val code: Int, val wireName: String, val category: ExitCategory) {
    UNKNOWN(0, "unknown", ExitCategory.UNKNOWN),
    EXIT_SELF(1, "exitSelf", ExitCategory.SELF),
    SIGNALED(2, "signaled", ExitCategory.SYSTEM),
    LOW_MEMORY(3, "lowMemory", ExitCategory.SYSTEM),
    CRASH(4, "crash", ExitCategory.CRASH),
    CRASH_NATIVE(5, "crashNative", ExitCategory.CRASH),
    ANR(6, "anr", ExitCategory.ANR),
    INITIALIZATION_FAILURE(7, "initializationFailure", ExitCategory.CRASH),
    PERMISSION_CHANGE(8, "permissionChange", ExitCategory.USER),
    EXCESSIVE_RESOURCE_USAGE(9, "excessiveResourceUsage", ExitCategory.SYSTEM),
    USER_REQUESTED(10, "userRequested", ExitCategory.USER),
    USER_STOPPED(11, "userStopped", ExitCategory.USER),
    DEPENDENCY_DIED(12, "dependencyDied", ExitCategory.SYSTEM),
    OTHER(13, "other", ExitCategory.SYSTEM),
    FREEZER(14, "freezer", ExitCategory.SYSTEM),
    PACKAGE_STATE_CHANGE(15, "packageStateChange", ExitCategory.UPDATE),
    PACKAGE_UPDATED(16, "packageUpdated", ExitCategory.UPDATE),
    ;

    companion object {
        fun fromCode(code: Int): ExitReason = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** Coarse grouping of [ExitReason] answering "who ended the process". */
enum class ExitCategory(val wireName: String) {
    CRASH("crash"),
    ANR("anr"),
    USER("user"),
    SYSTEM("system"),
    SELF("self"),
    UPDATE("update"),
    UNKNOWN("unknown"),
}

data class AppExitInfo(
    val reasonCode: Int,
    val reason: ExitReason,
    val status: Int,
    val importance: Int,
    val timestampMillis: Long,
    val pid: Int,
    val processName: String,
    val description: String?,
    val pssKb: Long,
    val rssKb: Long,
    /** Text set earlier with `setProcessStateSummary`, decoded as UTF-8. */
    val stateSummary: String?,
    /** Readable part of the ANR trace or native tombstone, at most the requested size. */
    val traceExcerpt: String?,
    val traceTruncated: Boolean,
)

data class ExitReasonsResult(
    val supported: Boolean,
    val exits: List<AppExitInfo>,
    /** Newest exit timestamp marked as seen before this call, or `null` if never marked. */
    val lastSeenMillis: Long?,
)

object ExitTraces {
    /** Maximum bytes `setProcessStateSummary` accepts. */
    const val MAX_STATE_SUMMARY_BYTES = 128

    /**
     * Reads at most [maxBytes] from [input] and returns the readable text. ANR traces are text;
     * native tombstones are protobuf, so only printable runs of 4+ characters are kept.
     */
    fun excerpt(input: InputStream, maxBytes: Int, binary: Boolean): Pair<String, Boolean> {
        val limit = maxBytes.coerceAtLeast(0)
        val buffer = ByteArray(limit + 1)
        var read = 0
        input.use { stream ->
            while (read < buffer.size) {
                val n = stream.read(buffer, read, buffer.size - read)
                if (n < 0) break
                read += n
            }
        }
        val truncated = read > limit
        val bytes = buffer.copyOf(minOf(read, limit))
        val text = if (binary) printableRuns(bytes) else String(bytes, Charsets.UTF_8)
        return text to truncated
    }

    /** Like the Unix `strings` tool: ASCII runs of at least [minRun] printable characters. */
    fun printableRuns(bytes: ByteArray, minRun: Int = 4): String {
        val out = StringBuilder()
        val run = StringBuilder()
        fun flush() {
            if (run.length >= minRun) {
                if (out.isNotEmpty()) out.append('\n')
                out.append(run)
            }
            run.setLength(0)
        }
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E || c == 0x09) run.append(c.toChar()) else flush()
        }
        flush()
        return out.toString()
    }

    /** UTF-8 bytes of [summary], or `null` if it exceeds [MAX_STATE_SUMMARY_BYTES]. */
    fun encodeStateSummary(summary: String): ByteArray? =
        summary.toByteArray(Charsets.UTF_8).takeIf { it.size <= MAX_STATE_SUMMARY_BYTES }

    /** Keeps exits newer than [lastSeenMillis] when [onlyUnseen] is set. */
    fun filterUnseen(exits: List<AppExitInfo>, lastSeenMillis: Long?, onlyUnseen: Boolean): List<AppExitInfo> =
        if (!onlyUnseen || lastSeenMillis == null) exits else exits.filter { it.timestampMillis > lastSeenMillis }
}
