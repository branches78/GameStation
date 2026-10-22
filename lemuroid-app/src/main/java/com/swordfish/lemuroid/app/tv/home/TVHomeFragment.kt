package com.swordfish.lemuroid.app.tv.home

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.home.HomeDynamicRows
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.tv.main.TVMainTabsFragment
import com.swordfish.lemuroid.app.tv.shared.GamePresenter
import com.swordfish.lemuroid.app.tv.shared.TVEmptyLibraryCard
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

/**
 * Content of the "Home" tab on TV. Like on mobile, the rows follow the same order
 * (recents, favorites, discover). Recents and favorites are hidden while empty,
 * while an empty discover row shows a hint in its middle. With an empty library
 * only the directory selection is shown.
 */
class TVHomeFragment : RowsSupportFragment() {
    @Inject
    lateinit var retrogradeDb: RetrogradeDatabase

    @Inject
    lateinit var gameInteractor: GameInteractor

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    private var emptyLibraryCard: TVEmptyLibraryCard? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // The rows keep only a small side padding for the zoom of the focused card (see TVHomeRowsOverlay).
        val rowsInflater = inflater.cloneInContext(ContextThemeWrapper(requireContext(), R.style.TVHomeRowsOverlay))
        val rowsView = super.onCreateView(rowsInflater, container, savedInstanceState) ?: return null

        // The rows are wrapped to add the card shown when the library is empty, like the mobile one.
        // They are centered with the width of the other tabs and clipped there, so no cover is ever cut
        // by the screen sides, also while scrolling.
        val root = FrameLayout(requireContext())
        val rowEdge = resources.getDimensionPixelSize(R.dimen.tv_home_row_edge)
        val rowsLayoutParams =
            FrameLayout.LayoutParams(rowsContentWidth() + 2 * rowEdge, ViewGroup.LayoutParams.MATCH_PARENT)
        rowsLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(rowsView, rowsLayoutParams)

        val emptyView = inflater.inflate(R.layout.tv_home_empty_library, root, false)
        root.addView(emptyView)
        emptyLibraryCard = TVEmptyLibraryCard(emptyView, rowsView)

        return root
    }

    override fun onDestroyView() {
        emptyLibraryCard = null
        super.onDestroyView()
    }

    private fun updateEmptyLibraryCard(viewState: TVHomeViewModel.HomeViewState) {
        emptyLibraryCard?.update(
            isLibraryEmpty = viewState.metaSystems.isEmpty(),
            indexInProgress = viewState.indexInProgress,
            scanProgress = viewState.scanProgress,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        onItemViewClickedListener =
            OnItemViewClickedListener { _, item, _, _ ->
                when (item) {
                    is Game -> gameInteractor.onGamePlay(item)
                    is TVSetting -> onSettingClicked(item)
                }
            }

        createAdapter(emptyList())

        val factory = TVHomeViewModel.Factory(retrogradeDb, requireContext().applicationContext)
        val homeViewModel = ViewModelProvider(this, factory)[TVHomeViewModel::class.java]

        launchOnState(Lifecycle.State.RESUMED) {
            homeViewModel.getViewStates()
                .collect { update(it) }
        }
    }

    private fun onSettingClicked(setting: TVSetting) {
        when (setting.type) {
            TVSettingType.SHOW_ALL_FAVORITES -> (parentFragment as? TVMainTabsFragment)?.showFavorites()
            else -> Unit
        }
    }

    /** A row of the home screen: [key] identifies it, [items] are games or placeholders. */
    private data class HomeRow(val key: String, val title: String, val items: List<Any>)

    private var currentRows: List<Pair<String, String>> = emptyList()

    private fun update(viewState: TVHomeViewModel.HomeViewState) {
        updateEmptyLibraryCard(viewState)

        val rows = buildRows(viewState)

        // Rows appear, disappear or get a new title (e.g. "Because you played..."): rebuild them.
        val rowsStructure = rows.map { it.key to it.title }
        if (rowsStructure != currentRows) {
            createAdapter(rows)
        }

        val rowsAdapter = adapter ?: return
        rows.forEachIndexed { index, row ->
            val listRow = rowsAdapter.get(index) as ListRow
            (listRow.adapter as ArrayObjectAdapter).setItems(row.items, LEANBACK_MULTI_DIFF_CALLBACK)
        }
    }

    private fun buildRows(viewState: TVHomeViewModel.HomeViewState): List<HomeRow> {
        val rows = mutableListOf<HomeRow>()

        // Empty library (e.g. first launch): no rows, the centered directory card is shown instead.
        if (viewState.metaSystems.isEmpty()) {
            return rows
        }

        // Recents and favorites stay hidden until they have games.
        if (viewState.recentGames.isNotEmpty()) {
            rows += HomeRow(KEY_RECENTS, getString(R.string.tv_home_section_recents), viewState.recentGames)
        }

        if (viewState.favoritesGames.isNotEmpty()) {
            val favoritesItems =
                if (viewState.favoritesGames.size <= TVHomeViewModel.CAROUSEL_MAX_ITEMS) {
                    viewState.favoritesGames
                } else {
                    viewState.favoritesGames.subList(0, TVHomeViewModel.CAROUSEL_MAX_ITEMS) +
                        listOf(TVSetting(TVSettingType.SHOW_ALL_FAVORITES))
                }
            rows += HomeRow(KEY_FAVORITES, getString(R.string.tv_home_section_favorites), favoritesItems)
        }

        rows += dynamicRows(viewState, HomeDynamicRows.Placement.BEFORE_DISCOVER)

        val discoverItems = viewState.discoveryGames.ifEmpty { listOf(EmptyRowMessage(R.string.home_row_empty_discover)) }
        rows += HomeRow(KEY_DISCOVER, getString(R.string.discover), discoverItems)

        rows += dynamicRows(viewState, HomeDynamicRows.Placement.AFTER_DISCOVER)

        return rows
    }

    private fun dynamicRows(
        viewState: TVHomeViewModel.HomeViewState,
        placement: HomeDynamicRows.Placement,
    ): List<HomeRow> {
        return viewState.dynamicRows
            .filter { it.placement == placement }
            .map { HomeRow(it.key, it.title, it.games) }
    }

    private fun rowsContentWidth(): Int = rowsContentWidth(resources)

    private fun homeCardSize(): Int = homeCardSize(resources)

    private fun homeCardSpacing(): Int = homeCardSpacing(resources)

    private fun createAdapter(rows: List<HomeRow>) {
        val rowEdge = resources.getDimensionPixelSize(R.dimen.tv_home_row_edge)
        val result = ArrayObjectAdapter(HomeListRowPresenter(rowEdge, homeCardSpacing()))
        val cardSize = homeCardSize()
        val settingsCardPadding =
            resources.getDimensionPixelSize(
                com.swordfish.lemuroid.lib.R.dimen.settings_card_padding,
            )

        val presenter =
            ClassPresenterSelector().apply {
                addClassPresenter(Game::class.java, GamePresenter(cardSize, gameInteractor, fixedRatio = 1f))
                addClassPresenter(TVSetting::class.java, SettingPresenter(cardSize, settingsCardPadding))
                addClassPresenter(EmptyRowMessage::class.java, EmptyRowMessagePresenter(cardSize, rowsContentWidth()))
            }

        rows.forEachIndexed { index, row ->
            result.add(ListRow(HeaderItem(index.toLong(), row.title), ArrayObjectAdapter(presenter)))
        }

        adapter = result
        currentRows = rows.map { it.key to it.title }
    }

    /**
     * Rows that move one card at a time: the focused card is aligned to the first position (like Google TV),
     * and at the end of the row the last cards fill it. As all the cards have the same width, the visible
     * cards are always whole.
     */
    private class HomeListRowPresenter(
        private val rowEdge: Int,
        private val cardSpacing: Int,
    ) : ListRowPresenter() {
        override fun initializeRowViewHolder(holder: RowPresenter.ViewHolder) {
            super.initializeRowViewHolder(holder)
            val gridView = (holder as ListRowPresenter.ViewHolder).gridView
            gridView.horizontalSpacing = cardSpacing
            gridView.windowAlignment = BaseGridView.WINDOW_ALIGN_BOTH_EDGE
            gridView.windowAlignmentOffset = rowEdge
            gridView.windowAlignmentOffsetPercent = BaseGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
            gridView.itemAlignmentOffset = 0
            gridView.itemAlignmentOffsetPercent = 0f
        }
    }

    companion object {
        /** Same width as the content of the other tabs, rounded down to whole cards. */
        fun rowsContentWidth(resources: Resources): Int {
            val cardsPerRow = cardsPerRow(resources)
            return cardsPerRow * homeCardSize(resources) + (cardsPerRow - 1) * homeCardSpacing(resources)
        }

        /** Size of the square game cards of the home rows (also used by the favorites grid). */
        fun homeCardSize(resources: Resources): Int {
            val cardsPerRow = cardsPerRow(resources)
            val contentWidth = resources.getDimensionPixelSize(R.dimen.tv_content_width)
            return (contentWidth - (cardsPerRow - 1) * homeCardSpacing(resources)) / cardsPerRow
        }

        fun homeCardSpacing(resources: Resources): Int = resources.getDimensionPixelSize(R.dimen.tv_home_card_spacing)

        fun cardsPerRow(resources: Resources): Int = resources.getInteger(R.integer.tv_home_cards_per_row)

        private const val KEY_RECENTS = "recents"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_DISCOVER = "discover"

        val LEANBACK_MULTI_DIFF_CALLBACK =
            object : DiffCallback<Any>() {
                override fun areContentsTheSame(
                    oldItem: Any,
                    newItem: Any,
                ): Boolean {
                    return when {
                        (oldItem is Game && newItem is Game) -> {
                            LEANBACK_GAME_DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        }
                        (oldItem is TVSetting && newItem is TVSetting) -> {
                            LEANBACK_SETTING_DIFF_CALLBACK.areContentsTheSame(oldItem, newItem)
                        }
                        else -> oldItem == newItem
                    }
                }

                override fun areItemsTheSame(
                    oldItem: Any,
                    newItem: Any,
                ): Boolean {
                    return when {
                        (oldItem is Game && newItem is Game) -> {
                            LEANBACK_GAME_DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
                        }
                        (oldItem is TVSetting && newItem is TVSetting) -> {
                            LEANBACK_SETTING_DIFF_CALLBACK.areItemsTheSame(oldItem, newItem)
                        }
                        else -> oldItem == newItem
                    }
                }
            }

        val LEANBACK_GAME_DIFF_CALLBACK =
            object : DiffCallback<Game>() {
                override fun areContentsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areItemsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem.id == newItem.id
                }
            }

        val LEANBACK_SYSTEM_DIFF_CALLBACK =
            object : DiffCallback<MetaSystemInfo>() {
                override fun areContentsTheSame(
                    oldInfo: MetaSystemInfo,
                    newInfo: MetaSystemInfo,
                ): Boolean {
                    return oldInfo == newInfo
                }

                override fun areItemsTheSame(
                    oldInfo: MetaSystemInfo,
                    newInfo: MetaSystemInfo,
                ): Boolean {
                    return oldInfo.metaSystem.name == newInfo.metaSystem.name
                }
            }

        val LEANBACK_SETTING_DIFF_CALLBACK =
            object : DiffCallback<TVSetting>() {
                override fun areContentsTheSame(
                    oldItem: TVSetting,
                    newItem: TVSetting,
                ): Boolean {
                    return oldItem == newItem
                }

                override fun areItemsTheSame(
                    oldItem: TVSetting,
                    newItem: TVSetting,
                ): Boolean {
                    return oldItem.type == newItem.type
                }
            }
    }

    @dagger.Module
    class Module
}
