package com.swordfish.lemuroid.app.shared.library

/**
 * Whether the running scan started with an empty library: then its progress is shown instead of the games until it
 * ends. Kept outside the screens, so it survives changing tab on TV or rotating the phone in the middle of the scan.
 */
object EmptyLibraryLoading {
    @Volatile
    private var loadingEmptyLibrary = false

    fun isLoading(): Boolean = loadingEmptyLibrary

    fun update(
        isLibraryEmpty: Boolean,
        scanProgress: LibraryLoadingProgress?,
    ): Boolean {
        loadingEmptyLibrary =
            when {
                scanProgress == null -> false
                isLibraryEmpty -> true
                else -> loadingEmptyLibrary
            }
        return loadingEmptyLibrary
    }
}
