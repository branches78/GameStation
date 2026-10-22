package com.swordfish.lemuroid.app.tv.shared

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.main.GameContextMenuContent
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.tv.coreoptions.TVCoreOptionsActivity
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Long press menu of a game on TV: the same actions and icons as on mobile, in a dialog centered on screen. */
object TVGameContextMenu {
    fun show(
        context: Context,
        game: Game,
        gameInteractor: GameInteractor,
    ) {
        val dialog = ComponentDialog(context)
        val content =
            ComposeView(context).apply {
                setContent {
                    AppTheme {
                        Surface(
                            modifier = Modifier.widthIn(max = DIALOG_MAX_WIDTH.dp),
                            shape = RoundedCornerShape(DIALOG_CORNER_RADIUS.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            GameContextMenuContent(
                                selectedGame = game,
                                onGamePlay = { gameInteractor.onGamePlay(it) },
                                onDismiss = { dialog.dismiss() },
                                onGameRestart = { gameInteractor.onGameRestart(it) },
                                onFavoriteToggle = { selected, isFavorite ->
                                    gameInteractor.onFavoriteToggle(selected, isFavorite)
                                },
                                shortcutSupported = gameInteractor.supportShortcuts(),
                                onCreateShortcut = { gameInteractor.onCreateShortcut(it) },
                                onSetCustomName = { selected, name -> gameInteractor.onSetCustomName(selected, name) },
                                onClearCustomName = { gameInteractor.onClearCustomName(it) },
                                onSetCustomCoverUri = { selected, uri ->
                                    gameInteractor.onSetCustomCoverUri(selected, uri)
                                },
                                onClearCustomCoverUri = { gameInteractor.onClearCustomCoverUri(it) },
                                onOpenCoreOptions = {
                                    context.startActivity(TVCoreOptionsActivity.newIntent(context, it.systemId))
                                },
                                requestInitialFocus = true,
                            )
                        }
                    }
                }
            }
        dialog.setContentView(content)
        // The rounded surface draws the background, the window itself stays transparent.
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private const val DIALOG_MAX_WIDTH = 460
    private const val DIALOG_CORNER_RADIUS = 20
}
