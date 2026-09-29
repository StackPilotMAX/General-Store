package com.stackpilotmax.rameshvegetableshop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FestivalThemesTest {
    @Test
    fun vendorHasMoreThanFiveFestivalThemes() {
        assertTrue(FestivalThemes.all.size > 5)
        assertEquals(FestivalThemes.all.size, FestivalThemes.all.map { it.key }.toSet().size)
    }

    @Test
    fun unknownThemeFallsBackSafely() {
        assertEquals(FestivalThemes.all.first().key, FestivalThemes.byKey("missing").key)
    }
}
