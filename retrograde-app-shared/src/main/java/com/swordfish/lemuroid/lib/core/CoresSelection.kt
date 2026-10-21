package com.swordfish.lemuroid.lib.core

import android.content.SharedPreferences
import androidx.core.content.edit
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CoresSelection(
    private val sharedPreferencesFactory: Lazy<SharedPreferences>,
) {
    private val sharedPreferences by lazy { sharedPreferencesFactory.get() }

    private val flowSharedPreferences by lazy { FlowSharedPreferences(sharedPreferences) }

    data class SelectedCore(
        val system: GameSystem,
        val coreConfig: SystemCoreConfig,
    )

    fun getSelectedCores(): Flow<List<SelectedCore>> {
        // Every system is listed, including the ones with a single core.
        val configurationFlows =
            GameSystem.all().map { system ->
                getSelectedCoreConfigForSystem(system)
                    .map { SelectedCore(system, it) }
            }

        return combine(configurationFlows) { it.toList() }
    }

    suspend fun updateCoreConfigForSystem(
        system: GameSystem,
        coreID: CoreID,
    ) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(computeSystemPreferenceKey(system.id), coreID.coreName)
            .commit()
    }

    suspend fun getCoreConfigForSystem(system: GameSystem): SystemCoreConfig {
        return getSelectedCoreConfigForSystem(system).first()
    }

    private fun getSelectedCoreConfigForSystem(system: GameSystem): Flow<SystemCoreConfig> {
        return getSelectedCoreNameForSystem(system)
            .map { coreName ->
                // Fall back to the default core if the stored one is no longer available (e.g. DeSmuME).
                system.systemCoreConfigs.firstOrNull { it.coreID.coreName == coreName }
                    ?: system.systemCoreConfigs.first()
            }
    }

    private fun getSelectedCoreNameForSystem(system: GameSystem): Flow<String> {
        return flow {
            val preferenceKey = computeSystemPreferenceKey(system.id)
            val currentValue = flowSharedPreferences.sharedPreferences.getString(preferenceKey, null)
            var defaultValue = currentValue

            if (currentValue == null) {
                defaultValue = getDefaultCoreForSystem(system)
                flowSharedPreferences.sharedPreferences.edit(true) {
                    putString(preferenceKey, defaultValue)
                }
            }

            val preference =
                flowSharedPreferences.getString(
                    preferenceKey,
                    defaultValue,
                )
            emitAll(preference.asFlow())
        }.flowOn(Dispatchers.IO)
    }

    private fun getDefaultCoreForSystem(system: GameSystem): String {
        return system.systemCoreConfigs.first().coreID.coreName
    }

    companion object {
        private const val CORE_SELECTION_BINDING_PREFERENCE_BASE_KEY = "pref_key_core_selection"

        fun computeSystemPreferenceKey(systemID: SystemID) =
            "${CORE_SELECTION_BINDING_PREFERENCE_BASE_KEY}_${systemID.dbname}"
    }
}
