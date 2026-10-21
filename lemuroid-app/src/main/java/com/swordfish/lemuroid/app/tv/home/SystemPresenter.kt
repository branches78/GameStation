package com.swordfish.lemuroid.app.tv.home

import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.app.tv.shared.TVCardStyle

class SystemPresenter(private val cardSize: Int, private val cardPadding: Int) : Presenter() {
    override fun onBindViewHolder(
        viewHolder: Presenter.ViewHolder?,
        item: Any,
    ) {
        val systemInfo = item as MetaSystemInfo
        val context = (viewHolder as ViewHolder).view.context

        viewHolder.mCardView.titleText = context.resources.getString(systemInfo.metaSystem.titleResId)
        viewHolder.mCardView.contentText = context.getString(R.string.system_grid_details, systemInfo.count.toString())
        viewHolder.mCardView.setMainImageDimensions(cardSize, cardSize)
        viewHolder.mCardView.mainImageView.setImageResource(systemInfo.metaSystem.imageResId)
        viewHolder.mCardView.mainImageView.setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
        viewHolder.mCardView.setMainImageScaleType(ImageView.ScaleType.FIT_CENTER)
        viewHolder.mCardView.mainImageView.setBackgroundColor(systemInfo.metaSystem.color())
    }

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        TVCardStyle.apply(cardView)
        return ViewHolder(cardView)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {}

    class ViewHolder(view: ImageCardView) : Presenter.ViewHolder(view) {
        val mCardView: ImageCardView = view
    }
}
