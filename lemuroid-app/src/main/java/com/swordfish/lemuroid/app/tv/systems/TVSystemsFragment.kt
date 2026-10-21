package com.swordfish.lemuroid.app.tv.systems

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.tv.home.SystemPresenter
import com.swordfish.lemuroid.app.tv.home.TVHomeFragment
import com.swordfish.lemuroid.app.tv.home.TVHomeViewModel
import com.swordfish.lemuroid.app.tv.main.TVMainTabsFragmentDirections
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Content of the "Systems" tab on TV: a grid with every system that has games. */
class TVSystemsFragment : VerticalGridSupportFragment() {
    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    init {
        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns = GRID_COLUMNS
        setGridPresenter(gridPresenter)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // The tabs bar already works as title, so the default leanback title area is not needed.
        showTitle(false)
        view.findViewById<View>(androidx.leanback.R.id.browse_grid)?.let {
            val topPadding = resources.getDimensionPixelSize(R.dimen.tv_systems_grid_top_padding)
            it.setPadding(it.paddingLeft, topPadding, it.paddingRight, it.paddingBottom)
        }
        allowFocusUpToTabs(view)

        val cardSize = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.card_size)
        val cardPadding = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.systems_card_padding)
        val systemsAdapter = ArrayObjectAdapter(SystemPresenter(cardSize, cardPadding))
        adapter = systemsAdapter

        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                if (item is MetaSystemInfo) {
                    val action =
                        TVMainTabsFragmentDirections.actionNavigationSystemsToNavigationGames(
                            item.metaSystem.name,
                        )
                    findNavController().navigate(action)
                }
            }

        val factory = TVHomeViewModel.Factory(retrogradeDb, requireContext().applicationContext)
        val viewModel = ViewModelProvider(this, factory)[TVHomeViewModel::class.java]

        launchOnState(Lifecycle.State.RESUMED) {
            viewModel.getViewStates()
                .map { it.metaSystems }
                .distinctUntilChanged()
                .collect { systemsAdapter.setItems(it, TVHomeFragment.LEANBACK_SYSTEM_DIFF_CALLBACK) }
        }
    }

    /**
     * Leanback moves the focus up from the first row to its own title area, which is hidden here,
     * so the focus got stuck. Letting it leave the grid makes the tabs bar take it instead.
     */
    private fun allowFocusUpToTabs(view: View) {
        (view.findViewById<View>(androidx.leanback.R.id.grid_frame) as? BrowseFrameLayout)
            ?.onFocusSearchListener = null
        (view.findViewById<View>(androidx.leanback.R.id.browse_grid) as? VerticalGridView)
            ?.setFocusOutAllowed(true, false)
    }

    companion object {
        private const val GRID_COLUMNS = 5
    }

    @dagger.Module
    class Module
}
