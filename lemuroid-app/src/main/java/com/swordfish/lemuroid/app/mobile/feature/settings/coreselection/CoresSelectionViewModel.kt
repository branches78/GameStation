package com.swordfish.lemuroid.app.mobile.feature.settings.coreselection

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOption
import com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.lib.core.CoreOptionsCache
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.ExposedSetting
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.launch

class CoresSelectionViewModel(
    context: Context,
    private val coresSelection: CoresSelection,
) : ViewModel() {
    class Factory(
        val context: Context,
        val coresSelection: CoresSelection,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoresSelectionViewModel(context, coresSelection) as T
        }
    }

    private val appContext = context.applicationContext
    private val coreOptionsCache = CoreOptionsCache(appContext)

    val indexingInProgress = PendingOperationsMonitor(context).anyLibraryOperationInProgress()

    fun getSelectedCores() = coresSelection.getSelectedCores()

    fun changeCore(
        system: GameSystem,
        coreConfig: SystemCoreConfig,
        context: Context,
    ) {
        viewModelScope.launch {
            coresSelection.updateCoreConfigForSystem(system, coreConfig.coreID)
            LibraryIndexScheduler.scheduleCoreUpdate(context)
        }
    }

    /** Options reported by the core the last time it ran, or null if it never ran on this device. */
    fun getCachedOptions(coreConfig: SystemCoreConfig): List<CoreOptionsCache.Definition>? {
        return coreOptionsCache.get(coreConfig.coreID)
    }

    /** Number of options of the core that can be changed from the app (normal and advanced). */
    fun countExposedOptions(
        coreConfig: SystemCoreConfig,
        definitions: List<CoreOptionsCache.Definition>,
    ): Int {
        val reportedKeys = definitions.map { it.key }.toSet()
        return (coreConfig.exposedSettings + coreConfig.exposedAdvancedSettings).count { it.key in reportedKeys }
    }

    fun buildOptions(
        coreConfig: SystemCoreConfig,
        definitions: List<CoreOptionsCache.Definition>,
        exposedSettings: List<ExposedSetting>,
    ): List<LemuroidCoreOption> {
        val appDefaults = coreConfig.defaultSettings.associate { it.key to it.value }
        return exposedSettings.mapNotNull { exposedSetting ->
            definitions.firstOrNull { it.key == exposedSetting.key }?.let { definition ->
                // The value set by the app (e.g. dynarec enabled) wins over the one seen when the core last ran.
                val value = appDefaults[definition.key] ?: definition.value
                val coreOption = CoreOption(CoreVariable(definition.key, value), definition.name, definition.values)
                LemuroidCoreOption(exposedSetting, coreOption)
            }
        }
    }

    /** Removes the values chosen by the user, so the app defaults are used again. */
    fun resetCoreOptions(
        system: GameSystem,
        coreConfig: SystemCoreConfig,
    ) {
        val keys =
            (coreConfig.exposedSettings + coreConfig.exposedAdvancedSettings)
                .map { CoreVariablesManager.computeSharedPreferenceKey(it.key, system.id.dbname) }
        SharedPreferencesHelper.getSharedPreferences(appContext).edit {
            keys.forEach { remove(it) }
        }
    }
}
