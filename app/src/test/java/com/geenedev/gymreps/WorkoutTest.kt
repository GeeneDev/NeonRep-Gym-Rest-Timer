package com.geenedev.gymreps

import com.geenedev.gymreps.model.HistoryEntry
import com.geenedev.gymreps.model.SavedContext
import com.geenedev.gymreps.model.SessionState
import com.geenedev.gymreps.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTest {
    @Test
    fun firstTapStartsWithZeroSets() {
        val s = SessionState().tap(1_000, restMs = 60_000)
        assertTrue(s.isRunning)
        assertFalse(s.isResting)
        assertEquals(0, s.sets)
        assertEquals(500, s.workElapsed(1_500))
    }

    @Test
    fun secondTapCountsASetAndStartsRest() {
        val s = SessionState().tap(0, 60_000).tap(30_000, 60_000)
        assertEquals(1, s.sets)
        assertTrue(s.isResting)
        assertEquals(50_000, s.restRemaining(40_000))
        assertEquals(0, s.restRemaining(200_000))
    }

    @Test
    fun restEndingReturnsToWorkTimedFromEnd() {
        val s = SessionState().tap(0, 60_000).tap(30_000, 60_000).endRest(90_000)
        assertFalse(s.isResting)
        assertEquals(1, s.sets)
        assertEquals(5_000, s.workElapsed(95_000))
        assertEquals(95_000, s.totalElapsed(95_000))
    }

    @Test
    fun tapWhileRestingSkipsRestWithoutCounting() {
        val s = SessionState().tap(0, 60_000).tap(30_000, 60_000).tap(40_000, 60_000)
        assertFalse(s.isResting)
        assertEquals(1, s.sets)
    }

    @Test
    fun minusOneNeverGoesBelowZero() {
        assertEquals(0, SessionState().tap(0, 1).minusOne().sets)
        assertEquals(0, SessionState().tap(0, 1).tap(1, 1).minusOne().sets)
    }

    @Test
    fun historyIsNewestFirst() {
        val old = HistoryEntry(1, "Squat", "", 5, 0, restSeconds = 60, finishedAt = 100)
        val new = HistoryEntry(2, "Curl", "", 8, 0, finishedAt = 200)
        assertEquals(listOf(new, old), Workout.addToHistory(listOf(old), new))
    }

    @Test
    fun notesDefaultEmptyAndLatestNotesPrefill() {
        assertEquals("", HistoryEntry(1, "Squat", "", 5, 0, finishedAt = 100).notes)
        val history = listOf(
            HistoryEntry(4, "Curl", "", 3, 0, finishedAt = 400, notes = "12 kg"),
            HistoryEntry(3, "bench", "", 3, 0, finishedAt = 300), // newest bench, no notes
            HistoryEntry(2, "Bench", "", 3, 0, finishedAt = 200, notes = "60 kg, seat 4"),
            HistoryEntry(1, "Bench", "", 3, 0, finishedAt = 100, notes = "55 kg"),
        )
        assertEquals("60 kg, seat 4", Workout.latestNotesFor(history, " BENCH "))
        assertEquals("12 kg", Workout.latestNotesFor(history, "curl"))
        assertEquals("", Workout.latestNotesFor(history, "Squat"))
    }

    @Test
    fun freshLaunchKeepsOnlyARunningWorkout() {
        assertFalse(Workout.keepInProgressOnLaunch(SessionState(), groupRunActive = false))
        assertTrue(Workout.keepInProgressOnLaunch(SessionState().tap(1_000, 60_000), groupRunActive = false))
        assertTrue(Workout.keepInProgressOnLaunch(SessionState(sets = 2), groupRunActive = false))
        assertTrue(Workout.keepInProgressOnLaunch(SessionState(), groupRunActive = true))
    }

    @Test
    fun dropdownOptionsComeOnlyFromHistoryDeduplicated() {
        val history = listOf(
            HistoryEntry(3, "bench", "c", 3, 0, finishedAt = 300),
            HistoryEntry(2, "Squat", "a", 4, 0, finishedAt = 200),
            HistoryEntry(1, "Bench", "b", 5, 0, finishedAt = 100),
        )
        assertEquals(
            listOf(SavedContext("bench", "c"), SavedContext("Squat", "a")),
            Workout.contextsFromHistory(history),
        )
        assertEquals(emptyList<SavedContext>(), Workout.contextsFromHistory(emptyList()))
    }

    @Test
    fun restPresetsStaySortedUniqueAndNonEmpty() {
        assertEquals(listOf(30, 45, 60), Workout.addRestPreset(listOf(60, 30), 45))
        assertEquals(listOf(30, 60), Workout.addRestPreset(listOf(30, 60), 60))
        assertEquals(listOf(30), Workout.removeRestPreset(listOf(30), 30))
    }

    @Test
    fun photoKeyIsFileSafeAndIgnoresCase() {
        val key = Workout.photoKey("  Leg Press #2 ")
        assertEquals(key, Workout.photoKey("leg press #2"))
        assertTrue(key.matches(Regex("[a-z0-9_]+")))
        assertTrue(key.startsWith("leg_press_2_"))
        assertTrue(Workout.photoKey("Squat") != Workout.photoKey("Squats"))
    }

    @Test
    fun formatting() {
        assertEquals("1:05", Workout.formatDuration(65_400))
        assertEquals("0:03.2", Workout.formatDuration(3_250, tenths = true))
        assertEquals("1m 30s", Workout.formatRest(90))
        assertEquals("45s", Workout.formatRest(45))
        assertEquals("2m", Workout.formatRest(120))
    }

    @Test
    fun daySpan_runsFromFirstStartToLastFinish() {
        val entries = listOf(
            HistoryEntry(2, "Curl", "", 3, durationMs = 60_000, finishedAt = 5_000_000),
            HistoryEntry(1, "Squat", "", 3, durationMs = 300_000, finishedAt = 1_000_000),
        )
        assertEquals(700_000L to 5_000_000L, Workout.daySpan(entries))
        assertEquals(null, Workout.daySpan(emptyList()))
    }

    @Test
    fun daySeriesNames_distinctInOrderSkippingBlank() {
        fun e(at: Long, series: String) = HistoryEntry(
            id = at, context = "x", iconKey = "", sets = 1, durationMs = 0, finishedAt = at, seriesName = series,
        )
        val entries = listOf(e(3, "Pull"), e(1, "Push"), e(2, ""), e(4, "Push"))
        assertEquals(listOf("Push", "Pull"), Workout.daySeriesNames(entries))
        assertEquals(emptyList<String>(), Workout.daySeriesNames(listOf(e(1, " "))))
    }

    @Test
    fun dayProgress_countsSeriesSizePlusLooseExercises() {
        fun e(at: Long, series: String, size: Int) = HistoryEntry(
            id = at, context = "x", iconKey = "", sets = 1, durationMs = 0, finishedAt = at,
            seriesName = series, seriesSize = size,
        )
        assertEquals(null, Workout.dayProgress(listOf(e(1, "", 0))))
        assertEquals(7 to 8, Workout.dayProgress(List(7) { e(it.toLong(), "Push", 8) }))
        // One loose exercise on top of a full series of 2.
        assertEquals(3 to 3, Workout.dayProgress(listOf(e(1, "Pull", 2), e(2, "Pull", 2), e(3, "", 0))))
        // Done more than planned (repeated exercise): planned grows to match.
        assertEquals(3 to 3, Workout.dayProgress(List(3) { e(it.toLong(), "Legs", 2) }))
    }

    @Test
    fun formatSpan_minutesAndHours() {
        assertEquals("<1m", Workout.formatSpan(30_000))
        assertEquals("45m", Workout.formatSpan(45 * 60_000L))
        assertEquals("1h 05m", Workout.formatSpan(65 * 60_000L))
    }

    @Test
    fun latestRestForPicksNewestEntryWithARest() {
        val history = listOf(
            HistoryEntry(3, "bench", "", 3, 0, finishedAt = 300), // newest bench, no rest tracked
            HistoryEntry(2, "Bench", "", 3, 0, restSeconds = 90, finishedAt = 200),
            HistoryEntry(1, "Bench", "", 3, 0, restSeconds = 60, finishedAt = 100),
        )
        assertEquals(90, Workout.latestRestFor(history, " BENCH "))
        assertEquals(null, Workout.latestRestFor(history, "Squat"))
    }
}
