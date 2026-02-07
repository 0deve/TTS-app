package com.example.tts_app.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_app_prefs", Context.MODE_PRIVATE)

    fun saveBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    fun saveFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)

    fun saveString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default

    fun saveInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)

    fun saveLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()
    fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    companion object {
        const val KEY_SERVER_ENABLED = "server_enabled"
        const val KEY_TTS_SPEED = "tts_speed"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_SERVER_IP = "server_ip"
        const val KEY_LINE_HEIGHT = "line_height"
        const val KEY_TEXT_MARGIN = "text_margin"
        const val KEY_FONT_FAMILY = "font_family"
        const val KEY_OLED_MODE = "oled_mode"
        const val KEY_SELECTED_VOICE = "selected_voice"
        const val KEY_FONT_COLOR = "font_color"
        const val KEY_VOICE_PITCH = "voice_pitch"
    }
}