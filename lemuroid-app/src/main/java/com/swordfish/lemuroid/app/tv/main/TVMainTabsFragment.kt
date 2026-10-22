package com.swordfish.lemuroid.app.tv.main

import android.os.Bundle
import android.view.FocusFinder
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.widget.BrowseFrameLayout
import androidx.leanback.widget.SearchOrbView
import androidx.navigation.fragment.findNavController
import com.swordfish.lemuroid.R
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
            )

        tabs.forEachIndexed { index, tab ->
            // Like most TV launchers, moving the focus over a tab switches the content.
            tab.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) selectTab(index)
            }
            tab.setOnClickListener { focusContent() }
        }

        view.findViewById<SearchOrbView>(R.id.tv_search_orb).apply {
            orbColor = ContextCompat.getColor(requireContext(), R.color.md_theme_dark_primary)
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
                getContentView()?.hasFocus() == true -> tabs[selectedTab].requestFocus()
                selectedTab != TAB_HOME -> tabs[TAB_HOME].requestFocus()
                else -> {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        view.post {
            if (!view.hasFocus()) {
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
        if (focused == null || direction != View.FOCUS_UP || !isDescendant(content, focused)) {
            return null
        }

        // When moving up out of the content, always land on the selected tab.
        val nextInContent = FocusFinder.getInstance().findNextFocus(content, focused, direction)
        return if (nextInContent == null) tabs[selectedTab] else null
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
        val fragment =
            when (index) {
                TAB_SYSTEMS -> TVSystemsFragment()
                TAB_SETTINGS -> TVSettingsTabFragment()
                else -> TVHomeFragment()
            }
        childFragmentManager.beginTransaction()
            .replace(R.id.tv_tab_content, fragment)
            .commit()
    }

    private fun updateTabsState() {
        tabs.forEachIndexed { index, tab -> tab.isSelected = index == selectedTab }
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
        private const val TAB_HOME = 0
        private const val TAB_SYSTEMS = 1
        private const val TAB_SETTINGS = 2
    }
}
