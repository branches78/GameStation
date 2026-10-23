package com.swordfish.lemuroid.app.mobile.feature.details

import android.os.Build
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameImage
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.covers.GameScreenshots
import com.swordfish.lemuroid.app.shared.covers.TrimBlackBordersTransformation
import com.swordfish.lemuroid.app.shared.details.GameAction
import com.swordfish.lemuroid.app.shared.details.GameActionsHost
import com.swordfish.lemuroid.app.shared.details.GameDetailsViewModel
import com.swordfish.lemuroid.app.shared.details.gameCleanTitle
import com.swordfish.lemuroid.app.shared.details.gameRegion
import com.swordfish.lemuroid.app.shared.details.gameSystemName
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Details of a game on mobile, opened when a game is chosen (setting "Show game details"): screenshot on top, cover
 * and title, a full width play button, the actions of the long press menu as icon buttons, the description from
 * Wikipedia and other games of the same system.
 */
@Composable
fun GameDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: GameDetailsViewModel,
    gameInteractor: GameInteractor,
    onBack: () -> Unit,
    onOpenCoreOptions: (Game) -> Unit,
    onOpenGame: (Game) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val game = state.game ?: return

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Cover and title go over the bottom of the screenshot.
            Box {
                GameScreenshotHeader(game)
                GameTitleHeader(game, Modifier.padding(top = (HEADER_HEIGHT - COVER_OVERLAP).dp))
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GameActionsHost(game, gameInteractor, onOpenCoreOptions) { actions ->
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp),
                        onClick = actions.play.onClick,
                    ) {
                        Icon(imageVector = actions.play.icon, contentDescription = null)
                        Text(modifier = Modifier.padding(start = 8.dp), text = actions.play.label)
                    }
                    val secondary =
                        listOfNotNull(
                            actions.restart,
                            actions.favorite,
                            actions.coreOptions,
                            actions.customName,
                            actions.shortcut,
                            actions.customCover,
                        )
                    ActionsGrid(secondary)
                }

                state.description?.let { GameDescription(it) }
                GameFacts(game, state.coreName)

                if (state.moreGames.isNotEmpty()) {
                    Text(
                        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp),
                        text = stringResource(R.string.game_details_more_from, gameSystemName(game).orEmpty()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (state.moreGames.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(state.moreGames, key = { it.id }) { other ->
                        MoreGameCard(other) { onOpenGame(other) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Over the screenshot, as the screen has no top bar.
        IconButton(
            modifier =
                Modifier
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            onClick = onBack,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}

/** Screenshot of the game, then its title screen, then its cover blurred; faded into the background at the bottom. */
@Composable
private fun GameScreenshotHeader(game: Game) {
    val urls = remember(game.id) { GameScreenshots.urls(game) }
    var urlIndex by remember(game.id) { mutableIntStateOf(0) }
    val screenshotUrl = urls.getOrNull(urlIndex)
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxWidth().height(HEADER_HEIGHT.dp)) {
        if (screenshotUrl != null) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(screenshotUrl)
                        .transformations(TrimBlackBordersTransformation())
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                // Screenshots are small pixel art images: sharper without filtering.
                filterQuality = FilterQuality.None,
                onError = { urlIndex++ },
            )
        } else {
            val blurred = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(COVER_BLUR.dp) else Modifier
            AsyncImage(
                modifier = Modifier.fillMaxSize().then(blurred),
                model = game.displayCoverUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0.55f),
                            0.45f to background.copy(alpha = 0.2f),
                            1f to background,
                        ),
                    ),
        )
    }
}

@Composable
private fun GameTitleHeader(
    game: Game,
    modifier: Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier =
                Modifier
                    .size(COVER_SIZE.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            LemuroidGameImage(game = game, fixedRatio = 1f)
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = gameCleanTitle(game),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta =
                listOfNotNull(gameSystemName(game), game.developer?.takeIf { it.isNotBlank() }, gameRegion(game))
                    .joinToString(" · ")
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Secondary actions in centered rows with the icons aligned at the top. Four actions (a game never played) go in two
 * rows of two; otherwise in rows of three across the whole width.
 */
@Composable
private fun ActionsGrid(actions: List<GameAction>) {
    val perRow = if (actions.size == 4) 2 else ACTIONS_PER_ROW
    Column(modifier = Modifier.padding(top = 12.dp)) {
        actions.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Top,
            ) {
                row.forEach { action ->
                    val cellModifier = if (perRow == 2) Modifier.width(TWO_COLUMNS_CELL_WIDTH.dp) else Modifier.weight(1f)
                    ActionButton(action, cellModifier)
                }
                if (perRow == ACTIONS_PER_ROW) {
                    repeat(perRow - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: GameAction,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = action.onClick)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = action.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun GameDescription(description: String) {
    var expanded by remember(description) { mutableStateOf(false) }
    var canExpand by remember(description) { mutableStateOf(false) }

    Text(
        modifier = Modifier.padding(top = 12.dp),
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_LINES,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { if (!expanded) canExpand = it.hasVisualOverflow },
    )
    if (canExpand || expanded) {
        Text(
            modifier =
                Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
            text = stringResource(if (expanded) R.string.game_details_read_less else R.string.game_details_read_more),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Text(
        modifier = Modifier.padding(top = 4.dp),
        text = stringResource(R.string.game_details_description_source),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun GameFacts(
    game: Game,
    coreName: String?,
) {
    val facts =
        listOfNotNull(
            game.lastPlayedAt?.let {
                stringResource(R.string.game_details_last_played) to
                    DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                        .toString()
            },
            coreName?.let { stringResource(R.string.game_details_core) to it },
        )
    if (facts.isEmpty()) return
    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        facts.forEach { (label, value) ->
            Row {
                Text(
                    modifier = Modifier.width(110.dp),
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MoreGameCard(
    game: Game,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.width(MORE_CARD_SIZE.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)) {
        Box(
            modifier =
                Modifier
                    .size(MORE_CARD_SIZE.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            LemuroidGameImage(game = game, fixedRatio = 1f)
        }
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = gameCleanTitle(game),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val HEADER_HEIGHT = 220
private const val COVER_SIZE = 96
private const val COVER_OVERLAP = 64
private const val COVER_BLUR = 16
private const val ACTIONS_PER_ROW = 3
private const val TWO_COLUMNS_CELL_WIDTH = 140
private const val DESCRIPTION_LINES = 4
private const val MORE_CARD_SIZE = 104
