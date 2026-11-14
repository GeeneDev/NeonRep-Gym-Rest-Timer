package com.geenedev.gymreps

import com.geenedev.gymreps.model.ExerciseGroup
import com.geenedev.gymreps.model.GroupAdvance
import com.geenedev.gymreps.model.GroupItem
import com.geenedev.gymreps.model.GroupRun
import com.geenedev.gymreps.model.Groups
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroupsTest {
    private val a = GroupItem("Bench press", "dumbbell")
    private val b = GroupItem("Dips", "body")
    private val c = GroupItem("Push-ups", "bolt")

    @Test
    fun doneMovesToTheNextExercise() {
        val (run, adv) = Groups.advance(GroupRun(1, 0), 3)
        assertEquals(GroupAdvance.Next(1), adv)
        assertEquals(GroupRun(1, 1, setOf(0)), run)
    }

    @Test
    fun lastDoneFinishesTheGroup() {
        var run = GroupRun(1, 0)
        repeat(2) { run = Groups.advance(run, 3).first }
        assertEquals(GroupAdvance.Finished, Groups.advance(run, 3).second)
    }

    @Test
    fun nextMovesForwardThenWrapsToSkipped() {
        // Jumped from 0 straight to 2, so 0 and 1 were skipped: Next goes to 3, then back to 0, 1.
        val r1 = Groups.advance(GroupRun(1, 2), 4)
        assertEquals(GroupAdvance.Next(3), r1.second)
        val r2 = Groups.advance(r1.first, 4)
        assertEquals(GroupAdvance.Next(0), r2.second)
        val r3 = Groups.advance(r2.first, 4)
        assertEquals(GroupAdvance.Next(1), r3.second)
        assertEquals(GroupAdvance.Finished, Groups.advance(r3.first, 4).second)
    }

    @Test
    fun nextIndexKeepsGoingForwardWithoutDone() {
        // Skipping ahead with Next and nothing saved: 0 -> 1 -> 2 -> 3, never back to 0.
        assertEquals(2, Groups.nextIndex(GroupRun(1, 1), 4))
        assertEquals(3, Groups.nextIndex(GroupRun(1, 2), 4))
        assertEquals(0, Groups.nextIndex(GroupRun(1, 3), 4))
        assertEquals(2, Groups.nextIndex(GroupRun(1, 0, setOf(1)), 4))
        assertNull(Groups.nextIndex(GroupRun(1, 2, setOf(0, 1)), 3))
    }

    @Test
    fun moveSwapsNeighboursAndIgnoresEdges() {
        assertEquals(listOf(b, a, c), Groups.move(listOf(a, b, c), 1, -1))
        assertEquals(listOf(a, b, c), Groups.move(listOf(a, b, c), 0, -1))
        assertEquals(listOf(a, b, c), Groups.move(listOf(a, b, c), 2, +1))
    }

    @Test
    fun addItemTrimsAndSkipsBlank() {
        assertEquals(listOf(GroupItem("Dips", "body")), Groups.addItem(emptyList(), "  Dips ", "body"))
        assertEquals(emptyList<GroupItem>(), Groups.addItem(emptyList(), "   ", "body"))
    }

    @Test
    fun upsertReplacesById() {
        val g = ExerciseGroup(5, "Push", listOf(a))
        val edited = g.copy(name = "Push day")
        assertEquals(listOf(edited), Groups.upsert(listOf(g), edited))
        assertEquals(2, Groups.upsert(listOf(g), g.copy(id = 6)).size)
    }

    @Test
    fun fitKeepsRunInsideAShorterGroup() {
        assertEquals(GroupRun(1, 1, setOf(0)), Groups.fit(GroupRun(1, 4, setOf(0, 3)), 2))
        assertNull(Groups.fit(GroupRun(1, 0), 0))
    }
}
