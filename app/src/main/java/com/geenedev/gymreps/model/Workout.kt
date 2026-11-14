package com.geenedev.gymreps.model

/** One finished exercise, as shown in the history list. */
data class HistoryEntry(
    val id: Long,
    val context: String,
    val iconKey: String,
    val sets: Int,
    val durationMs: Long,
    /** Rest time that was selected between sets; 0 for entries saved before this was tracked. */
    val restSeconds: Int = 0,
    /** Wall-clock time (epoch millis) when the set was saved. */
    val finishedAt: Long,
    /** Free text: weights, machine height, anything. Empty for older entries. */
    val notes: String = "",
    /** Name of the series (group) this was saved in; empty when saved outside a series. */
    val seriesName: String = "",
    /** Number of exercises in that series when this was saved; 0 outside a series. */
    val seriesSize: Int = 0,
)

/** An exercise name from saved history, offered again in the dropdown. */
data class SavedContext(val name: String, val iconKey: String)

/**
 * The exercise in progress, counted in sets. Times come from a monotonic clock
 * (SystemClock.elapsedRealtime on Android), so they are not persisted across process death.
 *
 * Flow: idle -> START -> working -> tap (+1 set) -> resting -> rest ends or Skip -> working ...
 */
data class SessionState(
    val sets: Int = 0,
    val startedAt: Long? = null,
    /** Start of the current working period; null while resting. */
    val workStartedAt: Long? = null,
    val restEndsAt: Long? = null,
) {
    val isRunning: Boolean get() = startedAt != null
    val isResting: Boolean get() = restEndsAt != null

    /** The big button: starts, counts a set and starts the rest, or (while resting) ends the rest. */
    fun tap(now: Long, restMs: Long): SessionState = when {
        !isRunning -> SessionState(startedAt = now, workStartedAt = now)
        isResting -> endRest(now)
        else -> copy(sets = sets + 1, workStartedAt = null, restEndsAt = now + restMs)
    }

    /** Skip, or the countdown reaching zero: back to working, timed from [now]. */
    fun endRest(now: Long): SessionState =
        if (!isResting) this else copy(restEndsAt = null, workStartedAt = now)

    fun minusOne(): SessionState = if (sets == 0) this else copy(sets = sets - 1)

    fun totalElapsed(now: Long): Long = startedAt?.let { now - it } ?: 0L
    fun workElapsed(now: Long): Long = workStartedAt?.let { now - it } ?: 0L
    fun restRemaining(now: Long): Long = restEndsAt?.let { (it - now).coerceAtLeast(0) } ?: 0L
}

object Workout {
    val DEFAULT_REST_PRESETS = listOf(30, 60, 90, 120)

    /** Newest first. */
    fun addToHistory(history: List<HistoryEntry>, entry: HistoryEntry): List<HistoryEntry> =
        (listOf(entry) + history).sortedByDescending { it.finishedAt }

    /** One entry per exercise name in [history] (newest first), using its latest icon. */
    fun contextsFromHistory(history: List<HistoryEntry>): List<SavedContext> =
        history.distinctBy { it.context.lowercase() }.map { SavedContext(it.context, it.iconKey) }

    /** Notes from the newest history entry of [exercise] (case-insensitive) that has any; "" if none. */
    /** Rest time last used for [exercise] (case-insensitive); null if it never had one saved. */
    fun latestRestFor(history: List<HistoryEntry>, exercise: String): Int? {
        val name = exercise.trim()
        return history.sortedByDescending { it.finishedAt }
            .firstOrNull { it.context.equals(name, ignoreCase = true) && it.restSeconds > 0 }
            ?.restSeconds
    }

    fun latestNotesFor(history: List<HistoryEntry>, exercise: String): String {
        val name = exercise.trim()
        return history.sortedByDescending { it.finishedAt }
            .firstOrNull { it.context.equals(name, ignoreCase = true) && it.notes.isNotBlank() }
            ?.notes ?: ""
    }

    /**
     * On a fresh app launch the Exercise and Notes fields start empty, unless a workout is
     * underway: a timer running / sets counted, or a group run in progress (whose current
     * exercise sits in the field).
     */
    fun keepInProgressOnLaunch(session: SessionState, groupRunActive: Boolean): Boolean =
        session.isRunning || session.sets > 0 || groupRunActive

    /** File-safe, case-insensitive key for an exercise's machine photo. */
    fun photoKey(exercise: String): String {
        val name = exercise.trim().lowercase()
        val slug = name.replace(Regex("[^a-z0-9]+"), "_").trim('_').take(40)
        return "${slug.ifEmpty { "exercise" }}_${name.hashCode().toUInt().toString(16)}"
    }

    fun filterContexts(contexts: List<SavedContext>, query: String): List<SavedContext> {
        val q = query.trim()
        if (q.isEmpty()) return contexts
        return contexts.filter { it.name.contains(q, ignoreCase = true) }
    }

    fun addRestPreset(presets: List<Int>, seconds: Int): List<Int> =
        if (seconds <= 0) presets else (presets + seconds).distinct().sorted()

    /** Never removes the last preset, so there is always something to pick. */
    fun removeRestPreset(presets: List<Int>, seconds: Int): List<Int> =
        if (presets.size <= 1) presets else presets - seconds

    /** "m:ss", or "m:ss.d" with [tenths]. Negative values count as zero. */
    fun formatDuration(ms: Long, tenths: Boolean = false): String {
        val clamped = ms.coerceAtLeast(0)
        val totalSeconds = clamped / 1000
        val base = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
        return if (tenths) "$base.${(clamped % 1000) / 100}" else base
    }

    /**
     * Time span of a day's [entries]: from the start of the earliest exercise
     * (finishedAt - durationMs) to the latest finishedAt. Null for an empty list.
     */
    fun daySeriesNames(entries: List<HistoryEntry>): List<String> =
        entries.sortedBy { it.finishedAt }.map { it.seriesName.trim() }.filter { it.isNotEmpty() }.distinct()

    /**
     * "done/planned" for a day's header: every saved exercise counts as done; planned is the
     * size of each series trained that day plus the exercises saved outside a series.
     * Null when nothing that day was saved inside a series.
     */
    fun dayProgress(entries: List<HistoryEntry>): Pair<Int, Int>? {
        val inSeries = entries.filter { it.seriesName.isNotBlank() }
        if (inSeries.isEmpty()) return null
        val planned = inSeries.groupBy { it.seriesName.trim() }
            .values.sumOf { list -> maxOf(list.maxOf { it.seriesSize }, list.size) }
        return entries.size to planned + (entries.size - inSeries.size)
    }

    fun daySpan(entries: List<HistoryEntry>): Pair<Long, Long>? {
        if (entries.isEmpty()) return null
        val start = entries.minOf { it.finishedAt - it.durationMs.coerceAtLeast(0) }
        val end = entries.maxOf { it.finishedAt }
        return start to end
    }

    /** Span length label: "45m", "1h 05m"; under a minute is "<1m". */
    fun formatSpan(ms: Long): String {
        val minutes = ms.coerceAtLeast(0) / 60_000
        return when {
            minutes == 0L -> "<1m"
            minutes < 60 -> "${minutes}m"
            else -> "%dh %02dm".format(minutes / 60, minutes % 60)
        }
    }

    /** Rest preset label: "45s", "1m", "1m 30s". */
    fun formatRest(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return when {
            m == 0 -> "${s}s"
            s == 0 -> "${m}m"
            else -> "${m}m ${s}s"
        }
    }
}
