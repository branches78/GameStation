package com.swordfish.lemuroid.metadata.libretrodb

import com.swordfish.lemuroid.common.kotlin.filterNullable
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.metadata.libretrodb.db.LibretroDBManager
import com.swordfish.lemuroid.metadata.libretrodb.db.LibretroDatabase
import com.swordfish.lemuroid.metadata.libretrodb.db.entity.LibretroRom
import timber.log.Timber
import java.util.Locale

class LibretroDBMetadataProvider(private val ovgdbManager: LibretroDBManager) :
    GameMetadataProvider {
    companion object {
        private val THUMB_REPLACE = Regex("[&*/:`<>?\\\\|]")
        private val BRACKET_GROUPS = Regex("\\[[^\\]]*\\]")
        private val PARENTHESIS_GROUPS = Regex("\\s*\\([^)]*\\)")
        private val UNKNOWN_PUBLISHER = Regex("\\s*\\(-\\)")

        // Versions that should never be picked when matching a game only by its title.
        private val UNWANTED_VERSIONS = Regex("\\((Beta|Proto|Demo|Sample|Pirate|Unl|Hack)[^)]*\\)", RegexOption.IGNORE_CASE)

        // Preferred regions when several versions share the same title (lower is better).
        private val REGION_PRIORITY = listOf("USA", "World", "Europe", "Japan")
    }

    private val sortedSystemIds: List<String> by lazy {
        SystemID.values()
            .map { it.dbname }
            .sortedByDescending { it.length }
    }

    override suspend fun retrieveMetadata(storageFile: StorageFile): GameMetadata? {
        val db = ovgdbManager.dbInstance

        Timber.d("Looking metadata for file: $storageFile")

        val metadata =
            runCatching {
                findByCRC(storageFile, db)
                    ?: findBySerial(storageFile, db)
                    ?: findByFilename(db, storageFile)
                    ?: findByPathAndFilename(db, storageFile)
                    ?: findByTitle(db, storageFile)
                    ?: findByUniqueExtension(storageFile)
                    ?: findByKnownSystem(storageFile)
                    ?: findByPathAndSupportedExtension(storageFile)
            }.getOrElse {
                Timber.e("Error in retrieving $storageFile metadata: $it... Skipping.")
                null
            }

        metadata?.let { Timber.d("Metadata retrieved for item: $it") }

        return metadata
    }

    private suspend fun convertToGameMetadata(
        db: LibretroDatabase,
        rom: LibretroRom,
    ): GameMetadata {
        if (!isTosec(rom)) {
            return buildMetadata(rom)
        }
        return resolveTosec(db, rom)
    }

    private fun buildMetadata(rom: LibretroRom): GameMetadata {
        val system = GameSystem.findById(rom.system!!)
        return GameMetadata(
            name = rom.name,
            romName = rom.romName,
            thumbnail = computeCoverUrl(system, rom.name),
            system = rom.system,
            developer = rom.developer,
        )
    }

    /**
     * TOSEC entries are mapped to their No-Intro equivalent when there is one (No-Intro names are the common ones).
     * Otherwise the name is cleaned up and the cover uses the TOSEC file name, which is how the thumbnails
     * server stores TOSEC covers (e.g. "Balloon Fight (1985-01-22)(Nintendo)(JP).png").
     */
    private suspend fun resolveTosec(
        db: LibretroDatabase,
        rom: LibretroRom,
    ): GameMetadata {
        val equivalent = findNoIntroEquivalent(db, rom)
        if (equivalent != null) {
            return buildMetadata(equivalent).copy(developer = equivalent.developer ?: rom.developer)
        }

        val system = GameSystem.findById(rom.system!!)
        return buildMetadata(rom).copy(
            name = rom.name?.let { cleanTosecName(it) },
            thumbnail = computeCoverUrl(system, rom.romName?.substringBeforeLast(".")),
        )
    }

    override suspend fun refreshMetadata(game: Game): Game? {
        val db = ovgdbManager.dbInstance

        // Games that were not recognized before (no cover) get a second chance by title.
        if (game.coverFrontUrl == null) {
            val extension = game.fileName.substringAfterLast(".", "")
            val match = findBestTitleMatch(db, game.fileName.substringBeforeLast("."), game.systemId, extension)
            return match?.let {
                val metadata = buildMetadata(it)
                game.copy(
                    title = metadata.name ?: game.title,
                    coverFrontUrl = metadata.thumbnail,
                    developer = metadata.developer ?: game.developer,
                )
            }
        }

        // TOSEC names have at least two groups, e.g. "Arkanoid (Taito Software) (USA)".
        if (PARENTHESIS_GROUPS.findAll(game.title).count() < 2) return null

        val roms = db.gameDao().findByNameAndSystem(game.title, game.systemId)
        val tosecRom = roms.firstOrNull { isTosec(it) } ?: return null
        if (roms.any { !isTosec(it) }) return null

        val metadata = resolveTosec(db, tosecRom)
        return game.copy(
            title = metadata.name ?: game.title,
            coverFrontUrl = metadata.thumbnail,
            developer = metadata.developer ?: game.developer,
        )
    }

    // TOSEC rom names look like "Arkanoid (1987-08)(Taito Software)(US).nes".
    private fun isTosec(rom: LibretroRom): Boolean {
        return rom.romName?.contains(")(") == true
    }

    private suspend fun findNoIntroEquivalent(
        db: LibretroDatabase,
        rom: LibretroRom,
    ): LibretroRom? {
        val name = rom.name ?: return null
        val system = rom.system ?: return null
        val extension = rom.romName?.substringAfterLast(".", "")?.takeIf { it.isNotEmpty() } ?: return null
        val candidates = noIntroNameCandidates(name)

        // Exact names first, e.g. "Arkanoid (USA)".
        for (candidate in candidates) {
            val match = db.gameDao().findByFileName("$candidate.$extension")
            if (match != null && match.system == system && !isTosec(match)) {
                return match
            }
        }

        // Then names with extra groups, e.g. "Balloon Fight (Japan)" -> "Balloon Fight (Japan) (En)".
        for (candidate in candidates) {
            val match =
                db.gameDao().findByRomNameLikeAndSystem("$candidate (%.$extension", system)
                    .filter { !isTosec(it) && it.romName?.startsWith("$candidate (") == true }
                    .minByOrNull { it.name?.length ?: Int.MAX_VALUE }
            if (match != null) {
                return match
            }
        }

        return null
    }

    /** Names to try: without [flags], and then also without each (group), e.g. the publisher. */
    private fun noIntroNameCandidates(name: String): List<String> {
        val base = name.replace(BRACKET_GROUPS, "").trim()
        val withoutEachGroup =
            PARENTHESIS_GROUPS.findAll(base)
                .map { base.replaceFirst(it.value, "").trim() }
                .toList()
        return (listOf(base) + withoutEachGroup).distinct()
    }

    /**
     * Last chance before giving up: match by title only, so files named just "Circus Charlie.nes"
     * (or with a different region/tags) are still recognized. Only systems with a unique extension are used,
     * otherwise the system would be ambiguous.
     */
    private suspend fun findByTitle(
        db: LibretroDatabase,
        file: StorageFile,
    ): GameMetadata? {
        val system = GameSystem.findByUniqueFileExtension(file.extension) ?: return null
        if (!system.scanOptions.scanByFilename) return null

        return findBestTitleMatch(db, file.extensionlessName, system.id.dbname, file.extension)
            ?.let { buildMetadata(it) }
    }

    private suspend fun findBestTitleMatch(
        db: LibretroDatabase,
        fileName: String,
        system: String,
        extension: String,
    ): LibretroRom? {
        val title = fileName.replace(BRACKET_GROUPS, "").replace(PARENTHESIS_GROUPS, "").trim()
        if (title.isEmpty() || extension.isEmpty()) return null

        // No-Intro moves leading articles to the end: "The Legend of Zelda" -> "Legend of Zelda, The".
        val titles = listOfNotNull(title, title.removePrefix("The ").takeIf { it != title }?.let { "$it, The" })

        for (candidate in titles) {
            // Titles are compared ignoring case and punctuation: "Super Mario Bros" == "Super Mario Bros.".
            val normalizedCandidate = normalizeTitle(candidate)
            val matches =
                db.gameDao().findByRomNameLikeAndSystem("$candidate%.$extension", system)
                    .filter { !isTosec(it) && normalizeTitle(it.name?.substringBefore(" (") ?: "") == normalizedCandidate }
                    .filterNot { UNWANTED_VERSIONS.containsMatchIn(it.name ?: "") }

            val best = matches.minWithOrNull(compareBy({ regionPriority(it.name) }, { it.name?.length ?: Int.MAX_VALUE }))
            if (best != null) {
                return best
            }
        }
        return null
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    }

    private fun regionPriority(name: String?): Int {
        val regions = name?.let { PARENTHESIS_GROUPS.find(it)?.value } ?: return REGION_PRIORITY.size
        val index = REGION_PRIORITY.indexOfFirst { regions.contains(it) }
        return if (index >= 0) index else REGION_PRIORITY.size
    }

    /** "Balloon Fight (Nintendo) (Japan)[h]" -> "Balloon Fight (Japan)": drops the [flags] and the publisher. */
    private fun cleanTosecName(name: String): String {
        val base = name.replace(BRACKET_GROUPS, "").trim()
        val groups = PARENTHESIS_GROUPS.findAll(base).toList()
        val withoutPublisher = if (groups.size >= 2) base.replaceFirst(groups.first().value, "") else base
        return withoutPublisher.replace(UNKNOWN_PUBLISHER, "").trim()
    }

    private suspend fun findByFilename(
        db: LibretroDatabase,
        file: StorageFile,
    ): GameMetadata? {
        return db.gameDao().findByFileName(file.name)
            .filterNullable { extractGameSystem(it).scanOptions.scanByFilename }
            ?.let { convertToGameMetadata(db, it) }
    }

    private suspend fun findByPathAndFilename(
        db: LibretroDatabase,
        file: StorageFile,
    ): GameMetadata? {
        return db.gameDao().findByFileName(file.name)
            .filterNullable { extractGameSystem(it).scanOptions.scanByPathAndFilename }
            .filterNullable { parentContainsSystem(file.path, extractGameSystem(it).id.dbname) }
            ?.let { convertToGameMetadata(db, it) }
    }

    private fun findByPathAndSupportedExtension(file: StorageFile): GameMetadata? {
        val system =
            sortedSystemIds
                .filter { parentContainsSystem(file.path, it) }
                .map { GameSystem.findById(it) }
                .filter { it.scanOptions.scanByPathAndSupportedExtensions }
                .firstOrNull { it.supportedExtensions.contains(file.extension) }

        return system?.let {
            GameMetadata(
                name = file.extensionlessName,
                romName = file.name,
                thumbnail = null,
                system = it.id.dbname,
                developer = null,
            )
        }
    }

    private fun parentContainsSystem(
        parent: String?,
        dbname: String,
    ): Boolean {
        return parent?.toLowerCase(Locale.getDefault())?.contains(dbname) == true
    }

    private suspend fun findByCRC(
        file: StorageFile,
        db: LibretroDatabase,
    ): GameMetadata? {
        if (file.crc == null || file.crc == "0") return null
        return file.crc?.let { crc32 -> db.gameDao().findByCRC(crc32) }
            ?.let { convertToGameMetadata(db, it) }
    }

    private suspend fun findBySerial(
        file: StorageFile,
        db: LibretroDatabase,
    ): GameMetadata? {
        if (file.serial == null) return null
        return db.gameDao().findBySerial(file.serial!!)
            ?.let { convertToGameMetadata(db, it) }
    }

    private fun findByKnownSystem(file: StorageFile): GameMetadata? {
        if (file.systemID == null) return null

        return GameMetadata(
            name = file.extensionlessName,
            romName = file.name,
            thumbnail = null,
            system = file.systemID!!.dbname,
            developer = null,
        )
    }

    private fun findByUniqueExtension(file: StorageFile): GameMetadata? {
        val system = GameSystem.findByUniqueFileExtension(file.extension)

        if (system?.scanOptions?.scanByUniqueExtension == false) {
            return null
        }

        val result =
            system?.let {
                GameMetadata(
                    name = file.extensionlessName,
                    romName = file.name,
                    thumbnail = null,
                    system = it.id.dbname,
                    developer = null,
                )
            }

        return result
    }

    private fun extractGameSystem(rom: LibretroRom): GameSystem {
        return GameSystem.findById(rom.system!!)
    }

    private fun computeCoverUrl(
        system: GameSystem,
        name: String?,
    ): String? {
        var systemName = system.libretroFullName

        // Specific mame version don't have any thumbnails in Libretro database
        if (system.id == SystemID.MAME2003PLUS) {
            systemName = "MAME"
        }

        if (name == null) {
            return null
        }

        val imageType = "Named_Boxarts"

        val thumbGameName = name.replace(THUMB_REPLACE, "_")

        return "http://thumbnails.libretro.com/$systemName/$imageType/$thumbGameName.png"
    }
}
