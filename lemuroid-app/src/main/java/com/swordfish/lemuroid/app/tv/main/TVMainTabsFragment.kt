package com.swordfish.lemuroid.app.tv.main

import android.os.Bundle
import android.view.FocusFinder
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.SearchOrbView
import androidx.navigation.fragment.findNavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.tv.about.TVAboutFragment
import com.swordfish.lemuroid.app.tv.favorites.TVFavoritesFragment
import com.swordfish.lemuroid.app.tv.games.TVGamesFragment
import com.swordfish.lemuroid.app.tv.home.TVHomeFragment
import com.swordfish.lemuroid.app.tv.settings.TVSettingsTabFragment
import com.swordfish.lemuroid.app.tv.systems.TVSystemsFragment

/**
 * TV start screen: search button on the left, centered tabs (home, systems, settings)
 * and the app name on the right. The selected tab content is shown below.
 */
class TVMainTabsFragment : Fragment(R.layout.fragment_tv_main_tabs) {
    private var selectedTab = TAB_HOME
    private var tabs: List<View> = emptyList()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(KEY_SELECTED_TAB, TAB_HOME)
        }

        tabs =
            listOf(
                view.findViewById(R.id.tv_tab_home),
                view.findViewById(R.id.tv_tab_systems),
                view.findViewById(R.id.tv_tab_settings),
                view.findViewById(R.id.tv_tab_about),
            )

        tabs.forEachIndexed { index, tab ->
            // Like most TV launchers, moving the focus over a tab switches the content.
            tab.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) selectTab(index)
            }
            tab.setOnClickListener { focusContent() }
        }

        view.findViewById<SearchOrbView>(R.id.tv_search_orb).apply {
            orbColor = ContextCompat.getColor(requireContext(), R.color.tv_accent)
            setOnOrbClickedListener { findNavController().navigate(R.id.navigation_search) }
        }

        view.findViewById<BrowseFrameLayout>(R.id.tv_tabs_root).onFocusSearchListener =
            BrowseFrameLayout.OnFocusSearchListener { focused, direction ->
                onFocusSearch(focused, direction)
            }

        if (childFragmentManager.findFragmentById(R.id.tv_tab_content) == null) {
            showTabContent(selectedTab)
        }
        updateTabsState()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            when {
                isShowingInnerScreen() -> closeInnerScreen()
                getContentView()?.hasFocus() == true -> tabs[selectedTab].requestFocus()
                selectedTab != TAB_HOME -> tabs[TAB_HOME].requestFocus()
                else -> {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        // The search orb is the first focusable view, so Android gives it the initial focus: start on the tab instead.
        val searchOrb = view.findViewById<View>(R.id.tv_search_orb)
        view.post {
            if (!view.hasFocus() || searchOrb.hasFocus()) {
                tabs[selectedTab].requestFocus()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, selectedTab)
    }

    private fun onFocusSearch(
        focused: View?,
        direction: Int,
    ): View? {
        val content = getContentView() ?: return null
        if (focused == null || !isDescendant(content, focused)) {
            return null
        }

        val nextInContent = FocusFinder.getInstance().findNextFocus(content, focused, direction)
        return when {
            nextInContent != null -> null
            // When moving up out of the content, always land on the selected tab.
            direction == View.FOCUS_UP -> tabs[selectedTab]
            // Left, right or down out of the content (e.g. left on the first card of a row) keeps the focus where it
            // is: otherwise Android would move it to the search button, the closest view on that side.
            else -> focused
        }
    }

    private fun isDescendant(
        parent: ViewGroup,
        view: View,
    ): Boolean {
        var current: View? = view
        while (current != null) {
            if (current === parent) return true
            current = current.parent as? View
        }
        return false
    }

    private fun selectTab(index: Int) {
        if (index == selectedTab && childFragmentManager.findFragmentById(R.id.tv_tab_content) != null) {
            updateTabsState()
            return
        }
        selectedTab = index
        showTabContent(index)
        updateTabsState()
    }

    private fun showTabContent(index: Int) {
        // The games of a system and the favorites belong to their tab: they are closed when another tab is selected.
        childFragmentManager.popBackStack(BACK_STACK_INNER_SCREEN, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val fragment =
            when (index) {
                TAB_SYSTEMS -> TVSystemsFragment()
                TAB_SETTINGS -> TVSettingsTabFragment()
                TAB_ABOUT -> TVAboutFragment()
                else -> TVHomeFragment()
            }
        childFragmentManager.beginTransaction()
            .replace(R.id.tv_tab_content, fragment)
            .commit()
    }

    private fun updateTabsState() {
        tabs.forEachIndexed { index, tab -> tab.isSelected = index == selectedTab }
    }

    /** Opens the games of a system in place of the "Consoles" grid, keeping the tabs bar on screen. */
    fun showSystemGames(metaSystemId: String) {
        showInnerScreen(TVGamesFragment.newInstance(metaSystemId))
    }

    /** Opens all the favorites ("Show all" card) in place of the "Home" rows, keeping the tabs bar on screen. */
    fun showFavorites() {
        showInnerScreen(TVFavoritesFragment())
    }

    private fun showInnerScreen(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.tv_tab_content, fragment)
            .addToBackStack(BACK_STACK_INNER_SCREEN)
            .commit()
        childFragmentManager.executePendingTransactions()
        focusContent()
    }

    private fun isShowingInnerScreen(): Boolean = childFragmentManager.backStackEntryCount > 0

    /** Back from the games of a system or from the favorites returns to the tab content they were opened from. */
    private fun closeInnerScreen() {
        val contentHadFocus = getContentView()?.hasFocus() == true
        childFragmentManager.popBackStackImmediate()
        if (contentHadFocus) focusContent()
    }

    /** Used by the tab contents that cannot move the focus up to the tabs bar by themselves. */
    fun focusSelectedTab(): Boolean {
        return tabs.getOrNull(selectedTab)?.requestFocus() == true
    }

    private fun focusContent() {
        getContentView()?.requestFocus()
    }

    private fun getContentView(): ViewGroup? = view?.findViewById(R.id.tv_tab_content)

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
        private const val BACK_STACK_INNER_SCREEN = "inner_screen"
        private const val TAB_HOME = 0
        private const val TAB_SYSTEMS = 1
        private const val TAB_SETTINGS = 2
        private const val TAB_ABOUT = 3
    }
}
