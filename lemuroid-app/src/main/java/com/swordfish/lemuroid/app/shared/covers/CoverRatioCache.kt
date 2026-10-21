package com.swordfish.lemuroid.app.shared.covers

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Remembers the real width / height ratio of every box art once it has been loaded,
 * so cards can take the shape of each cover (e.g. NES games from Japan often have landscape boxes).
 *
 * Values are kept on the device and backed by a Compose state map, so cards update as soon as a cover is measured.
 */
object CoverRatioCache {
    private const val PREFERENCES_NAME = "cover_ratios"
    private const val MIN_RATIO = 0.5f
    private const val MAX_RATIO = 2.0f

    // Upper bound for very large libraries: when reached, everything is measured again as covers load.
    private const val MAX_ENTRIES = 5000

    // Covers whose shape is this close to the expected one are cropped to fill the card instead of letterboxed.
    private const val SAME_SHAPE_TOLERANCE = 0.1f

    private val ratios = mutableStateMapOf<String, Float>()
    private var preferences: SharedPreferences? = null

    fun init(context: Context) {
        if (preferences != null) return
        val prefs = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (url, value) -> (value as? Float)?.let { ratios[url] = it } }
        preferences = prefs
    }

    fun get(url: String?): Float? = url?.let { ratios[it] }

    fun store(
        url: String?,
        drawable: Drawable,
    ): Float? {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (url == null || width <= 0 || height <= 0) return null

        // Rounded to avoid tiny differences between decodes of the same image.
        val ratio = ((width.toFloat() / height) * 100).roundToInt() / 100f
        val clamped = ratio.coerceIn(MIN_RATIO, MAX_RATIO)
        if (ratios[url] != clamped) {
            if (ratios.size >= MAX_ENTRIES) {
                ratios.clear()
                preferences?.edit { clear() }
            }
            ratios[url] = clamped
            preferences?.edit { putFloat(url, clamped) }
        }
        return clamped
    }

    /** True when the cover would be cut noticeably if cropped to [slotRatio]. */
    fun needsLetterbox(
        coverRatio: Float?,
        slotRatio: Float,
    ): Boolean {
        if (coverRatio == null) return false
        return abs(coverRatio - slotRatio) / slotRatio > SAME_SHAPE_TOLERANCE
    }
}
