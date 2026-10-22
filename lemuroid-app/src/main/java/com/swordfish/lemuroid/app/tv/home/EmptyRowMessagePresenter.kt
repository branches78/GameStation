package com.swordfish.lemuroid.app.tv.home

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.swordfish.lemuroid.R

/** Placeholder item shown in the middle of a home row when the row has no games. */
data class EmptyRowMessage(val textResId: Int)

class EmptyRowMessagePresenter(
    private val cardSize: Int,
    // Width of the visible part of the row, so the message is centered in it.
    private val rowWidth: Int,
) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val textView =
            TextView(context).apply {
                // As wide as the row, as tall as a game card with its texts.
                val height = cardSize + (CARD_INFO_HEIGHT_DP * context.resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.LayoutParams(rowWidth, height)
                gravity = Gravity.CENTER
                textSize = TEXT_SIZE_SP
                setTextColor(ContextCompat.getColor(context, R.color.md_theme_dark_onSurfaceVariant))
                // Focusable so the row can still be reached and scrolled into view with the remote.
                isFocusable = true
                isFocusableInTouchMode = true
            }
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        item: Any?,
    ) {
        val message = item as? EmptyRowMessage ?: return
        (viewHolder.view as TextView).setText(message.textResId)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}

    companion object {
        private const val CARD_INFO_HEIGHT_DP = 52
        private const val TEXT_SIZE_SP = 16f
    }
}
