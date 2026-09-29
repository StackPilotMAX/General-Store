package com.stackpilotmax.rameshvegetableshop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceVegetableMatcherTest {
    private val vegetables = listOf(
        VegetableOption("Methi", "Bunch", "🌿"),
        VegetableOption("Palak", "Bunch", "🥬"),
        VegetableOption("Soya", "Bunch", "🌱"),
        VegetableOption("Hara Kanda", "Bunch", "🧅"),
        VegetableOption("China Kothmir", "Bunch", "🌿"),
        VegetableOption("Pudina", "Bunch", "🍃"),
        VegetableOption("Chaulai", "Bunch", "🥬"),
        VegetableOption("Mooli ke Patte", "Bunch", "🌿"),
        VegetableOption("Desi Kothmir", "Bunch", "🌱"),
        VegetableOption("Mooli", "Bunch", "🥕"),
        VegetableOption("Hari Mirch", "Kg", "🌶️"),
        VegetableOption("Naya Adrak", "Kg", "🫚"),
        VegetableOption("Purana Adrak", "Kg", "🫚"),
        VegetableOption("Kadi Patta", "Kg", "🍃"),
        VegetableOption("Nimbu", "Kg", "🍋"),
        VegetableOption("Gobhi", "Kg", "🥦"),
        VegetableOption("Shimla Mirch", "Kg", "🫑"),
        VegetableOption("Chota Lahsun", "Kg", "🧄"),
        VegetableOption("Bada Lahsun", "Kg", "🧄"),
        VegetableOption("Kakdi", "Kg", "🥒")
    )

    private fun match(vararg hypotheses: String): String? =
        VoiceVegetableMatcher.bestMatch(hypotheses.toList(), vegetables)?.vegetable?.name

    @Test
    fun hindiCommandOpensMethi() {
        assertEquals("Methi", match("मेथी खोलो"))
    }

    @Test
    fun marathiShopNamesAreRecognized() {
        assertEquals("Soya", match("शेपू"))
        assertEquals("Hara Kanda", match("हिरवा कांदा"))
        assertEquals("Shimla Mirch", match("ढोबळी मिरची"))
        assertEquals("Nimbu", match("लिंबू"))
    }

    @Test
    fun longestAliasWinsForMooliLeaves() {
        assertEquals("Mooli ke Patte", match("मूली के पत्ते का मेनू खोलो"))
    }

    @Test
    fun chinaAndDesiCorianderStaySeparate() {
        assertEquals("China Kothmir", match("चाइना कोथिंबीर"))
        assertEquals("Desi Kothmir", match("धनिया"))
    }

    @Test
    fun hinglishAndEnglishAliasesAreRecognized() {
        assertEquals("Kakdi", match("kheera"))
        assertEquals("Kadi Patta", match("curry leaves"))
        assertEquals("Hari Mirch", match("green chilli"))
    }

    @Test
    fun smallRecognitionSpellingErrorStillMatches() {
        assertEquals("Shimla Mirch", match("shmila mirch"))
    }

    @Test
    fun checksAllRecognizerHypotheses() {
        assertEquals("Gobhi", match("copy", "गोभी", "gobi"))
    }

    @Test
    fun unrelatedSpeechDoesNotOpenWrongMenu() {
        assertNull(match("customer ka bill save karo"))
    }
}
