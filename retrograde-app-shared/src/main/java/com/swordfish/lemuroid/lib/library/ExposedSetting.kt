package com.swordfish.lemuroid.lib.library

import java.io.Serializable

data class ExposedSetting(
    val key: String,
    val titleId: Int,
    val values: ArrayList<Value> = arrayListOf(),
    // Optional short explanation shown below the option title.
    val descriptionId: Int? = null,
) : Serializable {
    data class Value(val key: String, val titleId: Int) : Serializable
}
