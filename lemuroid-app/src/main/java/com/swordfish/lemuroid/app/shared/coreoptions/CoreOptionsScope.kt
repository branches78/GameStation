package com.swordfish.lemuroid.app.shared.coreoptions

import android.content.Context
import androidx.core.content.edit
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import java.io.Serializable

/**
 * Where the core options are saved: for every game of a system (as in Settings) or only for one game. The values of
 * a game win over the ones of its system when it is played (see CoreVariablesManager).
 */
sealed interface CoreOptionsScope : Serializable {
    val systemId: String

    data class System(override val systemId: String) : CoreOptionsScope

    data class Game(override val systemId: String, val gameId: Int) : CoreOptionsScope

    fun preferenceKey(variable: String): String {
        return when (this) {
            is System -> CoreVariablesManager.computeSharedPreferenceKey(variable, systemId)
            is Game -> CoreVariablesManager.computeGamePreferenceKey(variable, gameId)
        }
    }

    companion object {
        /** Options with a value of their own for the game, as opposed to the ones that follow its system. */
        fun hasGameValue(
            context: Context,
            variable: String,
            gameId: Int,
        ): Boolean {
            val key = CoreVariablesManager.computeGamePreferenceKey(variable, gameId)
            return SharedPreferencesHelper.getSharedPreferences(context).contains(key)
        }

        /** Value chosen for the whole system, or null when the app or core default is used. */
        fun systemValue(
            context: Context,
            variable: String,
            systemId: String,
        ): String? {
            val key = CoreVariablesManager.computeSharedPreferenceKey(variable, systemId)
            return when (val value = SharedPreferencesHelper.getSharedPreferences(context).all[key]) {
                is Boolean -> if (value) "enabled" else "disabled"
                is String -> value
                else -> null
            }
        }

        /** Removes every core option value of a game, so it follows its system again. */
        fun clearGameValues(
            context: Context,
            gameId: Int,
        ) {
            val prefix = CoreVariablesManager.computeGamePreferencesPrefix(gameId)
            val preferences = SharedPreferencesHelper.getSharedPreferences(context)
            val keys = preferences.all.keys.filter { it.startsWith(prefix) }
            preferences.edit { keys.forEach { remove(it) } }
        }
    }
}
