package com.geenedev.gymreps.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.geenedev.gymreps.data.GroupStore
import com.geenedev.gymreps.model.ExerciseGroup
import com.geenedev.gymreps.model.GroupAdvance
import com.geenedev.gymreps.model.GroupRun
import com.geenedev.gymreps.model.Groups
import com.geenedev.gymreps.model.SavedContext

/**
 * Exercise groups on top of the normal workout flow. Starting a group loads its first
 * exercise into the Exercise field; Done saves as usual and then loads the next one.
 * Loading never starts the timer: the session stays idle until the user taps the core.
 */
class GroupsController(
    private val store: GroupStore,
    private val vm: WorkoutViewModel,
    private val newId: () -> Long = System::currentTimeMillis,
) {
    var groups by mutableStateOf(store.loadGroups())
        private set

    var run by mutableStateOf(
        store.loadRun()?.let { r -> store.loadGroups().find { it.id == r.groupId }?.let { Groups.fit(r, it.items.size) } },
    )
        private set

    /** The group being worked through, if any. */
    val activeGroup: ExerciseGroup? get() = run?.let { r -> groups.find { it.id == r.groupId } }

    /** Saves a new or edited group (new groups have id 0). Returns the stored group. */
    fun saveGroup(group: ExerciseGroup): ExerciseGroup {
        val stored = if (group.id == 0L) group.copy(id = newId()) else group
        groups = Groups.upsert(groups, stored)
        store.saveGroups(groups)
        run?.let { r -> if (r.groupId == stored.id) updateRun(Groups.fit(r, stored.items.size)) }
        return stored
    }

    fun deleteGroup(id: Long) {
        groups = groups.filterNot { it.id == id }
        store.saveGroups(groups)
        if (run?.groupId == id) updateRun(null)
    }

    /** Starts [group] from its first exercise. Any unsaved sets are cleared. */
    fun start(group: ExerciseGroup) {
        if (group.items.isEmpty()) return
        updateRun(GroupRun(group.id, 0))
        load(group, 0)
    }

    /** Switches to another exercise of the running group. Any unsaved sets are cleared. */
    fun jumpTo(index: Int) {
        val r = run ?: return
        val group = activeGroup ?: return
        if (index !in group.items.indices) return
        updateRun(r.copy(index = index))
        load(group, index)
    }

    /** Leaves the group; whatever is in the Exercise field stays as it is. */
    fun stop() = updateRun(null)

    /**
     * Call right after Done saved an exercise. Loads the next exercise of the group
     * (idle, waiting for the first tap) and returns where the group went; null when no group runs.
     */
    fun onSaved(): GroupAdvance? {
        val r = run ?: return null
        val group = activeGroup ?: return null
        val (next, advance) = Groups.advance(r, group.items.size)
        when (advance) {
            is GroupAdvance.Next -> {
                updateRun(next)
                load(group, advance.index)
            }
            GroupAdvance.Finished -> updateRun(null)
        }
        return advance
    }

    private fun load(group: ExerciseGroup, index: Int) {
        val item = group.items[index]
        vm.onReset()
        // Empty the fields first so the picked exercise brings back its last notes.
        vm.onNotesChange("")
        vm.onContextTextChange("")
        vm.onContextPicked(SavedContext(item.name, item.iconKey.ifEmpty { ContextIcons.DEFAULT_KEY }))
        // The loaded exercise brings back its own rest time, as last saved for it.
        vm.applyLatestRestFor(item.name)
    }

    private fun updateRun(value: GroupRun?) {
        run = value
        store.saveRun(value)
    }
}
