package com.yashd.assignment2

import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityTest {
    @Test fun existingBooleanStarsRemainReadable() {
        assertEquals(1, readPriority(true))
        assertEquals(0, readPriority(false))
        assertEquals(0, readPriority(null))
    }

    @Test fun firebaseNumbersSupportAllThreeLevels() {
        assertEquals(0, readPriority(0L))
        assertEquals(1, readPriority(1L))
        assertEquals(2, readPriority(2L))
    }

    @Test fun unexpectedValuesAreHandledSafely() {
        assertEquals(0, readPriority("high"))
        assertEquals(0, readPriority(-1L))
        assertEquals(2, readPriority(99L))
    }
}
