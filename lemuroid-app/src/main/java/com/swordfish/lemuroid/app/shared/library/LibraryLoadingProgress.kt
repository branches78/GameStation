package com.swordfish.lemuroid.app.shared.library

/**
 * Progress of the library indexing work: first the files of the directory are recognized, then the cores of the
 * systems found are prepared. [total] is null while the files are still being listed.
 */
data class LibraryLoadingProgress(val stage: Stage, val processed: Int, val total: Int?) {
    enum class Stage { FILES, CORES }

    /** Completed part of both stages, from 0 to 1, or null while the files are still being listed. */
    val fraction: Float?
        get() {
            val total = total ?: return if (stage == Stage.CORES) FILES_SHARE else null
            val stageFraction = if (total == 0) 1f else (processed.toFloat() / total).coerceIn(0f, 1f)
            return when (stage) {
                Stage.FILES -> stageFraction * FILES_SHARE
                Stage.CORES -> FILES_SHARE + stageFraction * (1f - FILES_SHARE)
            }
        }

    companion object {
        // Part of the bar used by the files, the rest is for the cores.
        private const val FILES_SHARE = 0.85f
    }
}
