package com.swordfish.lemuroid.app.shared.coreoptions

import android.content.Context
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

object CoreOptionsPreferenceHelper {
    val BOOLEAN_SET = setOf("enabled", "disabled")

    private const val SCOPE_PREFERENCE_KEY = "core_options_scope"
    private const val SCOPE_GAME = "game"
    private const val SCOPE_SYSTEM = "system"

    /**
     * Selector between "Only this game" and "All the games of <system>": [onGameScopeChange] rebuilds the options
     * with the chosen scope.
     */
    fun addScopeSelector(
        preferenceScreen: PreferenceScreen,
        systemName: String,
        gameScope: Boolean,
        onGameScopeChange: (Boolean) -> Unit,
    ) {
        val context = preferenceScreen.context
        val preference = ListPreference(context)
        preference.key = SCOPE_PREFERENCE_KEY
        preference.isPersistent = false
        preference.title = context.getString(R.string.core_options_scope_title)
        preference.entries =
            arrayOf(
                context.getString(R.string.core_options_scope_game),
                context.getString(R.string.core_options_scope_system, systemName),
            )
        preference.entryValues = arrayOf(SCOPE_GAME, SCOPE_SYSTEM)
        preference.setDefaultValue(if (gameScope) SCOPE_GAME else SCOPE_SYSTEM)
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        preference.isIconSpaceReserved = false
        preference.setOnPreferenceChangeListener { _, newValue ->
            onGameScopeChange(newValue == SCOPE_GAME)
            true
        }
        preferenceScreen.addPreference(preference)
    }

    fun addPreferences(
        preferenceScreen: PreferenceScreen,
        scope: CoreOptionsScope,
        baseOptions: List<LemuroidCoreOption>,
        advancedOptions: List<LemuroidCoreOption>,
    ) {
        if (baseOptions.isEmpty() && advancedOptions.isEmpty()) {
            return
        }

        val context = preferenceScreen.context

        val title = context.getString(R.string.core_settings_category_preferences)
        val preferencesCategory = createCategory(preferenceScreen.context, preferenceScreen, title)

        addPreferences(context, preferencesCategory, baseOptions, scope)
        addPreferences(context, preferencesCategory, advancedOptions, scope)
    }

    fun addControllers(
        preferenceScreen: PreferenceScreen,
        systemID: String,
        coreID: CoreID,
        connectedGamePads: Int,
        controllers: Map<Int, List<ControllerConfig>>,
    ) {
        val visibleControllers =
            (0 until connectedGamePads)
                .map { it to controllers[it] }
                .filter { (_, controllers) -> controllers != null && controllers.size >= 2 }

        if (visibleControllers.isEmpty()) {
            return
        }

        val context = preferenceScreen.context
        val title = context.getString(R.string.core_settings_category_controllers)
        val category = createCategory(context, preferenceScreen, title)

        visibleControllers
            .forEach { (port, controllers) ->
                val preference = buildControllerPreference(context, systemID, coreID, port, controllers!!)
                category.addPreference(preference)
            }
    }

    private fun addPreferences(
        context: Context,
        preferenceGroup: PreferenceGroup,
        options: List<LemuroidCoreOption>,
        scope: CoreOptionsScope,
    ) {
        options
            .map { convertToPreference(context, it, scope) }
            .forEach { preferenceGroup.addPreference(it) }
    }

    private fun convertToPreference(
        context: Context,
        it: LemuroidCoreOption,
        scope: CoreOptionsScope,
    ): Preference {
        val preference =
            if (it.getEntriesValues().toSet() == BOOLEAN_SET) {
                buildSwitchPreference(context, it, scope)
            } else {
                buildListPreference(context, it, scope)
            }
        if (scope is CoreOptionsScope.Game) {
            saveOnlyForGame(context, preference, it, scope)
        }
        return preference
    }

    /**
     * The values of a game are saved by hand: a persisted preference would save its initial value too, giving the
     * game its own value for every option as soon as the menu is opened.
     */
    private fun saveOnlyForGame(
        context: Context,
        preference: Preference,
        option: LemuroidCoreOption,
        scope: CoreOptionsScope.Game,
    ) {
        preference.isPersistent = false
        preference.title = gameOptionTitle(context, option, scope)
        preference.setOnPreferenceChangeListener { changed, newValue ->
            SharedPreferencesHelper.getSharedPreferences(context).edit {
                when (newValue) {
                    is Boolean -> putBoolean(changed.key, newValue)
                    is String -> putString(changed.key, newValue)
                }
            }
            changed.title = gameOptionTitle(context, option, scope)
            true
        }
    }

    /** Title followed by "· This game" when the option has a value of its own for the game. */
    private fun gameOptionTitle(
        context: Context,
        option: LemuroidCoreOption,
        scope: CoreOptionsScope.Game,
    ): String {
        val title = option.getDisplayName(context)
        return if (CoreOptionsScope.hasGameValue(context, option.getKey(), scope.gameId)) {
            "$title · ${context.getString(R.string.core_options_game_value)}"
        } else {
            title
        }
    }

    /** Value shown at first: for a game its own one, else the one of its system, else the one in use. */
    private fun initialValue(
        context: Context,
        option: LemuroidCoreOption,
        scope: CoreOptionsScope,
    ): String {
        if (scope !is CoreOptionsScope.Game) return option.getCurrentValue()
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        val gameValue =
            when (val value = preferences.all[scope.preferenceKey(option.getKey())]) {
                is Boolean -> if (value) "enabled" else "disabled"
                is String -> value
                else -> null
            }
        return gameValue
            ?: CoreOptionsScope.systemValue(context, option.getKey(), scope.systemId)
            ?: option.getCurrentValue()
    }

    private fun buildListPreference(
        context: Context,
        it: LemuroidCoreOption,
        scope: CoreOptionsScope,
    ): ListPreference {
        val values = it.getEntriesValues()
        val value = initialValue(context, it, scope).takeIf { value -> value in values } ?: values.first()
        val preference = ListPreference(context)
        preference.key = scope.preferenceKey(it.getKey())
        preference.title = it.getDisplayName(context)
        preference.entries = it.getEntries(context).toTypedArray()
        preference.entryValues = values.toTypedArray()
        preference.setDefaultValue(value)
        preference.setValueIndex(values.indexOf(value))
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        preference.isIconSpaceReserved = false
        return preference
    }

    private fun buildSwitchPreference(
        context: Context,
        it: LemuroidCoreOption,
        scope: CoreOptionsScope,
    ): SwitchPreference {
        val enabled = initialValue(context, it, scope) == "enabled"
        val preference = SwitchPreference(context)
        preference.key = scope.preferenceKey(it.getKey())
        preference.title = it.getDisplayName(context)
        preference.setDefaultValue(enabled)
        preference.isChecked = enabled
        preference.summary = it.getDescription(context)
        preference.isIconSpaceReserved = false
        return preference
    }

    private fun buildControllerPreference(
        context: Context,
        systemID: String,
        coreID: CoreID,
        port: Int,
        controllerConfigs: List<ControllerConfig>,
    ): Preference {
        val preference = ListPreference(context)
        preference.key = ControllerConfigsManager.getSharedPreferencesId(systemID, coreID, port)
        preference.title = context.getString(R.string.core_settings_controller, (port + 1).toString())
        preference.entries = controllerConfigs.map { context.getString(it.displayName) }.toTypedArray()
        preference.entryValues = controllerConfigs.map { it.name }.toTypedArray()
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        preference.isIconSpaceReserved = false
        preference.setDefaultValue(controllerConfigs.map { it.name }.first())
        return preference
    }

    private fun createCategory(
        context: Context,
        preferenceScreen: PreferenceScreen,
        title: String,
    ): PreferenceCategory {
        val category = PreferenceCategory(context)
        preferenceScreen.addPreference(category)
        category.title = title
        category.isIconSpaceReserved = false
        return category
    }
}
