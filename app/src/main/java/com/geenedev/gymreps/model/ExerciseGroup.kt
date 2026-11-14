package com.geenedev.gymreps.model

/** One exercise slot in a group: the name typed into the Exercise field, and its icon. */
data class GroupItem(val name: String, val iconKey: String)

/** A named list of exercises (a routine), done in order and chained by Done. */
data class ExerciseGroup(
    val id: Long,
    val name: String,
    val items: List<GroupItem>,
)

/**
 * A group being worked through. [index] is the exercise loaded in the Exercise field;
 * [done] holds the positions already saved with Done (positions can be done out of order
 * when the user jumps around).
 */
data class GroupRun(
    val groupId: Long,
    val index: Int,
    val done: Set<Int> = emptySet(),
)

/** Where a group run goes after Done saved the current exercise. */
sealed interface GroupAdvance {
    data class Next(val index: Int) : GroupAdvance
    data object Finished : GroupAdvance
}

object Groups {
    /** Adds [group], or replaces the group with the same id. */
    fun upsert(groups: List<ExerciseGroup>, group: ExerciseGroup): List<ExerciseGroup> =
        if (groups.any { it.id == group.id }) groups.map { if (it.id == group.id) group else it }
        else groups + group

    /** Appends an exercise; blank names are ignored. Duplicates are allowed (e.g. a superset). */
    fun addItem(items: List<GroupItem>, name: String, iconKey: String): List<GroupItem> {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) items else items + GroupItem(trimmed, iconKey)
    }

    /** Moves the item at [from] one step up (-1) or down (+1). Out-of-range moves do nothing. */
    fun move(items: List<GroupItem>, from: Int, by: Int): List<GroupItem> {
        val to = from + by
        if (from !in items.indices || to !in items.indices) return items
        return items.toMutableList().apply { add(to, removeAt(from)) }
    }

    /**
     * The exercise Next goes to: the next one still to do after [run].index in the group's
     * order, so repeated Next keeps moving forward; once past the end it wraps to any skipped
     * earlier one. Null when nothing else is left.
     */
    fun nextIndex(run: GroupRun, size: Int): Int? =
        ((run.index + 1 until size) + (0 until run.index)).firstOrNull { it !in run.done }

    /**
     * After Done at [run].index: marks it done and moves to [nextIndex].
     * Finished when no exercise is left to do.
     */
    fun advance(run: GroupRun, size: Int): Pair<GroupRun, GroupAdvance> {
        val done = run.done + run.index
        val next = nextIndex(run.copy(done = done), size)
        return if (next == null) run.copy(done = done) to GroupAdvance.Finished
        else run.copy(index = next, done = done) to GroupAdvance.Next(next)
    }

    /**
     * Keeps a run valid after its group was edited: drops done marks past the end and moves
     * the index inside the list. Null when the group no longer has any exercise.
     */
    fun fit(run: GroupRun, size: Int): GroupRun? {
        if (size == 0) return null
        return run.copy(index = run.index.coerceIn(0, size - 1), done = run.done.filter { it < size }.toSet())
    }
}
