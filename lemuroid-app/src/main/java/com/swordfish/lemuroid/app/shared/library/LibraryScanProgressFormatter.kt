package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import java.text.NumberFormat

/** Texts of the library scan progress, shared by the mobile and TV screens. */
object LibraryScanProgressFormatter {
    /** Completed part from 0 to 1, or null while the files are still being listed. */
    fun fraction(progress: LemuroidLibrary.IndexProgress): Float? {
        val total = progress.total ?: return null
        if (total == 0) return 1f
        return (progress.processed.toFloat() / total).coerceIn(0f, 1f)
    }

    /** Percentage in the format of the current language (e.g. "45%" or "45 %"). */
    fun percent(fraction: Float): String {
        return NumberFormat.getPercentInstance().format(fraction)
    }

    /** "Looking for files…" while listing, then "121 of 268 files". */
    fun details(
        context: Context,
        progress: LemuroidLibrary.IndexProgress,
    ): String {
        val total = progress.total ?: return context.getString(R.string.library_loading_listing)
        return context.getString(R.string.library_loading_files, progress.processed.coerceAtMost(total), total)
    }
}
