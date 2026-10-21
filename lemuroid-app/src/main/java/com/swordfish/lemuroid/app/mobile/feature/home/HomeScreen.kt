package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.shared.covers.CoverAspectRatio
import com.swordfish.lemuroid.app.shared.home.HomeDynamicRows
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
            }
            else -> { }
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    HomeScreen(
        modifier,
        state.value,
        onGameClick,
        onGameLongClick,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
    ) // TODO COMPOSE We need to understand what's going to happen here.
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(state.showNoNotificationPermissionCard) {
            HomeNotification(
                titleId = R.string.home_notification_title,
                messageId = R.string.home_notification_message,
                actionId = R.string.home_notification_action,
                onAction = onEnableNotificationsClicked,
            )
        }
        AnimatedVisibility(state.showNoGamesCard) {
            HomeNotification(
                titleId = R.string.home_empty_title,
                messageId = R.string.home_empty_message,
                actionId = R.string.home_empty_action,
                onAction = onSetDirectoryClicked,
                enabled = !state.indexInProgress,
            )
        }
        AnimatedVisibility(state.showNoMicrophonePermissionCard) {
            HomeNotification(
                titleId = R.string.home_microphone_title,
                messageId = R.string.home_microphone_message,
                actionId = R.string.home_microphone_action,
                onAction = onEnableMicrophoneClicked,
            )
        }
        HomeRow(
            stringResource(id = R.string.recent),
            state.recentGames,
            null,
            onGameClicked,
            onGameLongClick,
        )
        HomeRow(
            stringResource(id = R.string.favorites),
            state.favoritesGames,
            null,
            onGameClicked,
            onGameLongClick,
        )
        DynamicHomeRows(state.dynamicRows, HomeDynamicRows.Placement.BEFORE_DISCOVER, onGameClicked, onGameLongClick)
        HomeRow(
            stringResource(id = R.string.discover),
            state.discoveryGames,
            // With an empty library (e.g. first launch) the row stays hidden: the directory card is shown instead.
            stringResource(id = R.string.home_row_empty_discover).takeUnless { state.showNoGamesCard },
            onGameClicked,
            onGameLongClick,
        )
        DynamicHomeRows(state.dynamicRows, HomeDynamicRows.Placement.AFTER_DISCOVER, onGameClicked, onGameLongClick)
    }
}

@Composable
private fun DynamicHomeRows(
    rows: List<HomeDynamicRows.Row>,
    placement: HomeDynamicRows.Placement,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    rows.filter { it.placement == placement }.forEach { row ->
        key(row.key) {
            HomeRow(row.title, row.games, null, onGameClicked, onGameLongClick)
        }
    }
}

@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    emptyMessage: String?,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    // Rows without an empty message (recents, favorites) stay hidden until they have games.
    if (games.isEmpty() && emptyMessage == null) {
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        )
        if (games.isEmpty() && emptyMessage != null) {
            HomeRowEmptyMessage(emptyMessage)
        } else {
            HomeRowGames(games, onGameClicked, onGameLongClick)
        }
    }
}

@Composable
private fun HomeRowEmptyMessage(message: String) {
    // When an always-visible row is empty, a hint with the same height as the cards is shown instead.
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(HOME_ROW_PADDING)
                .height(HOME_ROW_EMPTY_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRowGames(
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HOME_ROW_SPACING),
        contentPadding = PaddingValues(HOME_ROW_PADDING),
    ) {
        items(games.size, key = { games[it].id }) { index ->
            val game = games[index]
            // All covers share the same height, so the card width follows the system box art ratio.
            val cardWidth = HOME_ROW_COVER_HEIGHT * CoverAspectRatio.forGame(game)
            LemuroidGameCard(
                modifier =
                    Modifier
                        .width(cardWidth)
                        .animateItem(),
                game = game,
                onClick = { onGameClicked(game) },
                onLongClick = { onGameLongClick(game) },
            )
        }
    }
}

private val HOME_ROW_PADDING = 16.dp
private val HOME_ROW_SPACING = 12.dp
private val HOME_ROW_COVER_HEIGHT = 112.dp

// Cover height plus the title and subtitle shown below it.
private val HOME_ROW_EMPTY_HEIGHT = HOME_ROW_COVER_HEIGHT + 52.dp

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                Text(stringResource(id = actionId))
            }
        }
    }
}
