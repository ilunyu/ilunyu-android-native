package com.ilunyu.lunyu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ilunyu.lunyu.LunyuApplication
import com.ilunyu.lunyu.data.model.AnalectsLibrary
import com.ilunyu.lunyu.data.model.AppFontPreference
import com.ilunyu.lunyu.data.model.AppThemeMode
import com.ilunyu.lunyu.data.model.Chapter
import com.ilunyu.lunyu.data.model.Exercise
import com.ilunyu.lunyu.data.model.Pian
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LunyuApplication
    private val userPrefs = app.userPreferencesRepository
    private val analectsRepo = app.analectsRepository
    private val exerciseRepo = app.exerciseRepository

    val themeMode: StateFlow<AppThemeMode> = userPrefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppThemeMode.SYSTEM)

    val fontPreference: StateFlow<AppFontPreference> = userPrefs.fontPreferenceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppFontPreference.SANS)

    val favoriteChapters: StateFlow<Set<String>> = userPrefs.favoriteChaptersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val favoriteExercises: StateFlow<Set<String>> = userPrefs.favoriteExercisesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _library = MutableStateFlow<AnalectsLibrary?>(null)
    val library: StateFlow<AnalectsLibrary?> = _library.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    init {
        viewModelScope.launch {
            _library.value = analectsRepo.getLibrary()
        }
        viewModelScope.launch {
            _exercises.value = exerciseRepo.getExerciseList()
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            userPrefs.setThemeMode(mode)
        }
    }

    fun setFontPreference(pref: AppFontPreference) {
        viewModelScope.launch {
            userPrefs.setFontPreference(pref)
        }
    }

    fun toggleChapterFavorite(id: String) {
        viewModelScope.launch {
            userPrefs.toggleChapterFavorite(id)
        }
    }

    fun toggleExerciseFavorite(id: String) {
        viewModelScope.launch {
            userPrefs.toggleExerciseFavorite(id)
        }
    }

    suspend fun getChapterDetail(pianSlug: String, chapterNumber: Int): Pair<Pian, Chapter>? {
        return analectsRepo.getChapter(pianSlug, chapterNumber)
    }

    suspend fun getChapterById(chapterId: String): Pair<Pian, Chapter>? {
        return analectsRepo.getChapterById(chapterId)
    }

    suspend fun getAdjacentChapters(pianSlug: String, chapterNumber: Int): Pair<Pair<Pian, Chapter>?, Pair<Pian, Chapter>?> {
        return analectsRepo.getAdjacentChapters(pianSlug, chapterNumber)
    }

    suspend fun getExerciseDetail(id: String): Exercise? {
        return exerciseRepo.getExerciseDetail(id)
    }
}

