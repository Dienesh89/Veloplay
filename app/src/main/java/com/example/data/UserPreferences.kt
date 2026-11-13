package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "veloplay_settings")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light")
}

data class PlayerSettings(
    val autoplayNext: Boolean = true,
    val rememberSpeedPerVideo: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val defaultPitch: Float = 1.0f,
    val defaultPitchLock: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val showEngineStats: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundPlayEnabled: Boolean = true,
    val autoPipEnabled: Boolean = true
)

class UserPreferences(private val context: Context) {

    private object Keys {
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val REMEMBER_SPEED = booleanPreferencesKey("remember_speed")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val DEFAULT_PITCH = floatPreferencesKey("default_pitch")
        val DEFAULT_PITCH_LOCK = booleanPreferencesKey("default_pitch_lock")
        val DOUBLE_TAP_SEEK_SECONDS = intPreferencesKey("double_tap_seek_seconds")
        val SHOW_ENGINE_STATS = booleanPreferencesKey("show_engine_stats")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val BACKGROUND_PLAY = booleanPreferencesKey("background_play")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
    }

    val settingsFlow: Flow<PlayerSettings> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.SYSTEM)
        PlayerSettings(
            autoplayNext = prefs[Keys.AUTOPLAY_NEXT] ?: true,
            rememberSpeedPerVideo = prefs[Keys.REMEMBER_SPEED] ?: true,
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1.0f,
            defaultPitch = prefs[Keys.DEFAULT_PITCH] ?: 1.0f,
            defaultPitchLock = prefs[Keys.DEFAULT_PITCH_LOCK] ?: true,
            doubleTapSeekSeconds = prefs[Keys.DOUBLE_TAP_SEEK_SECONDS] ?: 10,
            showEngineStats = prefs[Keys.SHOW_ENGINE_STATS] ?: false,
            themeMode = themeMode,
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY] ?: true,
            autoPipEnabled = prefs[Keys.AUTO_PIP] ?: true
        )
    }

    suspend fun setAutoplayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTOPLAY_NEXT] = enabled }
    }

    suspend fun setRememberSpeedPerVideo(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_SPEED] = enabled }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed.coerceIn(0.25f, 8.0f) }
    }

    suspend fun setDefaultPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_PITCH] = pitch.coerceIn(0.5f, 2.0f) }
    }

    suspend fun setDefaultPitchLock(locked: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_PITCH_LOCK] = locked }
    }

    suspend fun setDoubleTapSeekSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.DOUBLE_TAP_SEEK_SECONDS] = seconds }
    }

    suspend fun setShowEngineStats(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ENGINE_STATS] = show }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setBackgroundPlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAY] = enabled }
    }

    suspend fun setAutoPipEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PIP] = enabled }
    }
}
