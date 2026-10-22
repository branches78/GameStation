package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import com.swordfish.lemuroid.R
import java.text.NumberFormat

/** Texts of the library scan progress, shared by the mobile and TV screens. */
object LibraryScanProgressFormatter {
    /** Percentage in the format of the current language (e.g. "45%" or "45 %"). */
    fun percent(fraction: Float): String {
        return NumberFormat.getPercentInstance().format(fraction)
    }

    /** "Looking for files…" while listing, then "121 of 268 files" and at last "Preparing cores: 2 of 5". */
    fun details(
        context: Context,
        progress: LibraryLoadingProgress,
    ): String {
        val total = progress.total ?: return context.getString(R.string.library_loading_listing)
        val processed = progress.processed.coerceAtMost(total)
        return when (progress.stage) {
            LibraryLoadingProgress.Stage.FILES -> context.getString(R.string.library_loading_files, processed, total)
            LibraryLoadingProgress.Stage.CORES -> context.getString(R.string.library_loading_cores, processed, total)
        }
    }
}
