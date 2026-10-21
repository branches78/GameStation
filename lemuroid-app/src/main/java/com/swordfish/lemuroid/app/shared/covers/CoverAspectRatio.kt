package com.swordfish.lemuroid.app.shared.covers

import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * Width / height ratio of the box art for each system.
 * Values were measured on the images of the libretro thumbnails server (mostly US/EU releases).
 */
object CoverAspectRatio {
    private const val LANDSCAPE = 1.43f
    private const val HANDHELD_LANDSCAPE = 1.12f
    private const val PORTRAIT = 0.71f
    private const val NARROW_PORTRAIT = 0.58f
    private const val NEAR_SQUARE = 0.87f
    private const val SQUARE = 1.0f

    fun forGame(game: Game): Float {
        val systemId = SystemID.values().firstOrNull { it.dbname == game.systemId }
        return forSystem(systemId)
    }

    fun forSystem(systemId: SystemID?): Float {
        return when (systemId) {
            SystemID.SNES, SystemID.N64 -> LANDSCAPE

            SystemID.NDS,
            SystemID.NINTENDO_3DS,
            SystemID.LYNX,
            -> HANDHELD_LANDSCAPE

            SystemID.NES,
            SystemID.GENESIS,
            SystemID.SMS,
            SystemID.GG,
            SystemID.ATARI2600,
            SystemID.ATARI7800,
            SystemID.NGC,
            SystemID.WS,
            SystemID.WSC,
            SystemID.FBNEO,
            SystemID.MAME2003PLUS,
            SystemID.DOS,
            -> PORTRAIT

            SystemID.PSP -> NARROW_PORTRAIT

            SystemID.NGP -> NEAR_SQUARE

            SystemID.GB,
            SystemID.GBC,
            SystemID.GBA,
            SystemID.PSX,
            SystemID.SEGACD,
            SystemID.PC_ENGINE,
            null,
            -> SQUARE
        }
    }
}
