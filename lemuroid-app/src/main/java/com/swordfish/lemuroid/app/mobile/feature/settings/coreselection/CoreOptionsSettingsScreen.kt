package com.swordfish.lemuroid.app.mobile.feature.settings.coreselection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsItems
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.GameSystem

/**
 * Core of a system and its options, the same ones shown in the in-game menu.
 * Options are only known after the core has run once (see CoreOptionsCache).
 */
@Composable
fun CoreOptionsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: CoresSelectionViewModel,
    systemId: String,
    // On TV there is no top bar, so the system name is shown at the top of the page.
    showTitle: Boolean = false,
) {
    val applicationContext = LocalContext.current.applicationContext
    val system = remember(systemId) { GameSystem.findById(systemId) }
    val selectedCores = viewModel.getSelectedCores().collectAsState(emptyList()).value
    val selectedCore = selectedCores.firstOrNull { it.system.id == system.id }?.coreConfig ?: return
    val indexingInProgress = viewModel.indexingInProgress.collectAsState(false).value
    val definitions = remember(selectedCore.coreID) { viewModel.getCachedOptions(selectedCore) }
    var resetCount by remember { mutableIntStateOf(0) }

    LemuroidSettingsPage(modifier = modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(system.titleResId),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        LemuroidCardSettingsGroup(title = { Text(text = stringResource(R.string.core_options_category_core)) }) {
            if (system.systemCoreConfigs.size > 1) {
                val state = rememberMemoryIntSettingState(system.systemCoreConfigs.indexOf(selectedCore))
                LemuroidSettingsList(
                    state = state,
                    title = { Text(text = stringResource(R.string.core_options_category_core)) },
                    items = system.systemCoreConfigs.map { it.coreID.coreDisplayName },
                    enabled = !indexingInProgress,
                    onItemSelected = { index, _ ->
                        val newCore = system.systemCoreConfigs[index]
                        if (newCore != selectedCore) {
                            viewModel.changeCore(system, newCore, applicationContext)
                        }
                    },
                )
            } else {
                LemuroidSettingsMenuLink(
                    enabled = false,
                    title = { Text(text = stringResource(R.string.core_options_category_core)) },
                    subtitle = {
                        Text(text = stringResource(R.string.core_options_single_core, selectedCore.coreID.coreDisplayName))
                    },
                    onClick = { },
                )
            }
        }

        if (definitions == null) {
            CoreOptionsMessage(stringResource(R.string.core_options_not_used_yet, stringResource(system.titleResId)))
            return@LemuroidSettingsPage
        }

        val options = viewModel.buildOptions(selectedCore, definitions, selectedCore.exposedSettings)
        val advancedOptions = viewModel.buildOptions(selectedCore, definitions, selectedCore.exposedAdvancedSettings)

        if (options.isEmpty() && advancedOptions.isEmpty()) {
            CoreOptionsMessage(stringResource(R.string.core_options_none))
            return@LemuroidSettingsPage
        }

        // Recreated after a reset, so the rows read the default values again.
        key(resetCount) {
            if (options.isNotEmpty()) {
                LemuroidCardSettingsGroup(
                    title = { Text(text = stringResource(R.string.core_options_category_options)) },
                ) {
                    CoreOptionsItems(system.id.dbname, options)
                }
            }
            if (advancedOptions.isNotEmpty()) {
                LemuroidCardSettingsGroup(
                    title = { Text(text = stringResource(R.string.core_options_category_advanced)) },
                ) {
                    CoreOptionsItems(system.id.dbname, advancedOptions)
                }
            }
        }

        LemuroidCardSettingsGroup {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(R.string.core_options_reset)) },
                onClick = {
                    viewModel.resetCoreOptions(system, selectedCore)
                    resetCount++
                },
            )
        }
    }
}

@Composable
private fun CoreOptionsMessage(message: String) {
    LemuroidCardSettingsGroup(title = { Text(text = stringResource(R.string.core_options_category_options)) }) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
