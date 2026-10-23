package com.swordfish.lemuroid.app.shared.covers

import android.graphics.Bitmap
import android.graphics.Color
import coil.size.Size
import coil.transform.Transformation

/**
 * Removes the black bands around a screenshot. Many screenshots of the libretro thumbnails server (e.g. Nintendo 64
 * ones) have them on the sides: in the backdrop they show as a black stripe on the right edge and as a hard line
 * where the picture starts on the left one.
 *
 * Only whole black rows and columns are removed, and at most [MAX_TRIM] of each side, so dark scenes are kept.
 */
class TrimBlackBordersTransformation : Transformation {
    override val cacheKey: String = "trim_black_borders"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val width = input.width
        val height = input.height
        val maxColumns = (width * MAX_TRIM).toInt()
        val maxRows = (height * MAX_TRIM).toInt()

        val left = (0 until maxColumns).firstOrNull { !isBlackColumn(input, it) } ?: return input
        val right = (width - 1 downTo width - maxColumns).firstOrNull { !isBlackColumn(input, it) } ?: return input
        val top = (0 until maxRows).firstOrNull { !isBlackRow(input, it, left, right) } ?: return input
        val bottom = (height - 1 downTo height - maxRows).firstOrNull { !isBlackRow(input, it, left, right) } ?: return input

        if (left == 0 && top == 0 && right == width - 1 && bottom == height - 1) return input
        return Bitmap.createBitmap(input, left, top, right - left + 1, bottom - top + 1)
    }

    private fun isBlackColumn(
        bitmap: Bitmap,
        x: Int,
    ): Boolean = (0 until bitmap.height).all { isBlack(bitmap.getPixel(x, it)) }

    private fun isBlackRow(
        bitmap: Bitmap,
        y: Int,
        fromX: Int,
        toX: Int,
    ): Boolean = (fromX..toX).all { isBlack(bitmap.getPixel(it, y)) }

    private fun isBlack(color: Int): Boolean {
        return Color.red(color) < BLACK_THRESHOLD && Color.green(color) < BLACK_THRESHOLD && Color.blue(color) < BLACK_THRESHOLD
    }

    override fun equals(other: Any?) = other is TrimBlackBordersTransformation

    override fun hashCode() = javaClass.hashCode()

    companion object {
        private const val MAX_TRIM = 0.2f
        private const val BLACK_THRESHOLD = 24
    }
}
