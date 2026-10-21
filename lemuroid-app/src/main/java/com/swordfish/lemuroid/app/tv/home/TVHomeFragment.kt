package com.swordfish.lemuroid.app.tv.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.home.HomeDynamicRows
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.tv.shared.GamePresenter
import com.swordfish.lemuroid.app.tv.shared.TVHelper
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

    private var emptyLibraryView: View? = null
    private var rowsView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rowsView = super.onCreateView(inflater, container, savedInstanceState) ?: return null

        // The rows are wrapped to add the card shown when the library is empty, like the mobile one.
        val root = FrameLayout(requireContext())
        root.addView(rowsView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val emptyView = inflater.inflate(R.layout.tv_home_empty_library, root, false)
        emptyView.findViewById<View>(R.id.tv_empty_library_action)
            .setOnClickListener { TVHelper.launchFolderPicker(requireContext()) }
        root.addView(emptyView)
        emptyLibraryView = emptyView
        this.rowsView = rowsView

        return root
    }

    override fun onDestroyView() {
        emptyLibraryView = null
        rowsView = null
        super.onDestroyView()
    }

    private fun updateEmptyLibraryCard(viewState: TVHomeViewModel.HomeViewState) {
        val emptyView = emptyLibraryView ?: return
        val isLibraryEmpty = viewState.metaSystems.isEmpty()
        emptyView.isVisible = isLibraryEmpty
        // The empty rows list is still focusable and would take the remote focus instead of the button.
        rowsView?.isInvisible = isLibraryEmpty
        emptyView.findViewById<View>(R.id.tv_empty_library_action).isEnabled = !viewState.indexInProgress
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
            TVSettingType.SHOW_ALL_FAVORITES -> findNavController().navigate(R.id.navigation_favorites)
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

    private fun createAdapter(rows: List<HomeRow>) {
        val result = ArrayObjectAdapter(ListRowPresenter())
        val cardSize = resources.getDimensionPixelSize(com.swordfish.lemuroid.lib.R.dimen.card_size)
        val settingsCardPadding =
            resources.getDimensionPixelSize(
                com.swordfish.lemuroid.lib.R.dimen.settings_card_padding,
            )

        val presenter =
            ClassPresenterSelector().apply {
                addClassPresenter(Game::class.java, GamePresenter(cardSize, gameInteractor))
                addClassPresenter(TVSetting::class.java, SettingPresenter(cardSize, settingsCardPadding))
                addClassPresenter(EmptyRowMessage::class.java, EmptyRowMessagePresenter(cardSize))
            }

        rows.forEachIndexed { index, row ->
            result.add(ListRow(HeaderItem(index.toLong(), row.title), ArrayObjectAdapter(presenter)))
        }

        adapter = result
        currentRows = rows.map { it.key to it.title }
    }

    companion object {
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
