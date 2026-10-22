package com.swordfish.lemuroid.lib.core

import android.content.Context
import androidx.core.content.edit
import com.frybits.harmony.getHarmonySharedPreferences
import com.swordfish.lemuroid.lib.library.CoreID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Remembers the options each core reports while a game is running (name and allowed values),
 * so they can also be edited from the app settings when no game is open.
 *
 * Harmony preferences are used because games run in a separate process.
 */
class CoreOptionsCache(context: Context) {
    data class Definition(
        val key: String,
        val name: String,
        val values: List<String>,
        val value: String,
    )

    private val preferences = context.getHarmonySharedPreferences(PREFERENCES_NAME)

    /**
     * [gameOwnKeys] are the options with a value chosen only for the game being played: that value is not the one of
     * the system, so the previous one is kept (or the core default, the first value, the first time).
     */
    fun store(
        coreID: CoreID,
        definitions: List<Definition>,
        gameOwnKeys: Set<String> = emptySet(),
    ) {
        val previousValues = get(coreID)?.associate { it.key to it.value } ?: emptyMap()
        val json = JSONArray()
        definitions.forEach { reported ->
            val definition =
                if (reported.key in gameOwnKeys) {
                    val systemValue = previousValues[reported.key] ?: reported.values.firstOrNull()
                    reported.copy(value = systemValue ?: reported.value)
                } else {
                    reported
                }
            json.put(
                JSONObject()
                    .put(KEY_KEY, definition.key)
                    .put(KEY_NAME, definition.name)
                    .put(KEY_VALUES, JSONArray(definition.values))
                    .put(KEY_VALUE, definition.value),
            )
        }
        preferences.edit { putString(coreID.coreName, json.toString()) }
    }

    /** Null when the core has never been run on this device. */
    fun get(coreID: CoreID): List<Definition>? {
        val stored = preferences.getString(coreID.coreName, null) ?: return null
        return runCatching {
            val json = JSONArray(stored)
            (0 until json.length()).map { index ->
                val item = json.getJSONObject(index)
                val values = item.getJSONArray(KEY_VALUES)
                Definition(
                    key = item.getString(KEY_KEY),
                    name = item.getString(KEY_NAME),
                    values = (0 until values.length()).map { values.getString(it) },
                    value = item.getString(KEY_VALUE),
                )
            }
        }.getOrNull()
    }

    companion object {
        private const val PREFERENCES_NAME = "core_options_cache"
        private const val KEY_KEY = "key"
        private const val KEY_NAME = "name"
        private const val KEY_VALUES = "values"
        private const val KEY_VALUE = "value"
    }
}
