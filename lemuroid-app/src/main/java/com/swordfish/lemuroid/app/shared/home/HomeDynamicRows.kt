package com.swordfish.lemuroid.app.shared.home

import android.content.Context
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Builds the dynamic rows of the home screen, shared by mobile and TV.
 * Rows without games are not emitted, so they stay hidden.
 */
class HomeDynamicRows(
    private val appContext: Context,
    private val retrogradeDb: RetrogradeDatabase,
) {
    enum class Placement { BEFORE_DISCOVER, AFTER_DISCOVER }

    data class Row(
        val key: String,
        val title: String,
        val games: List<Game>,
        val placement: Placement,
    )

    fun getRows(): Flow<List<Row>> {
        val rows =
            listOf(
                becauseYouPlayedRow(),
                recentlyAddedRow(),
                playAgainRow(),
                systemClassicsRow(),
                moreFromDeveloperRow(),
            )
        return combine(rows) { it.filterNotNull().filter { row -> row.games.isNotEmpty() } }
            .distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun becauseYouPlayedRow(): Flow<Row?> {
        val dao = retrogradeDb.gameDao()
        return dao.selectLastPlayed()
            .distinctUntilChanged { old, new -> old?.id == new?.id && old?.displayName() == new?.displayName() }
            .flatMapLatest { lastPlayed ->
                if (lastPlayed == null) return@flatMapLatest flowOf(null)
                dao.selectSimilarNotPlayed(lastPlayed.id, lastPlayed.developer, lastPlayed.systemId, MAX_ITEMS)
                    .map { games ->
                        Row(
                            KEY_BECAUSE_YOU_PLAYED,
                            appContext.getString(R.string.home_row_because_you_played, lastPlayed.displayName()),
                            games,
                            Placement.BEFORE_DISCOVER,
                        )
                    }
            }
    }

    private fun recentlyAddedRow(): Flow<Row?> {
        return retrogradeDb.gameDao().selectRecentlyAdded(MAX_ITEMS)
            .map { games ->
                Row(
                    KEY_RECENTLY_ADDED,
                    appContext.getString(R.string.home_row_recently_added),
                    games,
                    Placement.BEFORE_DISCOVER,
                )
            }
    }

    private fun playAgainRow(): Flow<Row?> {
        val playedBefore = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(PLAY_AGAIN_DAYS)
        return retrogradeDb.gameDao().selectPlayedBefore(playedBefore, MAX_ITEMS)
            .map { games ->
                Row(
                    KEY_PLAY_AGAIN,
                    appContext.getString(R.string.home_row_play_again),
                    games,
                    Placement.BEFORE_DISCOVER,
                )
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun systemClassicsRow(): Flow<Row?> {
        val dao = retrogradeDb.gameDao()
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        return dao.selectSystemsWithCount()
            .map { systems ->
                // A different system every day, among the ones with enough games.
                val candidates =
                    systems
                        .filter { it.count >= MIN_SYSTEM_GAMES }
                        .map { it.systemId }
                        .sorted()
                candidates.getOrNull(day % candidates.size.coerceAtLeast(1))
            }
            .distinctUntilChanged()
            .flatMapLatest { systemId ->
                if (systemId == null) return@flatMapLatest flowOf(null)
                val system = GameSystem.findById(systemId)
                dao.selectBySystemShuffled(systemId, day + 1, MAX_ITEMS)
                    .map { games ->
                        Row(
                            KEY_SYSTEM_CLASSICS,
                            appContext.getString(R.string.home_row_system_classics, appContext.getString(system.titleResId)),
                            games,
                            Placement.AFTER_DISCOVER,
                        )
                    }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun moreFromDeveloperRow(): Flow<Row?> {
        val dao = retrogradeDb.gameDao()
        return dao.selectTopDeveloper()
            .map { it?.takeIf { developer -> developer.count >= MIN_DEVELOPER_GAMES }?.developer }
            .distinctUntilChanged()
            .flatMapLatest { developer ->
                if (developer == null) return@flatMapLatest flowOf(null)
                dao.selectByDeveloper(developer, MAX_ITEMS)
                    .map { games ->
                        Row(
                            KEY_MORE_FROM_DEVELOPER,
                            appContext.getString(R.string.home_row_more_from_developer, developer),
                            games,
                            Placement.AFTER_DISCOVER,
                        )
                    }
            }
    }

    companion object {
        private const val MAX_ITEMS = 10
        private const val PLAY_AGAIN_DAYS = 30L
        private const val MIN_SYSTEM_GAMES = 3
        private const val MIN_DEVELOPER_GAMES = 3

        private const val KEY_BECAUSE_YOU_PLAYED = "because_you_played"
        private const val KEY_RECENTLY_ADDED = "recently_added"
        private const val KEY_PLAY_AGAIN = "play_again"
        private const val KEY_SYSTEM_CLASSICS = "system_classics"
        private const val KEY_MORE_FROM_DEVELOPER = "more_from_developer"
    }
}
