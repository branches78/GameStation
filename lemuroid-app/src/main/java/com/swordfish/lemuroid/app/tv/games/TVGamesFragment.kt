package com.swordfish.lemuroid.app.tv.games

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.tv.home.TVHomeFragment
import com.swordfish.lemuroid.app.tv.shared.GamePresenter
import com.swordfish.lemuroid.app.tv.shared.TVTabGrids
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/** Games of a system, shown inside the "Consoles" tab so the tabs bar stays on screen. */
class TVGamesFragment : VerticalGridSupportFragment() {
    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    @Inject
    lateinit var gameInteractor: GameInteractor

    private val metaSystemId: MetaSystemID by lazy {
        MetaSystemID.valueOf(requireArguments().getString(ARG_META_SYSTEM_ID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Without the focus dimmer the covers always keep their colors; the focused card is already zoomed.
        val gridPresenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_LARGE, false)
        gridPresenter.numberOfColumns = TVHomeFragment.cardsPerRow(resources)
        setGridPresenter(gridPresenter)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // The default leanback title is replaced by a centered title with the system name.
        showTitle(false)
        TVTabGrids.addTitle(view as ViewGroup, getString(metaSystemId.titleResId))
        placeGridBelowTitle(view)

        val factory = TVGamesViewModel.Factory(retrogradeDb)
        val gamesViewModel = ViewModelProvider(this, factory)[TVGamesViewModel::class.java]

        // The same square cards of the home rows and the favorites, 4 per row for every system.
        val cardSize = TVHomeFragment.homeCardSize(resources)
        val pagingAdapter =
            PagingDataAdapter(
                GamePresenter(cardSize, gameInteractor, fixedRatio = 1f),
                Game.DIFF_CALLBACK,
            )

        this.adapter = pagingAdapter

        launchOnState(Lifecycle.State.RESUMED) {
            gamesViewModel.games
                .collect { pagingAdapter.submitData(lifecycle, it) }
        }

        gamesViewModel.metaSystemId.value = metaSystemId

        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                when (item) {
                    is Game -> gameInteractor.onGamePlay(item)
                }
            }
    }

    /**
     * The grid has the width of the other tabs content, its first row starts below the title and pressing up on it
     * moves the focus to the tabs bar.
     */
    private fun placeGridBelowTitle(root: View) {
        val grid = root.findViewById<VerticalGridView>(androidx.leanback.R.id.browse_grid) ?: return
        val topPadding = resources.getDimensionPixelSize(R.dimen.tv_games_grid_top_padding)
        grid.setPadding(grid.paddingLeft, topPadding, grid.paddingRight, grid.paddingBottom)
        TVTabGrids.setUpHomeCardsGrid(grid)
        TVTabGrids.allowFocusUpToTabs(this, grid, TVHomeFragment.cardsPerRow(resources))
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    companion object {
        private const val ARG_META_SYSTEM_ID = "meta_system_id"

        fun newInstance(metaSystemId: String): TVGamesFragment {
            return TVGamesFragment().apply { arguments = bundleOf(ARG_META_SYSTEM_ID to metaSystemId) }
        }
    }

    @dagger.Module
    class Module
}
