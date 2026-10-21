package com.swordfish.lemuroid.app.tv.home

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Presenter
import com.swordfish.lemuroid.R

/** Placeholder item shown in the middle of a home row when the row has no games. */
data class EmptyRowMessage(val textResId: Int)

class EmptyRowMessagePresenter(private val cardSize: Int) : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val context = parent.context
        val textView =
            TextView(context).apply {
                // Wide enough to look centered in the row, as tall as a game card with its texts.
                val width = (context.resources.displayMetrics.widthPixels * WIDTH_FRACTION).toInt()
                val height = cardSize + (CARD_INFO_HEIGHT_DP * context.resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.LayoutParams(width, height)
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
        private const val WIDTH_FRACTION = 0.8f
        private const val CARD_INFO_HEIGHT_DP = 52
        private const val TEXT_SIZE_SP = 16f
    }
}
