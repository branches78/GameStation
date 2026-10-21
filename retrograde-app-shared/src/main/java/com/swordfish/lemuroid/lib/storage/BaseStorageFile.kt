package com.swordfish.lemuroid.lib.storage

import android.net.Uri

data class BaseStorageFile(
    val name: String,
    val size: Long,
    val uri: Uri,
    val path: String? = null,
    // Used on rescan to detect files that changed since they were indexed.
    val lastModified: Long = 0,
) {
    val extension: String
        get() = name.substringAfterLast('.', "")

    val extensionlessName: String
        get() = name.substringBeforeLast('.', "")
}
