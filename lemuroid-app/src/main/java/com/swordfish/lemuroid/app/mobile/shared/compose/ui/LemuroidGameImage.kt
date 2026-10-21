package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.app.shared.covers.CoverAspectRatio
import com.swordfish.lemuroid.app.shared.covers.CoverRatioCache
import com.swordfish.lemuroid.app.shared.covers.CoverUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun LemuroidGameImage(
    modifier: Modifier = Modifier,
    game: Game,
    // When false the card keeps the shape of its system (grids) and other covers are shown whole inside it.
    matchCoverShape: Boolean = true,
) {
    val fallbackDrawable =
        remember(game) {
            CoverUtils.getFallbackDrawable(game)
        }

    val fallbackPainter = rememberDrawablePainter(drawable = fallbackDrawable)

    val coverUrl = game.displayCoverUrl()
    val coverRatio = CoverRatioCache.get(coverUrl)
    val cardRatio = if (matchCoverShape) CoverAspectRatio.forGame(game) else CoverAspectRatio.forGameSystem(game)
    val contentScale =
        if (CoverRatioCache.needsLetterbox(coverRatio, cardRatio)) ContentScale.Fit else ContentScale.Crop

    AsyncImage(
        model =
            ImageRequest.Builder(LocalContext.current)
                .data(coverUrl)
                .build(),
        contentDescription = game.displayName(),
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(cardRatio),
        fallback = fallbackPainter,
        error = fallbackPainter,
        contentScale = contentScale,
        onSuccess = { CoverRatioCache.store(coverUrl, it.result.drawable) },
    )
}
