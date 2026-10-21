package com.swordfish.lemuroid.app.tv.games

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.BaseGridView
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.covers.CoverAspectRatio
import com.swordfish.lemuroid.app.tv.shared.GamePresenter
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class TVGamesFragment : VerticalGridSupportFragment() {
    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    @Inject
    lateinit var gameInteractor: GameInteractor

    private val args: TVGamesFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = computeNumberOfColumns()
        setGridPresenter(gridPresenter)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // The default leanback title is replaced by a centered title with the system name.
        showTitle(false)
        addSystemTitle(view as ViewGroup)
        alignGridToTopStart(view)

        val factory = TVGamesViewModel.Factory(retrogradeDb)
        val gamesViewModel = ViewModelProvider(this, factory)[TVGamesViewModel::class.java]

        val cardSize = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.card_size)
        val pagingAdapter =
            PagingDataAdapter(
                // Grid cells share the system cover shape so the rows stay aligned.
                GamePresenter(cardSize, gameInteractor, matchCoverShape = false),
                Game.DIFF_CALLBACK,
            )

        this.adapter = pagingAdapter

        launchOnState(Lifecycle.State.RESUMED) {
            gamesViewModel.games
                .collect { pagingAdapter.submitData(lifecycle, it) }
        }

        args.metaSystemId.let {
            gamesViewModel.metaSystemId.value = MetaSystemID.valueOf(it)
        }

        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                when (item) {
                    is Game -> gameInteractor.onGamePlay(item)
                }
            }
    }

    private fun addSystemTitle(root: ViewGroup) {
        val titleView =
            TextView(requireContext()).apply {
                text = getString(MetaSystemID.valueOf(args.metaSystemId).titleResId)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_dark_onBackground))
                textSize = TITLE_TEXT_SIZE_SP
                gravity = Gravity.CENTER_HORIZONTAL
            }
        val layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            ).apply { topMargin = resources.getDimensionPixelSize(R.dimen.tv_games_title_top_margin) }
        root.addView(titleView, layoutParams)
    }

    /** Leanback centers the grid on screen: covers start instead from the top left corner, below the title. */
    private fun alignGridToTopStart(root: View) {
        val grid = root.findViewById<VerticalGridView>(androidx.leanback.R.id.browse_grid) ?: return
        (grid.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            it.gravity = Gravity.TOP or Gravity.START
            grid.layoutParams = it
        }
        grid.setGravity(Gravity.START)
        grid.windowAlignment = BaseGridView.WINDOW_ALIGN_LOW_EDGE
        grid.windowAlignmentOffsetPercent = 0f
        grid.setPadding(
            resources.getDimensionPixelSize(androidx.leanback.R.dimen.lb_browse_padding_start),
            resources.getDimensionPixelSize(R.dimen.tv_games_grid_top_padding),
            resources.getDimensionPixelSize(androidx.leanback.R.dimen.lb_browse_padding_end),
            grid.paddingBottom,
        )
    }

    /** As many columns as fit on screen, based on the box art shape of the system. */
    private fun computeNumberOfColumns(): Int {
        val metaSystem = MetaSystemID.valueOf(args.metaSystemId)
        val ratio = CoverAspectRatio.forSystem(metaSystem.systemIDs.firstOrNull())
        val cardSize = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.card_size)
        val cardWidth = (cardSize * ratio).toInt()
        val spacing = resources.getDimensionPixelSize(androidx.leanback.R.dimen.lb_browse_item_horizontal_spacing)
        val horizontalPadding =
            resources.getDimensionPixelSize(androidx.leanback.R.dimen.lb_browse_padding_start) +
                resources.getDimensionPixelSize(androidx.leanback.R.dimen.lb_browse_padding_end)
        val availableWidth = resources.displayMetrics.widthPixels - horizontalPadding
        val extraSpacing = resources.getDimensionPixelSize(R.dimen.tv_games_card_extra_spacing)
        return ((availableWidth + spacing) / (cardWidth + spacing + extraSpacing)).coerceAtLeast(1)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    companion object {
        private const val TITLE_TEXT_SIZE_SP = 28f
    }

    @dagger.Module
    class Module
}
