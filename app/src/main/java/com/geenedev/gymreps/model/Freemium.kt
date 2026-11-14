package com.geenedev.gymreps.model

/**
 * Free version rules. Unpaid installs with [FREE_ENTRIES] or more saved exercises see a
 * "support GymReps" popup that can only be skipped after a countdown. After a skip it stays
 * away for [SNOOZE_MS].
 */
object Freemium {
    const val FREE_ENTRIES = 150
    const val BASE_SECONDS = 30
    const val SECONDS_PER_EXTRA_ENTRY = 1
    const val SNOOZE_MS = 60 * 60 * 1000L

    /** Countdown before "Skip" works: 30 s at 150 entries, +1 s for each entry past 150. */
    fun skipSeconds(entries: Int): Int =
        BASE_SECONDS + (entries - FREE_ENTRIES).coerceAtLeast(0) * SECONDS_PER_EXTRA_ENTRY

    /** True when the popup should be shown now. [lastSkipAt] is wall-clock ms, 0 if never. */
    fun shouldNag(unlocked: Boolean, entries: Int, lastSkipAt: Long, now: Long): Boolean =
        !unlocked && entries >= FREE_ENTRIES && (lastSkipAt <= 0L || now - lastSkipAt >= SNOOZE_MS || now < lastSkipAt)
}
