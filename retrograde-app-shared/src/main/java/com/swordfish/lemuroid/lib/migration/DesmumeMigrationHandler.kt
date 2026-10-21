package com.swordfish.lemuroid.lib.migration

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber
import java.io.File

/**
 * DeSmuME has been removed. This only remains to let melonDS load saves made with DeSmuME (.dsv files).
 */
class DesmumeMigrationHandler(
    private val directoriesManager: DirectoriesManager,
) {
    fun resolveSaveData(
        game: Game,
        coreID: CoreID,
        defaultData: ByteArray?,
    ): SaveDataResult {
        if (coreID != CoreID.MELONDS) {
            return SaveDataResult(defaultData, null)
        }

        val savesDirectory = directoriesManager.getSavesDirectory()
        val baseFileName = game.fileName.substringBeforeLast(".", game.fileName)
        val srmFile = File(savesDirectory, "$baseFileName.$SRM_EXTENSION")
        val dsvFile = File(savesDirectory, "$baseFileName.$DSV_EXTENSION")
        val srmInfo = SaveCandidate(srmFile, defaultData ?: srmFile.readBytesIfValid())
        val dsvInfo = SaveCandidate(dsvFile, dsvFile.readBytesIfValid())

        return selectRawSave(baseFileName, srmInfo, dsvInfo)
    }

    data class SaveDataResult(val data: ByteArray?, val timestampOverride: Long?)

    private data class SaveCandidate(
        val file: File,
        val data: ByteArray?,
    ) {
        val isValid: Boolean = file.exists() && file.length() > 0 && data != null
        val timestamp: Long = if (isValid) file.lastModified() else 0
    }

    private fun selectRawSave(
        baseFileName: String,
        srmInfo: SaveCandidate,
        dsvInfo: SaveCandidate,
    ): SaveDataResult {
        if (dsvInfo.timestamp > srmInfo.timestamp && dsvInfo.data != null) {
            Timber.i("Using newer DSV save for %s when launching melonDS", baseFileName)
            return SaveDataResult(convertDsvToRaw(dsvInfo.data), dsvInfo.timestamp)
        }

        if (srmInfo.data != null) {
            Timber.d("Using SRM save for %s when launching melonDS", baseFileName)
            return SaveDataResult(srmInfo.data, srmInfo.timestamp)
        }

        if (dsvInfo.data != null) {
            Timber.i("SRM missing for %s. Converting DSV to raw for melonDS", baseFileName)
            return SaveDataResult(convertDsvToRaw(dsvInfo.data), dsvInfo.timestamp)
        }

        Timber.d("No save available for %s when launching melonDS", baseFileName)
        return SaveDataResult(null, null)
    }

    private fun File.readBytesSafely(): ByteArray? {
        return runCatching { readBytes() }
            .getOrElse {
                Timber.w(it, "Unable to read save file %s", absolutePath)
                null
            }
    }

    private fun File.readBytesIfValid(): ByteArray? {
        return if (exists() && length() > 0) {
            readBytesSafely()
        } else {
            null
        }
    }

    private fun convertDsvToRaw(data: ByteArray): ByteArray {
        val footerIndex = data.indexOfSubArray(DESMUME_FOOTER_PREFIX)
        return if (footerIndex >= 0) {
            data.copyOf(footerIndex)
        } else {
            data
        }
    }

    private fun ByteArray.indexOfSubArray(pattern: ByteArray): Int {
        if (pattern.isEmpty() || this.size < pattern.size) return -1
        outer@ for (i in 0..this.size - pattern.size) {
            for (j in pattern.indices) {
                if (this[i + j] != pattern[j]) continue@outer
            }
            return i
        }
        return -1
    }

    companion object {
        private const val SRM_EXTENSION = "srm"
        private const val DSV_EXTENSION = "dsv"
        private val DESMUME_FOOTER_PREFIX =
            "|<--Snip above here to create a raw sav by excluding this DeSmuME savedata footer:"
                .toByteArray(Charsets.US_ASCII)
    }
}
