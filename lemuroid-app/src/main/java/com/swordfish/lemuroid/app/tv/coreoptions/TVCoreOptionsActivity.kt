package com.swordfish.lemuroid.app.tv.coreoptions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoreOptionsSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoresSelectionViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** Core options of a system on TV, opened from the long press menu of a game (same screen as in Settings). */
class TVCoreOptionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val systemId = intent.getStringExtra(EXTRA_SYSTEM_ID)
        if (systemId == null) {
            finish()
            return
        }

        val coresSelection = CoresSelection { SharedPreferencesHelper.getSharedPreferences(applicationContext) }

        setContent {
            AppTheme {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CoreOptionsSettingsScreen(
                        modifier = Modifier.fillMaxWidth().widthIn(max = CONTENT_MAX_WIDTH.dp),
                        viewModel = viewModel(factory = CoresSelectionViewModel.Factory(applicationContext, coresSelection)),
                        systemId = systemId,
                        showTitle = true,
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SYSTEM_ID = "system_id"
        private const val CONTENT_MAX_WIDTH = 720

        fun newIntent(
            context: Context,
            systemId: String,
        ) = Intent(context, TVCoreOptionsActivity::class.java).putExtra(EXTRA_SYSTEM_ID, systemId)
    }
}
