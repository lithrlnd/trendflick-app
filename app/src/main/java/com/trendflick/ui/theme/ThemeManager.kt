package com.trendflick.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_IS_DARK = "is_dark_theme"
    private const val KEY_PRIMARY_HUE = "primary_hue"
    private const val DEFAULT_HUE = 265f // Default purple hue

    private lateinit var prefs: SharedPreferences
    
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _primaryHue = MutableStateFlow(DEFAULT_HUE)
    val primaryHue: StateFlow<Float> = _primaryHue.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Load saved preferences
        _isDarkTheme.value = prefs.getBoolean(KEY_IS_DARK, true)
        _primaryHue.value = prefs.getFloat(KEY_PRIMARY_HUE, DEFAULT_HUE)
    }

    fun updateThemeMode(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
    }

    fun updatePrimaryHue(hue: Float) {
        _primaryHue.value = hue
        prefs.edit().putFloat(KEY_PRIMARY_HUE, hue).apply()
    }

    fun getColorScheme(isDark: Boolean, hue: Float): ColorScheme {
        // Calculate color variations based on the selected hue
        val primary = Color.hsv(hue, 0.85f, 0.95f)
        val primaryContainer = Color.hsv(hue, 0.75f, 0.85f)
        val secondary = Color.hsv((hue + 120f) % 360f, 0.75f, 0.9f)
        val tertiary = Color.hsv((hue + 240f) % 360f, 0.75f, 0.9f)

        return if (isDark) {
            darkColorScheme(
                primary = primary,
                primaryContainer = primaryContainer,
                secondary = secondary,
                tertiary = tertiary,
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        } else {
            lightColorScheme(
                primary = primary,
                primaryContainer = primaryContainer,
                secondary = secondary,
                tertiary = tertiary,
                background = Color(0xFFFAFAFA),
                surface = Color.White,
                onPrimary = Color.White,
                onSecondary = Color.White,
                onTertiary = Color.White,
                onBackground = Color(0xFF121212),
                onSurface = Color(0xFF121212)
            )
        }
    }
} 