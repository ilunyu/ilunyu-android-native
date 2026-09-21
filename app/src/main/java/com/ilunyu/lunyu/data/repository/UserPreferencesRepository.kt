package com.ilunyu.lunyu.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ilunyu.lunyu.data.model.AppFontPreference
import com.ilunyu.lunyu.data.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_PREFERENCE = stringPreferencesKey("font_preference")
        val FAVORITE_CHAPTERS = stringSetPreferencesKey("favorite_chapters")
        val FAVORITE_EXERCISES = stringSetPreferencesKey("favorite_exercises")
        val DEFAULT_ANSWER_EXPANDED = booleanPreferencesKey("default_answer_expanded")
    }

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        AppThemeMode.fromKey(preferences[PreferencesKeys.THEME_MODE])
    }

    val fontPreferenceFlow: Flow<AppFontPreference> = context.dataStore.data.map { preferences ->
        AppFontPreference.fromKey(preferences[PreferencesKeys.FONT_PREFERENCE])
    }

    val favoriteChaptersFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FAVORITE_CHAPTERS] ?: emptySet()
    }

    val favoriteExercisesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.FAVORITE_EXERCISES] ?: emptySet()
        raw.map { migrateExerciseId(it) }.toSet()
    }

    companion object {
        val LEGACY_EXERCISE_ID_MAP = mapOf(
            "202301-cpgsem" to "202301-cpgsqm",
            "202301-cygsem" to "202301-cygsqm",
            "202301-dxgsem" to "202212-dxgsqm",
            "202301-fsgsem" to "202301-fsgsqm",
            "202301-ftgsem" to "202301-ftgsqm",
            "202301-hdgsem" to "202301-hdgsqm",
            "202301-sjsgsem" to "202301-sjsgsqm",
            "202505-dcem" to "202505-dcgsem",
            "202511-cygsq" to "202511-cygsqz",
            "202604-cpym" to "202604-cpgsym",
            "202604-dcym" to "202604-dcgsym",
            "202604-fsym" to "202604-fsgsym",
            "202604-ftym" to "202604-ftgsym",
            "202604-mtgym" to "202604-mtggsym",
            "202604-pgym" to "202604-pggsym",
            "202604-sjsym" to "202604-sjsgsym",
        )

        fun migrateExerciseId(id: String): String = LEGACY_EXERCISE_ID_MAP[id] ?: id
    }

    suspend fun migrateLegacyExerciseIds() {
        context.dataStore.edit { preferences ->
            val raw = preferences[PreferencesKeys.FAVORITE_EXERCISES] ?: return@edit
            val migrated = raw.map { migrateExerciseId(it) }.toSet()
            if (migrated != raw) {
                preferences[PreferencesKeys.FAVORITE_EXERCISES] = migrated
            }
        }
    }

    val defaultAnswerExpandedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_ANSWER_EXPANDED] ?: true
    }

    suspend fun setDefaultAnswerExpanded(expanded: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ANSWER_EXPANDED] = expanded
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.key
        }
    }

    suspend fun setFontPreference(font: AppFontPreference) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_PREFERENCE] = font.key
        }
    }

    suspend fun toggleChapterFavorite(chapterId: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITE_CHAPTERS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(chapterId)) {
                current.remove(chapterId)
            } else {
                current.add(chapterId)
            }
            preferences[PreferencesKeys.FAVORITE_CHAPTERS] = current
        }
    }

    suspend fun toggleExerciseFavorite(exerciseId: String) {
        val canonicalId = migrateExerciseId(exerciseId)
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITE_EXERCISES]
                ?.map { migrateExerciseId(it) }
                ?.toMutableSet() ?: mutableSetOf()
            if (current.contains(canonicalId)) {
                current.remove(canonicalId)
            } else {
                current.add(canonicalId)
            }
            preferences[PreferencesKeys.FAVORITE_EXERCISES] = current
        }
    }
}
