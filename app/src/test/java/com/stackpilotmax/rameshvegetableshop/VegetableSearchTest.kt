package com.stackpilotmax.rameshvegetableshop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VegetableSearchTest {
    private val vegetables = listOf(
        VegetableOption("Methi", "Bunch", "🌿"),
        VegetableOption("Palak", "Bunch", "🥬"),
        VegetableOption("Hara Kanda", "Bunch", "🧅"),
        VegetableOption("Desi Kothmir", "Bunch", "🌱"),
        VegetableOption("Hari Mirch", "Kg", "🌶️"),
        VegetableOption("Shimla Mirch", "Kg", "🫑"),
        VegetableOption("Kakdi", "Kg", "🥒")
    )

    @Test
    fun `misspelled methi finds Methi first`() {
        assertEquals(
            "Methi",
            VegetableSearchEngine.search("meti", vegetables).first().vegetable.name
        )
    }

    @Test
    fun `misspelled shimla mirch finds compound vegetable before generic mirch`() {
        assertEquals(
            "Shimla Mirch",
            VegetableSearchEngine.search("shimla mirh", vegetables).first().vegetable.name
        )
    }

    @Test
    fun `kakadi alias finds Kakdi`() {
        assertEquals(
            "Kakdi",
            VegetableSearchEngine.search("kakadi", vegetables).first().vegetable.name
        )
    }

    @Test
    fun `dhaniya alias finds Desi Kothmir`() {
        assertEquals(
            "Desi Kothmir",
            VegetableSearchEngine.search("dhaniya", vegetables).first().vegetable.name
        )
    }

    @Test
    fun `wrong hara kanda still returns nearest option`() {
        val results = VegetableSearchEngine.search("hara knda", vegetables)
        assertTrue(results.isNotEmpty())
        assertEquals("Hara Kanda", results.first().vegetable.name)
    }
}
