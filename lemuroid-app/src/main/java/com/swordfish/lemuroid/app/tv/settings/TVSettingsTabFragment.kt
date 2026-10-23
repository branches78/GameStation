package com.swordfish.lemuroid.app.tv.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.composable
import com.swordfish.lemuroid.app.mobile.feature.settings.bios.BiosScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.bios.BiosSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoreOptionsSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoresSelectionScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.coreselection.CoresSelectionViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.general.SettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.general.SettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.inputdevices.InputDevicesSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.inputdevices.InputDevicesSettingsViewModel
import com.swordfish.lemuroid.app.mobile.feature.settings.savesync.SaveSyncSettingsScreen
import com.swordfish.lemuroid.app.mobile.feature.settings.savesync.SaveSyncSettingsViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.app.utils.android.settings.LocalSettingsFocusMemory
import com.swordfish.lemuroid.app.utils.android.settings.SettingsFocusMemory
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Content of the "Settings" tab on TV. It reuses the mobile settings screens (same cards and options),
 * hiding the ones that only make sense on phones.
 */
class TVSettingsTabFragment : Fragment() {
    @Inject
    lateinit var settingsInteractor: SettingsInteractor

    @Inject
    lateinit var saveSyncManager: SaveSyncManager

    @Inject
    lateinit var biosManager: BiosManager

    @Inject
    lateinit var coresSelection: CoresSelection

    @Inject
    lateinit var inputDeviceManager: InputDeviceManager

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val appContext = requireContext().applicationContext
        val application = requireActivity().application

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        val navController = rememberNavController()
                        // widthIn goes first: after fillMaxWidth the maximum width would be ignored.
                        val contentModifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH).fillMaxWidth()

                        NavHost(
                            modifier = contentModifier,
                            navController = navController,
                            startDestination = MainRoute.SETTINGS.route,
                        ) {
                            composable(MainRoute.SETTINGS) {
                                // The first screen is shown with the tab: the focus stays on the tabs bar.
                                TVSettingsDestination(focusFirstItem = false) {
                                    SettingsScreen(
                                        viewModel =
                                            viewModel(
                                                factory =
                                                    SettingsViewModel.Factory(
                                                        appContext,
                                                        settingsInteractor,
                                                        saveSyncManager,
                                                        FlowSharedPreferences(
                                                            SharedPreferencesHelper.getLegacySharedPreferences(appContext),
                                                        ),
                                                    ),
                                            ),
                                        navController = navController,
                                        isTV = true,
                                    )
                                }
                            }
                            composable(MainRoute.SETTINGS_BIOS) {
                                TVSettingsDestination {
                                    BiosScreen(
                                        viewModel = viewModel(factory = BiosSettingsViewModel.Factory(biosManager)),
                                    )
                                }
                            }
                            composable(MainRoute.SETTINGS_CORES_SELECTION) {
                                TVSettingsDestination {
                                    CoresSelectionScreen(
                                        viewModel =
                                            viewModel(
                                                factory = CoresSelectionViewModel.Factory(appContext, coresSelection),
                                            ),
                                        navController = navController,
                                    )
                                }
                            }
                            composable(MainRoute.SETTINGS_CORE_OPTIONS) { entry ->
                                TVSettingsDestination {
                                    CoreOptionsSettingsScreen(
                                        viewModel =
                                            viewModel(
                                                factory = CoresSelectionViewModel.Factory(appContext, coresSelection),
                                            ),
                                        systemId = entry.arguments?.getString("systemId")!!,
                                        showTitle = true,
                                    )
                                }
                            }
                            composable(MainRoute.SETTINGS_INPUT_DEVICES) {
                                TVSettingsDestination {
                                    InputDevicesSettingsScreen(
                                        viewModel =
                                            viewModel(
                                                factory = InputDevicesSettingsViewModel.Factory(appContext, inputDeviceManager),
                                            ),
                                    )
                                }
                            }
                            composable(MainRoute.SETTINGS_SAVE_SYNC) {
                                TVSettingsDestination {
                                    SaveSyncSettingsScreen(
                                        viewModel =
                                            viewModel(
                                                factory = SaveSyncSettingsViewModel.Factory(application, saveSyncManager),
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Screen of the settings navigation. When it opens it moves the focus to its first item ([focusFirstItem]),
     * and when it is shown again with Back, to the item that opened the other screen. Otherwise the focus would
     * jump to the search button, the first focusable view of the window.
     */
    @OptIn(FlowPreview::class)
    @Composable
    private fun TVSettingsDestination(
        focusFirstItem: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        var clickedItemKey by rememberSaveable { mutableStateOf<Int?>(null) }
        val focusMemory = remember { SettingsFocusMemory() }
        focusMemory.onItemClicked = { clickedItemKey = it }

        CompositionLocalProvider(LocalSettingsFocusMemory provides focusMemory) {
            content()
        }

        LaunchedEffect(Unit) {
            val restoreKey = clickedItemKey
            if (restoreKey == null && !focusFirstItem) return@LaunchedEffect

            if (restoreKey != null) {
                val target =
                    withTimeoutOrNull(ITEMS_TIMEOUT_MS) {
                        snapshotFlow { focusMemory.item(restoreKey) }.filterNotNull().first()
                    } ?: focusMemory.firstItem()
                clickedItemKey = null
                target?.let { runCatching { it.requestFocus() } }
                return@LaunchedEffect
            }

            // Some screens load their items in the background, and those at the top may arrive after the ones at
            // the bottom (e.g. the gamepads of "External devices", after "Reset bindings"). For a while the focus
            // follows the topmost item, as long as it stays where it was put (the user didn't move it).
            var focused: FocusRequester? = null
            withTimeoutOrNull(FIRST_ITEM_WATCH_MS) {
                snapshotFlow { focusMemory.firstItem() }
                    .debounce(ITEMS_SETTLE_MS)
                    .filterNotNull()
                    // Stops as soon as the user moved the focus to another item.
                    .takeWhile { focused.let { it == null || focusMemory.hasFocus(it) } }
                    .collect { first ->
                        if (first != focused && runCatching { first.requestFocus() }.isSuccess) {
                            focused = first
                        }
                    }
            }
        }
    }

    companion object {
        private const val ITEMS_TIMEOUT_MS = 1000L
        private const val ITEMS_SETTLE_MS = 250L
        private const val FIRST_ITEM_WATCH_MS = 3000L

        // The settings cards have 16dp of side padding: they end up 672dp wide, like the "Consoles" grid
        // (5 cards of 128dp + 4 spacings of 8dp) and the "About" cards.
        private val CONTENT_MAX_WIDTH = 704.dp
    }

    @dagger.Module
    class Module
}
