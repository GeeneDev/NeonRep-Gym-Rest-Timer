package com.geenedev.gymreps.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.geenedev.gymreps.data.WorkoutStore
import com.geenedev.gymreps.model.HistoryEntry
import com.geenedev.gymreps.model.SavedContext
import com.geenedev.gymreps.model.SessionState
import com.geenedev.gymreps.model.Workout

enum class SaveResult { Saved, NotSavedNoContext, NothingToSave }

/**
 * Holds the screen state. [clock] is monotonic (elapsedRealtime) and drives the timers;
 * [wallClock] stamps history entries.
 */
class WorkoutViewModel(
    private val store: WorkoutStore,
    val clock: () -> Long,
    private val wallClock: () -> Long = System::currentTimeMillis,
    /**
     * True on a fresh launch (not a process-death restore). Then an idle exercise is dropped so
     * the app opens with empty Exercise and Notes fields; a running workout is always restored.
     */
    freshLaunch: Boolean = false,
    groupRunActive: Boolean = false,
) : ViewModel() {
    // A running exercise survives the app being swiped away: restored here, saved on every change.
    private val restored = store.loadInProgress()?.takeIf {
        !freshLaunch || Workout.keepInProgressOnLaunch(it.session, groupRunActive)
    }.also { if (it == null) store.saveInProgress(null) }
    private var sessionState by mutableStateOf(restored?.session ?: SessionState())
    private var contextTextState by mutableStateOf(restored?.contextText ?: "")
    private var iconKeyState by mutableStateOf(
        restored?.iconKey?.takeIf { it.isNotEmpty() } ?: ContextIcons.DEFAULT_KEY,
    )

    var session: SessionState
        get() = sessionState
        private set(value) {
            sessionState = value
            persistInProgress()
        }
    var contextText: String
        get() = contextTextState
        private set(value) {
            contextTextState = value
            persistInProgress()
        }
    var iconKey: String
        get() = iconKeyState
        private set(value) {
            iconKeyState = value
            persistInProgress()
        }
    private var notesTextState by mutableStateOf(restored?.notesText ?: "")
    /** Free-text notes for the exercise (weights, machine height...). */
    var notesText: String
        get() = notesTextState
        private set(value) {
            notesTextState = value
            persistInProgress()
        }

    private fun persistInProgress() = store.saveInProgress(
        WorkoutStore.InProgress(sessionState, contextTextState, iconKeyState, notesTextState),
    )

    var history by mutableStateOf(store.loadHistory())
        private set
    /** Dropdown options: only exercises that are in saved history. */
    val contexts: List<SavedContext> get() = Workout.contextsFromHistory(history)
    var restPresets by mutableStateOf(store.loadRestPresets())
        private set
    var selectedRest by mutableStateOf(store.selectedRestSeconds)
        private set

    fun onBigButton() {
        session = session.tap(clock(), selectedRest * 1000L)
    }

    fun onMinusOne() {
        session = session.minusOne()
    }

    /** Skip button, or the countdown reaching zero at [endedAt]. */
    fun onEndRest(endedAt: Long = clock()) {
        session = session.endRest(endedAt)
    }

    /** Clears everything without saving. */
    fun onReset() {
        session = SessionState()
    }

    /** Saves the exercise when it has sets and a context; only when saved, clears the counter and the exercise input. */
    fun onSave(seriesName: String = "", seriesSize: Int = 0): SaveResult {
        val name = contextText.trim()
        val result = when {
            session.sets == 0 -> SaveResult.NothingToSave
            name.isEmpty() -> SaveResult.NotSavedNoContext
            else -> {
                val finishedAt = wallClock()
                history = Workout.addToHistory(
                    history,
                    HistoryEntry(
                        id = finishedAt,
                        context = name,
                        iconKey = iconKey,
                        sets = session.sets,
                        durationMs = session.totalElapsed(clock()),
                        restSeconds = selectedRest,
                        finishedAt = finishedAt,
                        notes = notesText.trim(),
                        seriesName = seriesName,
                        seriesSize = seriesSize,
                    ),
                )
                store.saveHistory(history)
                session = SessionState()
                // Ready for the next exercise: empty name and notes, default icon.
                contextText = ""
                iconKey = ContextIcons.DEFAULT_KEY
                notesText = ""
                SaveResult.Saved
            }
        }
        return result
    }

    /** Clearing the exercise (X or deleting all text) clears its notes too. */
    fun onContextTextChange(text: String) {
        contextText = text
        if (text.isBlank()) notesText = ""
    }

    fun onNotesChange(text: String) {
        notesText = text
    }

    /** Picking an exercise from the dropdown: its icon and its latest notes (replacing the field). */
    fun onContextPicked(context: SavedContext) {
        onContextTextChange(context.name)
        if (context.iconKey.isNotEmpty()) onIconPicked(context.iconKey)
        notesText = Workout.latestNotesFor(history, context.name)
    }

    /**
     * Repeating a history entry: same exercise, icon, that entry's own notes (empty if it has
     * none) and rest time (added as a preset if missing).
     */
    fun onHistoryEntryPicked(entry: HistoryEntry) {
        onContextTextChange(entry.context)
        if (entry.iconKey.isNotEmpty()) onIconPicked(entry.iconKey)
        notesText = entry.notes
        if (entry.restSeconds > 0) {
            if (entry.restSeconds !in restPresets) onAddRestPreset(entry.restSeconds)
            onSelectRest(entry.restSeconds)
        }
    }

    /**
     * Selects the rest time last saved for [exercise] (added as a preset if missing).
     * Keeps the current selection when that exercise has no rest in history yet.
     */
    fun applyLatestRestFor(exercise: String) {
        val seconds = Workout.latestRestFor(history, exercise) ?: return
        if (seconds !in restPresets) onAddRestPreset(seconds)
        onSelectRest(seconds)
    }

    fun onIconPicked(key: String) {
        iconKey = key
    }

    fun onSelectRest(seconds: Int) {
        selectedRest = seconds
        store.selectedRestSeconds = seconds
    }

    fun onAddRestPreset(seconds: Int) {
        restPresets = Workout.addRestPreset(restPresets, seconds)
        store.saveRestPresets(restPresets)
    }

    fun onRemoveRestPreset(seconds: Int) {
        restPresets = Workout.removeRestPreset(restPresets, seconds)
        store.saveRestPresets(restPresets)
        if (selectedRest !in restPresets) onSelectRest(restPresets.first())
    }

    fun onDeleteEntry(id: Long) {
        history = history.filterNot { it.id == id }
        store.saveHistory(history)
    }

    /** Replaces the history entry with the same id (from the long-press Edit card). */
    fun onUpdateEntry(entry: HistoryEntry) {
        history = history.map { if (it.id == entry.id) entry else it }
        store.saveHistory(history)
    }

    fun onClearHistory() {
        history = emptyList()
        store.saveHistory(history)
    }
}
