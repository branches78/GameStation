package com.swordfish.lemuroid.app.shared.covers

import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Screenshots of a game on the libretro thumbnails server, next to its cover. */
object GameScreenshots {
    private const val BOX_ARTS_FOLDER = "/Named_Boxarts/"
    private val IMAGE_FOLDERS = listOf("/Named_Snaps/", "/Named_Titles/")

    /** In-game screenshot first, then the title screen. Empty for games without a cover from the server. */
    fun urls(game: Game): List<String> {
        val coverUrl = game.coverFrontUrl ?: return emptyList()
        if (!coverUrl.contains(BOX_ARTS_FOLDER)) return emptyList()
        return IMAGE_FOLDERS.map { coverUrl.replace(BOX_ARTS_FOLDER, it) }
    }
}
