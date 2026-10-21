package com.swordfish.lemuroid.app.shared.coreoptions

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.lib.core.CoreVariablesManager

/** Core options as settings rows. Shared by the in-game menu and the app settings, so both edit the same values. */
@Composable
fun CoreOptionsItems(
    systemID: String,
    coreOptions: List<LemuroidCoreOption>,
) {
    val context = LocalContext.current

    for (coreOption in coreOptions) {
        val preferenceKey = CoreVariablesManager.computeSharedPreferenceKey(coreOption.getKey(), systemID)
        val description = coreOption.getDescription(context)
        val subtitle: (@Composable () -> Unit)? = description?.let { { Text(text = it) } }

        if (coreOption.isBoolean()) {
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(preferenceKey, coreOption.getCurrentValue() == "enabled"),
                title = { Text(text = coreOption.getDisplayName(context)) },
                subtitle = subtitle,
            )
        } else {
            val values = coreOption.getEntriesValues()
            // Until the user picks a value, show the one really in use instead of the first of the list.
            val defaultValue = coreOption.getCurrentValue().takeIf { it in values } ?: values.first()
            LemuroidSettingsList(
                title = { Text(text = coreOption.getDisplayName(context)) },
                items = coreOption.getEntries(context),
                state = indexPreferenceState(preferenceKey, defaultValue, values),
            )
        }
    }
}
