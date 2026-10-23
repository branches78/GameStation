package com.swordfish.lemuroid.app.mobile.feature.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.swordfish.lemuroid.common.paging.buildFlowPaging
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GamesViewModel(
    private val retrogradeDb: RetrogradeDatabase,
    initialMetaSystem: MetaSystemID,
) : ViewModel() {
    class Factory(
        private val retrogradeDb: RetrogradeDatabase,
        private val initialMetaSystem: MetaSystemID,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GamesViewModel(retrogradeDb, initialMetaSystem) as T
        }
    }

    private val metaSystemId = MutableStateFlow(initialMetaSystem)

    @OptIn(ExperimentalCoroutinesApi::class)
    val games: Flow<PagingData<Game>> =
        metaSystemId
            .map { metaSystem -> metaSystem.systemIDs }
            .map { systemIds -> systemIds.map { it.dbname } }
            .flatMapLatest {
                when (it.size) {
                    0 -> emptyFlow<PagingData<Game>>()
                    1 -> buildFlowPaging(20, viewModelScope) { retrogradeDb.gameDao().selectBySystem(it.first()) }
                    else -> buildFlowPaging(20, viewModelScope) { retrogradeDb.gameDao().selectBySystems(it) }
                }
            }
            // Kept while the screen is in the back stack: back from the details of a game shows the list right away,
            // at the same position, instead of loading it again.
            .cachedIn(viewModelScope)
}
