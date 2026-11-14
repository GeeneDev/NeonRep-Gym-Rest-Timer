package com.geenedev.gymreps.data

import android.content.Context
import android.provider.Settings
import com.geenedev.gymreps.model.HistoryEntry
import com.geenedev.gymreps.model.SessionState
import com.geenedev.gymreps.model.Workout
import org.json.JSONArray
import org.json.JSONObject

/** Everything lives on the device in one SharedPreferences file, stored as JSON. */
class WorkoutStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("workout", Context.MODE_PRIVATE)

    /** Unsaved exercise: the running session plus the typed name and chosen icon. */
    data class InProgress(
        val session: SessionState,
        val contextText: String,
        val iconKey: String,
        val notesText: String = "",
    )

    fun loadHistory(): List<HistoryEntry> = readArray(KEY_HISTORY).map {
        HistoryEntry(
            id = it.getLong("id"),
            context = it.getString("context"),
            iconKey = it.optString("icon"),
            sets = it.optInt("sets", it.optInt("reps")),
            durationMs = it.optLong("durationMs"),
            restSeconds = it.optInt("restSeconds"),
            finishedAt = it.getLong("finishedAt"),
            notes = it.optString("notes", ""),
            seriesName = it.optString("series", ""),
            seriesSize = it.optInt("seriesSize", 0),
        )
    }.sortedByDescending { it.finishedAt }

    fun saveHistory(history: List<HistoryEntry>) = writeArray(KEY_HISTORY, history.map {
        JSONObject()
            .put("id", it.id)
            .put("context", it.context)
            .put("icon", it.iconKey)
            .put("sets", it.sets)
            .put("durationMs", it.durationMs)
            .put("restSeconds", it.restSeconds)
            .put("finishedAt", it.finishedAt)
            .put("notes", it.notes)
            .put("series", it.seriesName)
            .put("seriesSize", it.seriesSize)
    })

    fun loadRestPresets(): List<Int> {
        val raw = prefs.getString(KEY_REST_PRESETS, null) ?: return Workout.DEFAULT_REST_PRESETS
        val array = JSONArray(raw)
        return List(array.length()) { array.getInt(it) }.ifEmpty { Workout.DEFAULT_REST_PRESETS }
    }

    fun saveRestPresets(presets: List<Int>) =
        prefs.edit().putString(KEY_REST_PRESETS, JSONArray(presets).toString()).apply()

    var selectedRestSeconds: Int
        get() = prefs.getInt(KEY_SELECTED_REST, 90)
        set(value) = prefs.edit().putInt(KEY_SELECTED_REST, value).apply()

    /**
     * The exercise in progress, so swiping the app away (process death) doesn't lose it.
     * Session times are elapsedRealtime values, which restart at boot, so the saved copy is
     * tagged with the boot count and dropped after a reboot.
     */
    fun loadInProgress(): InProgress? {
        val raw = prefs.getString(KEY_IN_PROGRESS, null) ?: return null
        return try {
            val o = JSONObject(raw)
            if (o.optInt("boot", -2) != bootCount()) return null
            fun optTime(key: String): Long? = if (o.isNull(key) || !o.has(key)) null else o.getLong(key)
            InProgress(
                session = SessionState(
                    sets = o.optInt("sets"),
                    startedAt = optTime("startedAt"),
                    workStartedAt = optTime("workStartedAt"),
                    restEndsAt = optTime("restEndsAt"),
                ),
                contextText = o.optString("context"),
                iconKey = o.optString("icon"),
                notesText = o.optString("notes", ""),
            )
        } catch (e: org.json.JSONException) {
            null
        }
    }

    /** Pass null (or an empty, idle state) to forget the saved exercise. */
    fun saveInProgress(state: InProgress?) {
        if (state == null ||
            (!state.session.isRunning && state.contextText.isEmpty() && state.notesText.isEmpty())
        ) {
            prefs.edit().remove(KEY_IN_PROGRESS).apply()
            return
        }
        val s = state.session
        val o = JSONObject()
            .put("boot", bootCount())
            .put("sets", s.sets)
            .put("startedAt", s.startedAt ?: JSONObject.NULL)
            .put("workStartedAt", s.workStartedAt ?: JSONObject.NULL)
            .put("restEndsAt", s.restEndsAt ?: JSONObject.NULL)
            .put("context", state.contextText)
            .put("icon", state.iconKey)
            .put("notes", state.notesText)
        prefs.edit().putString(KEY_IN_PROGRESS, o.toString()).apply()
    }

    private fun bootCount(): Int =
        Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT, -1)

    private fun readArray(key: String): List<JSONObject> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { array.getJSONObject(it) }
        } catch (e: org.json.JSONException) {
            emptyList()
        }
    }

    private fun writeArray(key: String, items: List<JSONObject>) =
        prefs.edit().putString(key, JSONArray(items).toString()).apply()

    private companion object {
        const val KEY_HISTORY = "history"
        const val KEY_REST_PRESETS = "rest_presets"
        const val KEY_SELECTED_REST = "selected_rest"
        const val KEY_IN_PROGRESS = "in_progress"
    }
}
