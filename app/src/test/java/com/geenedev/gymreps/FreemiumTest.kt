package com.geenedev.gymreps

import com.geenedev.gymreps.model.Freemium
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreemiumTest {
    private val hour = 60 * 60 * 1000L

    @Test fun countdownGrowsPastLimit() {
        assertEquals(30, Freemium.skipSeconds(150))
        assertEquals(31, Freemium.skipSeconds(151))
        assertEquals(40, Freemium.skipSeconds(160))
    }

    @Test fun onlyNagsPastLimitWhenUnpaid() {
        assertFalse(Freemium.shouldNag(false, 149, 0, 1_000))
        assertTrue(Freemium.shouldNag(false, 150, 0, 1_000))
        assertTrue(Freemium.shouldNag(false, 151, 0, 1_000))
        assertFalse(Freemium.shouldNag(true, 500, 0, 1_000))
    }

    @Test fun skipSnoozesForAnHour() {
        val skip = 10 * hour
        assertFalse(Freemium.shouldNag(false, 160, skip, skip + hour - 1))
        assertTrue(Freemium.shouldNag(false, 160, skip, skip + hour))
        // Clock moved backwards: don't let a bad timestamp hide it forever.
        assertTrue(Freemium.shouldNag(false, 160, skip, skip - 1))
    }
}
