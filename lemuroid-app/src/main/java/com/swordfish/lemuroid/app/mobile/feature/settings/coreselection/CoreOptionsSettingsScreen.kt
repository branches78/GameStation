package com.swordfish.lemuroid.app.mobile.feature.settings.coreselection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionTitle
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsItems
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsScope
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsScopeSelector
import com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Core of a system and its options, the same ones shown in the in-game menu.
 * Options are only known after the core has run once (see CoreOptionsCache).
 * With a [game] (its "Configure core" entry) the core and the options can also be chosen only for that game:
 * the "Only this game" tab is selected at first.
 */
@Composable
fun CoreOptionsSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: CoresSelectionViewModel,
    systemId: String,
    // On TV there is no top bar, so the system name is shown at the top of the page.
    showTitle: Boolean = false,
    game: Game? = null,
) {
    val applicationContext = LocalContext.current.applicationContext
    val system = remember(systemId) { GameSystem.findById(systemId) }
    val systemName = stringResource(system.titleResId)
    var gameScope by rememberSaveable(game?.id) { mutableStateOf(game != null) }
    val scope =
        if (game != null && gameScope) {
            CoreOptionsScope.Game(system.id.dbname, game.id)
        } else {
            CoreOptionsScope.System(system.id.dbname)
        }

    val selectedCoresFlow = remember { viewModel.getSelectedCores() }
    val systemCore =
        selectedCoresFlow.collectAsState(emptyList()).value
            .firstOrNull { it.system.id == system.id }?.coreConfig
    val gameCoreFlow = remember(game?.id) { game?.let { viewModel.getSelectedCoreForGame(system, it.id) } }
    val gameCore = gameCoreFlow?.collectAsState(null)?.value
    val hasGameCoreFlow = remember(game?.id) { game?.let { viewModel.hasGameCore(it.id) } }
    val hasGameCore = hasGameCoreFlow?.collectAsState(false)?.value ?: false
    val selectedCore = (if (scope is CoreOptionsScope.Game) gameCore else systemCore) ?: return

    val indexingInProgress = viewModel.indexingInProgress.collectAsState(false).value
    val definitions = remember(selectedCore.coreID) { viewModel.getCachedOptions(selectedCore) }
    var resetCount by remember { mutableIntStateOf(0) }

    LemuroidSettingsPage(modifier = modifier.fillMaxWidth()) {
        if (game != null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = game.displayName(),
                    style = MaterialTheme.typography.titleLarge,
                    // Not inside a Surface: without an explicit color it would be black.
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = systemName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CoreOptionsScopeSelector(
                modifier = Modifier.padding(horizontal = 16.dp),
                systemName = systemName,
                gameScope = gameScope,
                onGameScopeChange = { gameScope = it },
            )
        } else if (showTitle) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = systemName,
                style = MaterialTheme.typography.titleLarge,
                // Not inside a Surface: without an explicit color it would be black.
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // Rows remember their values: they are created again for another tab, core or after a reset.
        key(scope, selectedCore.coreID, resetCount) {
            CoreSelection(
                system = system,
                selectedCore = selectedCore,
                gameValue = scope is CoreOptionsScope.Game && hasGameCore,
                enabled = !indexingInProgress,
                onCoreSelected = { newCore ->
                    if (scope is CoreOptionsScope.Game) {
                        viewModel.changeGameCore(scope.gameId, newCore, applicationContext)
                    } else {
                        viewModel.changeCore(system, newCore, applicationContext)
                    }
                },
            )

            val options =
                definitions?.let { viewModel.buildOptions(selectedCore, it, selectedCore.exposedSettings) }
                    ?: emptyList()
            val advancedOptions =
                definitions?.let { viewModel.buildOptions(selectedCore, it, selectedCore.exposedAdvancedSettings) }
                    ?: emptyList()
            val (displayOptions, otherOptions) = options.partition { it.getKey() in DISPLAY_OPTION_KEYS }
            val (advancedDisplayOptions, otherAdvancedOptions) =
                advancedOptions.partition { it.getKey() in DISPLAY_OPTION_KEYS }

            DisplayOptions(system, selectedCore, scope, displayOptions + advancedDisplayOptions)

            when {
                definitions == null -> {
                    CoreOptionsMessage(stringResource(R.string.core_options_not_used_yet, systemName))
                }
                options.isEmpty() && advancedOptions.isEmpty() -> {
                    CoreOptionsMessage(stringResource(R.string.core_options_none))
                }
                else -> {
                    if (otherOptions.isNotEmpty()) {
                        LemuroidCardSettingsGroup(
                            title = { Text(text = stringResource(R.string.core_options_category_options)) },
                        ) {
                            CoreOptionsItems(scope, otherOptions)
                        }
                    }
                    if (otherAdvancedOptions.isNotEmpty()) {
                        LemuroidCardSettingsGroup(
                            title = { Text(text = stringResource(R.string.core_options_category_advanced)) },
                        ) {
                            CoreOptionsItems(scope, otherAdvancedOptions)
                        }
                    }
                }
            }
        }

        LemuroidCardSettingsGroup {
            if (scope is CoreOptionsScope.Game) {
                LemuroidSettingsMenuLink(
                    title = { Text(text = stringResource(R.string.core_options_clear_game)) },
                    subtitle = {
                        Text(text = stringResource(R.string.core_options_clear_game_description, systemName))
                    },
                    onClick = {
                        viewModel.clearGameConfiguration(scope.gameId)
                        resetCount++
                    },
                )
            } else {
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
}

@Composable
private fun CoreSelection(
    system: GameSystem,
    selectedCore: SystemCoreConfig,
    gameValue: Boolean,
    enabled: Boolean,
    onCoreSelected: (SystemCoreConfig) -> Unit,
) {
    LemuroidCardSettingsGroup(title = { Text(text = stringResource(R.string.core_options_category_core)) }) {
        if (system.systemCoreConfigs.size > 1) {
            val state = rememberMemoryIntSettingState(system.systemCoreConfigs.indexOf(selectedCore))
            LemuroidSettingsList(
                state = state,
                title = { CoreOptionTitle(stringResource(R.string.core_options_category_core), gameValue) },
                items = system.systemCoreConfigs.map { it.coreID.coreDisplayName },
                enabled = enabled,
                onItemSelected = { index, _ ->
                    val newCore = system.systemCoreConfigs[index]
                    if (newCore != selectedCore) {
                        onCoreSelected(newCore)
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
}

/**
 * "Display" group with the aspect ratio. When the selected core can't change it but another core of the system
 * can (e.g. PCSX ReARMed and SwanStation), a disabled row explains which core to choose.
 */
@Composable
private fun DisplayOptions(
    system: GameSystem,
    selectedCore: SystemCoreConfig,
    scope: CoreOptionsScope,
    displayOptions: List<LemuroidCoreOption>,
) {
    val coreWithAspectRatio =
        system.systemCoreConfigs.firstOrNull { core ->
            (core.exposedSettings + core.exposedAdvancedSettings).any { it.key in DISPLAY_OPTION_KEYS }
        }
    val selectedCoreHasDisplayOptions =
        (selectedCore.exposedSettings + selectedCore.exposedAdvancedSettings).any { it.key in DISPLAY_OPTION_KEYS }

    when {
        displayOptions.isNotEmpty() -> {
            LemuroidCardSettingsGroup(
                title = { Text(text = stringResource(R.string.core_options_category_display)) },
            ) {
                CoreOptionsItems(scope, displayOptions)
            }
        }
        !selectedCoreHasDisplayOptions && coreWithAspectRatio != null -> {
            LemuroidCardSettingsGroup(
                title = { Text(text = stringResource(R.string.core_options_category_display)) },
            ) {
                LemuroidSettingsMenuLink(
                    enabled = false,
                    title = { Text(text = stringResource(com.swordfish.lemuroid.lib.R.string.setting_aspect_ratio)) },
                    subtitle = {
                        Text(text = stringResource(com.swordfish.lemuroid.lib.R.string.value_aspect_ratio_original))
                    },
                    onClick = { },
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    text =
                        stringResource(
                            R.string.core_options_aspect_ratio_unavailable,
                            selectedCore.coreID.coreDisplayName,
                            coreWithAspectRatio.coreID.coreDisplayName,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

// Options shown in the "Display" group: aspect ratio and widescreen hack.
private val DISPLAY_OPTION_KEYS =
    setOf("mupen64plus-aspect", "swanstation_Display_AspectRatio", "swanstation_GPU_WidescreenHack")
