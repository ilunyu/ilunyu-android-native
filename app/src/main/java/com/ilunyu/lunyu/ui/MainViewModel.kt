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

    val defaultAnswerExpanded: StateFlow<Boolean> = userPrefs.defaultAnswerExpandedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _library = MutableStateFlow<AnalectsLibrary?>(null)
    val library: StateFlow<AnalectsLibrary?> = _library.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private val _searchExercises = MutableStateFlow<List<Exercise>>(emptyList())
    val searchExercises: StateFlow<List<Exercise>> = _searchExercises.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTab = MutableStateFlow(0)
    val searchTab: StateFlow<Int> = _searchTab.asStateFlow()

    private val _chapterScrollIndex = MutableStateFlow(0)
    val chapterScrollIndex: StateFlow<Int> = _chapterScrollIndex.asStateFlow()
    private val _chapterScrollOffset = MutableStateFlow(0)
    val chapterScrollOffset: StateFlow<Int> = _chapterScrollOffset.asStateFlow()

    private val _exerciseScrollIndex = MutableStateFlow(0)
    val exerciseScrollIndex: StateFlow<Int> = _exerciseScrollIndex.asStateFlow()
    private val _exerciseScrollOffset = MutableStateFlow(0)
    val exerciseScrollOffset: StateFlow<Int> = _exerciseScrollOffset.asStateFlow()

    init {
        viewModelScope.launch {
            _library.value = analectsRepo.getLibrary()
        }
        viewModelScope.launch {
            _exercises.value = exerciseRepo.getExerciseList()
        }
        viewModelScope.launch {
            _searchExercises.value = exerciseRepo.getSearchExercises()
        }
    }

    fun setDefaultAnswerExpanded(expanded: Boolean) {
        viewModelScope.launch {
            userPrefs.setDefaultAnswerExpanded(expanded)
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchTab(tab: Int) {
        _searchTab.value = tab
    }

    fun saveSearchScroll(isExercise: Boolean, index: Int, offset: Int) {
        if (isExercise) {
            _exerciseScrollIndex.value = index
            _exerciseScrollOffset.value = offset
        } else {
            _chapterScrollIndex.value = index
            _chapterScrollOffset.value = offset
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _chapterScrollIndex.value = 0
        _chapterScrollOffset.value = 0
        _exerciseScrollIndex.value = 0
        _exerciseScrollOffset.value = 0
    }
}

