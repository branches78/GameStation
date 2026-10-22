package com.swordfish.lemuroid.app.utils.android.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
    internal class Item(val key: Int, val requester: FocusRequester, val isEnabled: () -> Boolean)

    internal val items = mutableStateListOf<Item>()

    var onItemClicked: (Int) -> Unit = { }

    /** First item that can take the focus (disabled items can't). */
    fun firstItem(): FocusRequester? = items.firstOrNull { it.isEnabled() }?.requester

    fun item(key: Int): FocusRequester? = items.firstOrNull { it.key == key && it.isEnabled() }?.requester
}

/** Only provided on TV: on mobile the settings items don't track the focus. */
val LocalSettingsFocusMemory = staticCompositionLocalOf<SettingsFocusMemory?> { null }

/**
 * Registers [content] (one settings item) in the [SettingsFocusMemory] of the screen, if any, and gives
 * [content] the callback to call when the item is clicked. The focus requester goes on a box around the item:
 * the settings library puts the modifier of its tiles after their clickable, where it would find nothing to focus.
 */
@Composable
internal fun SettingsFocusItem(
    enabled: Boolean,
    content: @Composable (onClicked: () -> Unit) -> Unit,
) {
    val memory = LocalSettingsFocusMemory.current
    if (memory == null) {
        content { }
        return
    }

    val key = currentCompositeKeyHash
    val requester = remember { FocusRequester() }
    val currentEnabled = rememberUpdatedState(enabled)
    DisposableEffect(memory, key) {
        val item = SettingsFocusMemory.Item(key, requester) { currentEnabled.value }
        memory.items.add(item)
        onDispose { memory.items.remove(item) }
    }
    Box(modifier = Modifier.focusRequester(requester)) {
        content { memory.onItemClicked(key) }
    }
}
