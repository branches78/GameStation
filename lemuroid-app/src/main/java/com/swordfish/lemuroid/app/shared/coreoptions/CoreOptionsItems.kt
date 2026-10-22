package com.swordfish.lemuroid.app.shared.coreoptions

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState

/**
 * Core options as settings rows. Shared by the in-game menu and the app settings, so both edit the same values.
 * With a game [scope] the values are saved only for that game: until one is changed it shows the value of the
 * system, and the changed ones are marked with "This game".
 */
@Composable
fun CoreOptionsItems(
    scope: CoreOptionsScope,
    coreOptions: List<LemuroidCoreOption>,
) {
    val context = LocalContext.current

    for (coreOption in coreOptions) {
        val preferenceKey = scope.preferenceKey(coreOption.getKey())
        val description = coreOption.getDescription(context)
        val subtitle: (@Composable () -> Unit)? = description?.let { { Text(text = it) } }
        // Value shown until the user picks one: the system one for a game, else the one really in use.
        val currentValue =
            (scope as? CoreOptionsScope.Game)
                ?.let { CoreOptionsScope.systemValue(context, coreOption.getKey(), it.systemId) }
                ?: coreOption.getCurrentValue()

        // The rows remember their value: a new key (e.g. another scope) needs new rows.
        key(preferenceKey) {
            if (coreOption.isBoolean()) {
                val state = booleanPreferenceState(preferenceKey, currentValue == "enabled")
                val gameValue = state.value.let { scope.hasOwnValue(context, coreOption.getKey()) }
                LemuroidSettingsSwitch(
                    state = state,
                    title = { CoreOptionTitle(coreOption.getDisplayName(context), gameValue) },
                    subtitle = subtitle,
                )
            } else {
                val values = coreOption.getEntriesValues()
                val defaultValue = currentValue.takeIf { it in values } ?: values.first()
                val state = indexPreferenceState(preferenceKey, defaultValue, values)
                val gameValue = state.value.let { scope.hasOwnValue(context, coreOption.getKey()) }
                LemuroidSettingsList(
                    title = { CoreOptionTitle(coreOption.getDisplayName(context), gameValue) },
                    items = coreOption.getEntries(context),
                    state = state,
                )
            }
        }
    }
}

private fun CoreOptionsScope.hasOwnValue(
    context: Context,
    variable: String,
): Boolean {
    return this is CoreOptionsScope.Game && CoreOptionsScope.hasGameValue(context, variable, gameId)
}

/** Option title, followed by the "This game" mark when the value was chosen only for the game. */
@Composable
fun CoreOptionTitle(
    title: String,
    gameValue: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(modifier = Modifier.weight(1f, fill = false), text = title)
        if (gameValue) {
            GameValueBadge()
        }
    }
}

@Composable
fun GameValueBadge() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            text = stringResource(R.string.core_options_game_value),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
