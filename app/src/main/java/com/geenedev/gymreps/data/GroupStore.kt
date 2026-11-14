package com.geenedev.gymreps.data

import android.content.Context
import com.geenedev.gymreps.model.ExerciseGroup
import com.geenedev.gymreps.model.GroupItem
import com.geenedev.gymreps.model.GroupRun
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Exercise groups and the group in progress, in their own SharedPreferences file as JSON. */
class GroupStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("groups", Context.MODE_PRIVATE)

    fun loadGroups(): List<ExerciseGroup> {
        val raw = prefs.getString(KEY_GROUPS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val g = array.getJSONObject(i)
                val items = g.optJSONArray("items") ?: JSONArray()
                ExerciseGroup(
                    id = g.getLong("id"),
                    name = g.optString("name"),
                    items = List(items.length()) { j ->
                        val it = items.getJSONObject(j)
                        GroupItem(it.optString("name"), it.optString("icon"))
                    },
                )
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    fun saveGroups(groups: List<ExerciseGroup>) {
        val array = JSONArray(groups.map { g ->
            JSONObject()
                .put("id", g.id)
                .put("name", g.name)
                .put("items", JSONArray(g.items.map { JSONObject().put("name", it.name).put("icon", it.iconKey) }))
        })
        prefs.edit().putString(KEY_GROUPS, array.toString()).apply()
    }

    fun loadRun(): GroupRun? {
        val raw = prefs.getString(KEY_RUN, null) ?: return null
        return try {
            val o = JSONObject(raw)
            val done = o.optJSONArray("done") ?: JSONArray()
            GroupRun(
                groupId = o.getLong("groupId"),
                index = o.optInt("index"),
                done = List(done.length()) { done.getInt(it) }.toSet(),
            )
        } catch (e: JSONException) {
            null
        }
    }

    /** Pass null to forget the group in progress. */
    fun saveRun(run: GroupRun?) {
        if (run == null) {
            prefs.edit().remove(KEY_RUN).apply()
            return
        }
        val o = JSONObject()
            .put("groupId", run.groupId)
            .put("index", run.index)
            .put("done", JSONArray(run.done.sorted()))
        prefs.edit().putString(KEY_RUN, o.toString()).apply()
    }

    private companion object {
        const val KEY_GROUPS = "groups"
        const val KEY_RUN = "run"
    }
}
