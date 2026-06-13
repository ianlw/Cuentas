package com.cuentas.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { DARK, LIGHT, CUSTOM }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cuentas_preferences")

class ThemePreferences(private val dataStore: DataStore<Preferences>) {

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_BALANCE = floatPreferencesKey("balance")
        private val KEY_COLOR1 = intPreferencesKey("custom_color1")
        private val KEY_COLOR2 = intPreferencesKey("custom_color2")
        private val KEY_COLOR3 = intPreferencesKey("custom_color3")
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        try { ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.DARK.name) }
        catch (e: Exception) { ThemeMode.DARK }
    }

    val balance: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_BALANCE] ?: 0f
    }

    val customColors: Flow<Triple<Int, Int, Int>> = dataStore.data.map { prefs ->
        Triple(
            prefs[KEY_COLOR1] ?: 0xFF7C3AED.toInt(),
            prefs[KEY_COLOR2] ?: 0xFF06B6D4.toInt(),
            prefs[KEY_COLOR3] ?: 0xFFF59E0B.toInt()
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setBalance(amount: Float) {
        dataStore.edit { it[KEY_BALANCE] = amount }
    }

    suspend fun setCustomColors(color1: Int, color2: Int, color3: Int) {
        dataStore.edit {
            it[KEY_COLOR1] = color1
            it[KEY_COLOR2] = color2
            it[KEY_COLOR3] = color3
        }
    }
}
