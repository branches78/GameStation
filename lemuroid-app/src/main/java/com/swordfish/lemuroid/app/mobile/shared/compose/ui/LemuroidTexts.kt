package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.games.GameUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Game name on up to two lines and its system below (the developer is left out to keep it short).
 * Cards keep the space of two title lines, so all of them have the same height.
 */
@Composable
fun LemuroidGameTexts(
    modifier: Modifier = Modifier,
    game: Game,
    reserveTitleLines: Boolean = false,
) {
    val context = LocalContext.current
    val subtitle =
        remember(game.id) {
            GameUtils.getSystemNameForGame(context, game)
        }

    LemuroidTexts(
        modifier = modifier,
        title = game.displayName(),
        subtitle = subtitle,
        titleMaxLines = GAME_TITLE_MAX_LINES,
        titleMinLines = if (reserveTitleLines) GAME_TITLE_MAX_LINES else 1,
    )
}

private const val GAME_TITLE_MAX_LINES = 2

@Composable
fun LemuroidTexts(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    titleMaxLines: Int = 1,
    titleMinLines: Int = 1,
) {
    Column(
        modifier = modifier.padding(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            minLines = titleMinLines,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
