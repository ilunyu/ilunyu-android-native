package com.ilunyu.lunyu.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
        preferences[PreferencesKeys.FAVORITE_EXERCISES] ?: emptySet()
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
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITE_EXERCISES]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(exerciseId)) {
                current.remove(exerciseId)
            } else {
                current.add(exerciseId)
            }
            preferences[PreferencesKeys.FAVORITE_EXERCISES] = current
        }
    }
}
