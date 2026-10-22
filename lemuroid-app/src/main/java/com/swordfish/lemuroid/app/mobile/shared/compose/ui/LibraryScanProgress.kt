package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.library.LibraryLoadingProgress
import com.swordfish.lemuroid.app.shared.library.LibraryScanProgressFormatter
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor

/** Progress of the directory scan, null when no scan is running. */
@Composable
fun rememberLibraryScanProgress(): State<LibraryLoadingProgress?> {
    val appContext = LocalContext.current.applicationContext
    val progressFlow = remember { PendingOperationsMonitor(appContext).directoryScanProgress() }
    return progressFlow.collectAsState(null)
}

/**
 * Bar with the percentage of the directory scan and "121 of 268 files" (then "Preparing cores: 2 of 5") below.
 * While the files are still being listed the bar moves without percentage and the text says "Looking for files…".
 */
@Composable
fun LibraryScanProgress(
    progress: LibraryLoadingProgress,
    modifier: Modifier = Modifier,
    barHeight: Dp = 6.dp,
    percentSize: TextUnit = MaterialTheme.typography.bodyMedium.fontSize,
) {
    val context = LocalContext.current
    val fraction = progress.fraction
    val barModifier = Modifier.height(barHeight)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (fraction == null) {
                LinearProgressIndicator(
                    modifier = barModifier.weight(1f),
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = barModifier.weight(1f),
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
            }
            Text(
                modifier = Modifier.widthIn(min = 40.dp),
                text = fraction?.let { LibraryScanProgressFormatter.percent(it) }.orEmpty(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = percentSize,
                textAlign = TextAlign.End,
            )
        }
        Text(
            text = LibraryScanProgressFormatter.details(context, progress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
