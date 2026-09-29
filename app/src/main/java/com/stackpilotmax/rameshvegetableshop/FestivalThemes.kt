package com.stackpilotmax.rameshvegetableshop

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Vendor-selectable Hindu festival themes. The selected key is persisted locally. */
data class FestivalTheme(
    val key: String,
    val name: String,
    val emoji: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color
)

object FestivalThemes {
    val all = listOf(
        FestivalTheme("diwali", "Diwali", "🪔", Color(0xFF5A143F), Color(0xFF6A1B78), Color(0xFFFFC83D), Color(0xFFFFF0D2), Color(0xFFF7E8FF)),
        FestivalTheme("holi", "Holi", "🌈", Color(0xFF7B1FA2), Color(0xFF00A896), Color(0xFFFF6B6B), Color(0xFFFFF0F7), Color(0xFFE7FFF7)),
        FestivalTheme("ganesh", "Ganesh Chaturthi", "🐘", Color(0xFF9C2F12), Color(0xFFE67E22), Color(0xFFFFC107), Color(0xFFFFF1D6), Color(0xFFFFE5D0)),
        FestivalTheme("navratri", "Navratri", "🔱", Color(0xFF8E1538), Color(0xFFC2185B), Color(0xFFFFB300), Color(0xFFFFE9EF), Color(0xFFFFF4D6)),
        FestivalTheme("dussehra", "Dussehra", "🏹", Color(0xFF8B1E1E), Color(0xFFD97706), Color(0xFFFFC107), Color(0xFFFFEAD0), Color(0xFFFFF7E6)),
        FestivalTheme("janmashtami", "Janmashtami", "🦚", Color(0xFF174A7E), Color(0xFF2E7D8F), Color(0xFFFFD54F), Color(0xFFE7F5FF), Color(0xFFF2ECFF)),
        FestivalTheme("rakhi", "Raksha Bandhan", "🎀", Color(0xFF8E3B7A), Color(0xFFD45D8C), Color(0xFFFFC1D9), Color(0xFFFFEEF8), Color(0xFFF5E8FF)),
        FestivalTheme("sankranti", "Makar Sankranti", "🪁", Color(0xFF1565C0), Color(0xFF00897B), Color(0xFFFFA000), Color(0xFFE7F7FF), Color(0xFFE9FFF7))
    )

    fun byKey(key: String): FestivalTheme = all.firstOrNull { it.key == key } ?: all.first()
}

object FestivalThemeStore {
    private const val PREFS = "sabzibill_settings"
    private const val KEY = "festival_theme"
    private val _selected = MutableStateFlow(FestivalThemes.all.first().key)
    val selected = _selected.asStateFlow()

    fun load(context: Context) {
        val key = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, FestivalThemes.all.first().key)
            ?: FestivalThemes.all.first().key
        _selected.value = FestivalThemes.byKey(key).key
    }

    fun set(context: Context, key: String) {
        val valid = FestivalThemes.byKey(key).key
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, valid)
            .apply()
        _selected.value = valid
    }

    fun current(): FestivalTheme = FestivalThemes.byKey(_selected.value)
}
