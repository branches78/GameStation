package com.swordfish.lemuroid.app.tv.shared

import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.covers.CoverAspectRatio
import com.swordfish.lemuroid.app.shared.covers.CoverRatioCache
import com.swordfish.lemuroid.app.shared.covers.CoverUtils
import com.swordfish.lemuroid.app.utils.games.GameUtils
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlin.math.roundToInt

class GamePresenter(
    private val cardSize: Int,
    private val gameInteractor: GameInteractor,
    // When false (grids) every card keeps the shape of its system and other covers are shown whole inside it.
    private val matchCoverShape: Boolean = true,
) : Presenter() {
    override fun onBindViewHolder(
        viewHolder: Presenter.ViewHolder?,
        item: Any?,
    ) {
        if (item == null || viewHolder !is ViewHolder) return
        val game = item as Game
        viewHolder.mCardView.titleText = game.displayName()
        viewHolder.mCardView.contentText = GameUtils.getGameSubtitle(viewHolder.mCardView.context, game)
        // Same height for every card; the width follows the cover shape (like on mobile).
        updateCardShape(viewHolder, game)
        viewHolder.updateCardViewImage(game) { updateCardShape(viewHolder, game) }
        // A long press opens the same actions menu as on mobile.
        viewHolder.view.setOnLongClickListener {
            TVGameContextMenu.show(it.context, game, gameInteractor)
            true
        }
    }

    private fun updateCardShape(
        viewHolder: ViewHolder,
        game: Game,
    ) {
        val cardRatio = if (matchCoverShape) CoverAspectRatio.forGame(game) else CoverAspectRatio.forGameSystem(game)
        val cardWidth = (cardSize * cardRatio).roundToInt()
        val imageView = viewHolder.mCardView.mainImageView
        val layoutParams = imageView?.layoutParams
        if (layoutParams == null || layoutParams.width != cardWidth || layoutParams.height != cardSize) {
            viewHolder.mCardView.setMainImageDimensions(cardWidth, cardSize)
        }

        val coverRatio = CoverRatioCache.get(game.displayCoverUrl())
        imageView?.scaleType =
            if (CoverRatioCache.needsLetterbox(coverRatio, cardRatio)) {
                ImageView.ScaleType.FIT_CENTER
            } else {
                ImageView.ScaleType.CENTER_CROP
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        TVCardStyle.apply(cardView)
        return ViewHolder(cardView)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {
        val viewHolder = viewHolder as ViewHolder
        viewHolder.mCardView.mainImage = null
        viewHolder.view.setOnLongClickListener(null)
    }

    class ViewHolder(view: ImageCardView) : Presenter.ViewHolder(view) {
        val mCardView: ImageCardView = view

        fun updateCardViewImage(
            game: Game,
            onCoverMeasured: () -> Unit,
        ) {
            // A new load cancels the previous one of this view, so the callback always belongs to this game.
            CoverUtils.loadCover(game, mCardView.mainImageView) { drawable ->
                CoverRatioCache.store(game.displayCoverUrl(), drawable)
                onCoverMeasured()
            }
        }
    }
}
