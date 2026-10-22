package com.swordfish.lemuroid.app.utils.android.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Settings items of one screen, in the order they appear. On TV it lets a screen move the focus to its first item
 * or back to the item that opened another screen: without it the focus jumps out of the settings (to the search
 * button) when the focused item leaves the screen. Items are identified by their position in the composition,
 * the same key rememberSaveable uses, so it is still valid when the screen is shown again.
 */
class SettingsFocusMemory {
    internal val items = mutableStateListOf<Pair<Int, FocusRequester>>()

    var onItemClicked: (Int) -> Unit = { }

    fun firstItem(): FocusRequester? = items.firstOrNull()?.second

    fun item(key: Int): FocusRequester? = items.firstOrNull { it.first == key }?.second
}

/** Only provided on TV: on mobile the settings items don't track the focus. */
val LocalSettingsFocusMemory = staticCompositionLocalOf<SettingsFocusMemory?> { null }

internal class SettingsFocusItem(val modifier: Modifier, val onClicked: () -> Unit)

@Composable
internal fun rememberSettingsFocusItem(): SettingsFocusItem? {
    val memory = LocalSettingsFocusMemory.current ?: return null
    val key = currentCompositeKeyHash
    val requester = remember { FocusRequester() }
    DisposableEffect(memory, key) {
        val entry = key to requester
        memory.items.add(entry)
        onDispose { memory.items.remove(entry) }
    }
    return remember(memory, key) {
        SettingsFocusItem(Modifier.focusRequester(requester)) { memory.onItemClicked(key) }
    }
}
