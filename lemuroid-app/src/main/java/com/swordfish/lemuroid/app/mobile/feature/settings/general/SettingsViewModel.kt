package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.content.Context
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import com.swordfish.lemuroid.lib.storage.cache.CacheCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    context: Context,
    private val settingsInteractor: SettingsInteractor,
    saveSyncManager: SaveSyncManager,
    sharedPreferences: FlowSharedPreferences,
) : ViewModel() {
    class Factory(
        private val context: Context,
        private val settingsInteractor: SettingsInteractor,
        private val saveSyncManager: SaveSyncManager,
        private val sharedPreferences: FlowSharedPreferences,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                context,
                settingsInteractor,
                saveSyncManager,
                sharedPreferences,
            ) as T
        }
    }

    data class State(
        val currentDirectory: String = "",
        val isSaveSyncSupported: Boolean = false,
    )

    data class CacheState(
        val default: String,
        val values: List<String>,
        val displayNames: List<String>,
    )

    val cacheState =
        flow { emit(buildCacheState(context)) }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val indexingInProgress = PendingOperationsMonitor(context).anyLibraryOperationInProgress()

    val directoryScanInProgress = PendingOperationsMonitor(context).isDirectoryScanInProgress()

    val uiState =
        sharedPreferences.getString(context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder))
            .asFlow()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.Lazily, "")
            .map { State(it, saveSyncManager.isSupported()) }

    fun changeLocalStorageFolder() {
        settingsInteractor.changeLocalStorageFolder()
    }

    fun resetAllSettings() {
        settingsInteractor.resetAllSettings()
    }

    private fun buildCacheState(context: Context): CacheState {
        val supportedCacheValues = CacheCleaner.getSupportedCacheLimits()
        return CacheState(
            default = CacheCleaner.getDefaultCacheLimit().toString(),
            values = supportedCacheValues.map { it.toString() },
            displayNames = supportedCacheValues.map { Formatter.formatShortFileSize(context, it) },
        )
    }
}
