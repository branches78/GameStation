package com.swordfish.lemuroid.app.shared.coreoptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R

/**
 * Two tabs above the core options of a game: "Only this game" (on the left, selected at first) and
 * "All the games of <system>".
 */
@Composable
fun CoreOptionsScopeSelector(
    systemName: String,
    gameScope: Boolean,
    onGameScopeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(SELECTOR_CORNER_RADIUS.dp)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        ScopeTab(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.core_options_scope_game),
            selected = gameScope,
            onClick = { onGameScopeChange(true) },
        )
        ScopeTab(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.core_options_scope_system, systemName),
            selected = !gameScope,
            onClick = { onGameScopeChange(false) },
        )
    }
}

@Composable
private fun ScopeTab(
    modifier: Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(SELECTOR_CORNER_RADIUS.dp)
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val colors = MaterialTheme.colorScheme
    val textColor = if (selected) colors.onSecondaryContainer else colors.onSurface

    Box(
        modifier =
            modifier
                .clip(shape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                // On TV the focused tab gets a white border, like the other focused elements.
                .border(FOCUS_BORDER_WIDTH.dp, if (focused) Color.White else Color.Transparent, shape)
                .padding(1.dp)
                .clip(shape)
                .background(background)
                .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private const val SELECTOR_CORNER_RADIUS = 20
private const val FOCUS_BORDER_WIDTH = 3
