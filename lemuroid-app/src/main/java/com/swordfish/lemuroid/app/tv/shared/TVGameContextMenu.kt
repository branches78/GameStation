package com.swordfish.lemuroid.app.tv.shared

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.KeyEvent
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
        val dialog = LongPressSafeDialog(context)
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

    /**
     * The menu opens with a long press of the confirm button, which is still held down: its repeated presses and its
     * release would select the first entry. They are ignored until the button is pressed again inside the menu.
     */
    private class LongPressSafeDialog(context: Context) : ComponentDialog(context) {
        private var confirmPressedInDialog = false

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (!isConfirmKey(event.keyCode)) {
                return super.dispatchKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                confirmPressedInDialog = true
            }
            if (!confirmPressedInDialog) {
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        private fun isConfirmKey(keyCode: Int): Boolean {
            return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                keyCode == KeyEvent.KEYCODE_BUTTON_A
        }
    }

    private const val DIALOG_MAX_WIDTH = 460
    private const val DIALOG_CORNER_RADIUS = 20
}
