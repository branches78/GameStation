package com.swordfish.lemuroid.app.utils.games

import android.content.Context
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game

class GameUtils {
    companion object {
        fun getGameSubtitle(
            context: Context,
            game: Game,
        ): String {
            val systemName = getSystemNameForGame(context, game)
            val developerName =
                if (game.developer?.isNotBlank() == true) {
                    "- ${game.developer}"
                } else {
                    ""
                }
            return "$systemName $developerName"
        }

        /** Short name of the system of the game, e.g. "SNES". */
        fun getSystemNameForGame(
            context: Context,
            game: Game,
        ): String {
            val systemTitleResource = GameSystem.findById(game.systemId).shortTitleResId
            return context.getString(systemTitleResource)
        }
    }
}
