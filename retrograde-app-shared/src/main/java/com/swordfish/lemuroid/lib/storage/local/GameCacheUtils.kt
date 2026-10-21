package com.swordfish.lemuroid.lib.storage.local

import android.content.Context
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File

object GameCacheUtils {
    fun getDataFileForGame(
        folderName: String,
        context: Context,
        game: Game,
        dataFile: DataFile,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return safeChildFile(gamesCacheDir, dataFile.fileName)
    }

    fun getCacheFileForGame(
        folderName: String,
        context: Context,
        game: Game,
    ): File {
        val gamesCacheDir = getCacheDirForGame(folderName, game, context)
        return safeChildFile(gamesCacheDir, game.fileName)
    }

    private fun getCacheDirForGame(
        folderName: String,
        game: Game,
        context: Context,
    ): File {
        val gamesCachePath = buildPath(folderName, game.systemId)
        val gamesCacheDir = File(context.cacheDir, gamesCachePath)
        gamesCacheDir.mkdirs()
        return gamesCacheDir
    }

    /**
     * File names may come from entries inside zip files, which can contain paths like "../../file" ("Zip Slip").
     * Only the last name is kept and the result must stay inside [directory].
     */
    private fun safeChildFile(
        directory: File,
        fileName: String,
    ): File {
        val file = File(directory, File(fileName).name)
        if (!file.canonicalPath.startsWith(directory.canonicalPath + File.separator)) {
            throw SecurityException("Invalid file name: $fileName")
        }
        return file
    }

    private fun buildPath(vararg chunks: String): String {
        return chunks.joinToString(separator = File.separator)
    }
}
