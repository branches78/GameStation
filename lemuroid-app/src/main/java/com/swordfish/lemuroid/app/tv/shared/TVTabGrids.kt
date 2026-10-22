package com.swordfish.lemuroid.app.tv.shared

import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.widget.VerticalGridView
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.tv.home.TVHomeFragment
import com.swordfish.lemuroid.app.tv.main.TVMainTabsFragment

/** Helpers for the leanback grids shown inside the tabs of the TV start screen. */
object TVTabGrids {
    /**
     * Title of a grid (e.g. the system name), right below the tabs bar and aligned with the first column of cards.
     * It sits on an opaque band above the grid, so the cards scroll under it instead of over the text.
     */
    fun addTitle(
        root: ViewGroup,
        text: String,
    ) {
        val context = root.context
        val resources = context.resources
        val band =
            FrameLayout(context).apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.leanback_background))
                // Above the cards (also the zoomed ones), without a shadow.
                translationZ = resources.getDimension(R.dimen.tv_grid_title_elevation)
                outlineProvider = null
            }
        val titleView =
            TextView(context).apply {
                this.text = text
                setTextColor(ContextCompat.getColor(context, R.color.md_theme_dark_onBackground))
                textSize = TITLE_TEXT_SIZE_SP
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
        band.addView(
            titleView,
            FrameLayout.LayoutParams(
                TVHomeFragment.rowsContentWidth(resources),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER_HORIZONTAL,
            ),
        )
        root.addView(
            band,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.tv_grid_title_height),
                Gravity.TOP,
            ),
        )
    }

    /**
     * Leanback does not let the focus leave a grid from its first row (it tries to reach its own title area,
     * hidden here), so pressing up there moves the focus to the selected tab of the tabs bar instead.
     * [onOtherKey] gets the keys this does not handle (the grid has a single key intercept listener).
     */
    fun allowFocusUpToTabs(
        fragment: Fragment,
        grid: VerticalGridView,
        columns: Int,
        onOtherKey: (KeyEvent) -> Boolean = { false },
    ) {
        grid.setOnKeyInterceptListener { event ->
            val isUpOnFirstRow =
                event.action == KeyEvent.ACTION_DOWN &&
                    event.keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                    grid.selectedPosition < columns
            if (isUpOnFirstRow && (fragment.parentFragment as? TVMainTabsFragment)?.focusSelectedTab() == true) {
                true
            } else {
                onOtherKey(event)
            }
        }
    }

    /**
     * Grid with the square cards of the home rows, 5 per row (games of a system, favorites). It is as wide as the
     * other tabs content and its cards start from the left, also when a row is not full.
     */
    fun setUpHomeCardsGrid(grid: VerticalGridView) {
        val resources = grid.resources
        grid.horizontalSpacing = TVHomeFragment.homeCardSpacing(resources)
        grid.setGravity(Gravity.START)
        (grid.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.width = TVHomeFragment.rowsContentWidth(resources) + grid.paddingLeft + grid.paddingRight
            it.gravity = Gravity.CENTER_HORIZONTAL
            grid.layoutParams = it
        }
    }

    private const val TITLE_TEXT_SIZE_SP = 28f
}
