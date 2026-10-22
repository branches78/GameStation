package com.swordfish.lemuroid.app.tv.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainTVViewModel(appContext: Context) : ViewModel() {
    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainTVViewModel(appContext) as T
        }
    }

    val inProgress =
        PendingOperationsMonitor(appContext)
            // The directory scan shows its own progress bar in the "Home" tab and in the settings.
            .anyOperationInProgressExceptScan()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = false,
            )
}
