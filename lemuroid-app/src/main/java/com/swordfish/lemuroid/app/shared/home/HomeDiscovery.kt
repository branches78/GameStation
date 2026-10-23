package com.swordfish.lemuroid.app.shared.home

import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.Calendar

/**
 * Games of the "Discover" row, shared by mobile and TV: games never played, from different systems (one of each in
 * turn), in an order that changes every day and every time a game is played. When there are not enough of them,
 * the games played longest ago fill the row.
 */
object HomeDiscovery {
    // Games read to choose from: enough to mix several systems.
    private const val POOL_SIZE = 300

    // Largest seed of the shuffled query, which orders by (id * seed) % 7919.
    private const val SEED_RANGE = 7918

    @OptIn(ExperimentalCoroutinesApi::class)
    fun games(
        retrogradeDb: RetrogradeDatabase,
        count: Int,
    ): Flow<List<Game>> {
        val dao = retrogradeDb.gameDao()
        return dao.selectLastPlayed()
            .map { lastPlayed -> seed(lastPlayed) }
            .distinctUntilChanged()
            .flatMapLatest { seed ->
                combine(
                    dao.selectNotPlayedShuffled(seed, POOL_SIZE),
                    dao.selectLeastRecentlyPlayed(count),
                ) { notPlayed, playedLongAgo ->
                    val discovered = mixSystems(notPlayed, count)
                    discovered + playedLongAgo.take(count - discovered.size)
                }
            }
    }

    /** Changes with the day and with the last game played, so the row changes while playing. */
    private fun seed(lastPlayed: Game?): Int {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastPlayedMinute = (lastPlayed?.lastPlayedAt ?: 0L) / 60_000
        val value = day * 7_919L + (lastPlayed?.id ?: 0) * 31L + lastPlayedMinute
        return (Math.floorMod(value, SEED_RANGE.toLong()) + 1).toInt()
    }

    /** Takes one game of each system in turn, keeping the shuffled order inside each system. */
    private fun mixSystems(
        games: List<Game>,
        count: Int,
    ): List<Game> {
        val bySystem = games.groupBy { it.systemId }.values.map { it.iterator() }
        val result = mutableListOf<Game>()
        while (result.size < count && bySystem.any { it.hasNext() }) {
            bySystem.forEach { iterator ->
                if (result.size < count && iterator.hasNext()) result += iterator.next()
            }
        }
        return result
    }
}
