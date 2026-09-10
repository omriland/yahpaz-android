package com.yahpz.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PatrolCallsignTest {
    @Test
    fun splitsNumberOnlyIntoNumber() {
        assertEquals(SplitPatrolCallsign("", "411"), splitPatrolCallsign("411"))
    }

    @Test
    fun splitsTextOnlyIntoPrefix() {
        assertEquals(SplitPatrolCallsign("אביב", ""), splitPatrolCallsign("אביב"))
    }

    @Test
    fun extractsLastDigitRun() {
        assertEquals(SplitPatrolCallsign("אביב", "411"), splitPatrolCallsign("אביב 411"))
        assertEquals(SplitPatrolCallsign("אביב", "411"), splitPatrolCallsign("אביב411"))
        assertEquals(SplitPatrolCallsign("אביב ב", "411"), splitPatrolCallsign("אביב 411 ב"))
    }

    @Test
    fun capsLongDigitRunAtFive() {
        assertEquals(SplitPatrolCallsign("", "12345"), splitPatrolCallsign("123456"))
    }
}
