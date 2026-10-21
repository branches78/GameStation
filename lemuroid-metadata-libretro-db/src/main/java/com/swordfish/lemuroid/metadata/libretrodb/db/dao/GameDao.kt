package com.swordfish.lemuroid.metadata.libretrodb.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.swordfish.lemuroid.metadata.libretrodb.db.entity.LibretroRom

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE romName LIKE :romName")
    suspend fun findByName(romName: String): List<LibretroRom>?

    @Query("SELECT * FROM games WHERE romName = :romName LIMIT 1")
    suspend fun findByFileName(romName: String): LibretroRom?

    @Query("SELECT * FROM games WHERE crc32 = :crc LIMIT 1")
    suspend fun findByCRC(crc: String): LibretroRom?

    @Query("SELECT * FROM games WHERE serial = :serial LIMIT 1")
    suspend fun findBySerial(serial: String): LibretroRom?

    @Query("SELECT * FROM games WHERE romName LIKE :romNamePattern AND system = :system")
    suspend fun findByRomNameLikeAndSystem(
        romNamePattern: String,
        system: String,
    ): List<LibretroRom>

    // Not indexed: only used to fix games already in the library.
    @Query("SELECT * FROM games WHERE name = :name AND system = :system")
    suspend fun findByNameAndSystem(
        name: String,
        system: String,
    ): List<LibretroRom>
}
