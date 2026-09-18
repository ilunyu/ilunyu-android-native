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
import com.ilunyu.lunyu.data.db.TagEntity
import com.ilunyu.lunyu.data.db.TagWithCounts
import com.ilunyu.lunyu.data.db.TargetType
import com.ilunyu.lunyu.ui.favorites.FavoritesSortMode
import kotlinx.coroutines.flow.Flow
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
    private val tagRepo = app.tagRepository

    val tagsWithCounts: StateFlow<List<TagWithCounts>> = tagRepo.allTagsWithCountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTagsForItem(targetType: String, targetId: String): Flow<List<TagEntity>> {
        return tagRepo.getTagsForItemFlow(targetType, targetId)
    }

    fun getChapterIdsForTag(tagId: String): Flow<List<String>> {
        return tagRepo.getChapterIdsForTagFlow(tagId)
    }

    fun getExerciseIdsForTag(tagId: String): Flow<List<String>> {
        return tagRepo.getExerciseIdsForTagFlow(tagId)
    }

    suspend fun getTagById(tagId: String): TagEntity? {
        return tagRepo.getTagById(tagId)
    }

    fun createTag(name: String, colorHex: String? = null, onResult: (Result<TagEntity>) -> Unit = {}) {
        viewModelScope.launch {
            val result = tagRepo.createTag(name, colorHex)
            onResult(result)
        }
    }

    fun updateTag(tagId: String, name: String, colorHex: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = tagRepo.updateTag(tagId, name, colorHex)
            onResult(result)
        }
    }

    fun deleteTag(tagId: String) {
        viewModelScope.launch {
            tagRepo.deleteTag(tagId)
        }
    }

    fun toggleItemTag(tagId: String, targetType: String, targetId: String) {
        viewModelScope.launch {
            tagRepo.toggleItemTag(tagId, targetType, targetId)
        }
    }

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

    // 1. ReadingScreen state
    private val _pianScrollMap = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val pianScrollMap: StateFlow<Map<String, Pair<Int, Int>>> = _pianScrollMap.asStateFlow()
    private val _catalogScrollIndex = MutableStateFlow(0)
    val catalogScrollIndex: StateFlow<Int> = _catalogScrollIndex.asStateFlow()
    private val _catalogScrollOffset = MutableStateFlow(0)
    val catalogScrollOffset: StateFlow<Int> = _catalogScrollOffset.asStateFlow()
    private val _activePianSlug = MutableStateFlow<String?>(null)
    val activePianSlug: StateFlow<String?> = _activePianSlug.asStateFlow()

    // 2. StudyScreen state
    private val _studyYears = MutableStateFlow<Set<String>>(emptySet())
    val studyYears: StateFlow<Set<String>> = _studyYears.asStateFlow()
    private val _studySources = MutableStateFlow<Set<String>>(emptySet())
    val studySources: StateFlow<Set<String>> = _studySources.asStateFlow()
    private val _studyGrades = MutableStateFlow<Set<Int>>(emptySet())
    val studyGrades: StateFlow<Set<Int>> = _studyGrades.asStateFlow()
    private val _studyTypes = MutableStateFlow<Set<String>>(emptySet())
    val studyTypes: StateFlow<Set<String>> = _studyTypes.asStateFlow()
    private val _studyScrollIndex = MutableStateFlow(0)
    val studyScrollIndex: StateFlow<Int> = _studyScrollIndex.asStateFlow()
    private val _studyScrollOffset = MutableStateFlow(0)
    val studyScrollOffset: StateFlow<Int> = _studyScrollOffset.asStateFlow()

    // 3. FavoritesScreen state
    private val _favoritesTab = MutableStateFlow(0)
    val favoritesTab: StateFlow<Int> = _favoritesTab.asStateFlow()
    private val _favoritesSortMode = MutableStateFlow(FavoritesSortMode.DEFAULT)
    val favoritesSortMode: StateFlow<FavoritesSortMode> = _favoritesSortMode.asStateFlow()
    private val _favoritesYears = MutableStateFlow<Set<String>>(emptySet())
    val favoritesYears: StateFlow<Set<String>> = _favoritesYears.asStateFlow()
    private val _favoritesSources = MutableStateFlow<Set<String>>(emptySet())
    val favoritesSources: StateFlow<Set<String>> = _favoritesSources.asStateFlow()
    private val _favoritesGrades = MutableStateFlow<Set<Int>>(emptySet())
    val favoritesGrades: StateFlow<Set<Int>> = _favoritesGrades.asStateFlow()
    private val _favoritesTypes = MutableStateFlow<Set<String>>(emptySet())
    val favoritesTypes: StateFlow<Set<String>> = _favoritesTypes.asStateFlow()
    private val _favoritesChapterScrollIndex = MutableStateFlow(0)
    val favoritesChapterScrollIndex: StateFlow<Int> = _favoritesChapterScrollIndex.asStateFlow()
    private val _favoritesChapterScrollOffset = MutableStateFlow(0)
    val favoritesChapterScrollOffset: StateFlow<Int> = _favoritesChapterScrollOffset.asStateFlow()
    private val _favoritesExerciseScrollIndex = MutableStateFlow(0)
    val favoritesExerciseScrollIndex: StateFlow<Int> = _favoritesExerciseScrollIndex.asStateFlow()
    private val _favoritesExerciseScrollOffset = MutableStateFlow(0)
    val favoritesExerciseScrollOffset: StateFlow<Int> = _favoritesExerciseScrollOffset.asStateFlow()

    // 4. Detail screens scroll state
    private val _chapterDetailScrollMap = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val chapterDetailScrollMap: StateFlow<Map<String, Pair<Int, Int>>> = _chapterDetailScrollMap.asStateFlow()
    private val _exerciseDetailScrollMap = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val exerciseDetailScrollMap: StateFlow<Map<String, Pair<Int, Int>>> = _exerciseDetailScrollMap.asStateFlow()

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
        if (_searchQuery.value != query) {
            _searchQuery.value = query
            _chapterScrollIndex.value = 0
            _chapterScrollOffset.value = 0
            _exerciseScrollIndex.value = 0
            _exerciseScrollOffset.value = 0
        }
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

    // ReadingScreen
    fun savePianScroll(slug: String, index: Int, offset: Int) {
        val prev = _pianScrollMap.value[slug]
        if (prev?.first != index || prev?.second != offset) {
            _pianScrollMap.value = _pianScrollMap.value + (slug to (index to offset))
        }
    }

    fun saveCatalogScroll(index: Int, offset: Int) {
        _catalogScrollIndex.value = index
        _catalogScrollOffset.value = offset
    }

    fun setActivePianSlug(slug: String?) {
        _activePianSlug.value = slug
    }

    // StudyScreen
    fun setStudyFilters(years: Set<String>, sources: Set<String>, grades: Set<Int>, types: Set<String>) {
        if (_studyYears.value != years || _studySources.value != sources || _studyGrades.value != grades || _studyTypes.value != types) {
            _studyYears.value = years
            _studySources.value = sources
            _studyGrades.value = grades
            _studyTypes.value = types
            _studyScrollIndex.value = 0
            _studyScrollOffset.value = 0
        }
    }

    fun saveStudyScroll(index: Int, offset: Int) {
        _studyScrollIndex.value = index
        _studyScrollOffset.value = offset
    }

    // FavoritesScreen
    fun setFavoritesTab(tab: Int) {
        _favoritesTab.value = tab
    }

    fun setFavoritesSortMode(mode: FavoritesSortMode) {
        if (_favoritesSortMode.value != mode) {
            _favoritesSortMode.value = mode
            _favoritesChapterScrollIndex.value = 0
            _favoritesChapterScrollOffset.value = 0
            _favoritesExerciseScrollIndex.value = 0
            _favoritesExerciseScrollOffset.value = 0
        }
    }

    fun setFavoritesExerciseFilters(years: Set<String>, sources: Set<String>, grades: Set<Int>, types: Set<String>) {
        if (_favoritesYears.value != years || _favoritesSources.value != sources || _favoritesGrades.value != grades || _favoritesTypes.value != types) {
            _favoritesYears.value = years
            _favoritesSources.value = sources
            _favoritesGrades.value = grades
            _favoritesTypes.value = types
            _favoritesExerciseScrollIndex.value = 0
            _favoritesExerciseScrollOffset.value = 0
        }
    }

    fun saveFavoritesScroll(isExercise: Boolean, index: Int, offset: Int) {
        if (isExercise) {
            _favoritesExerciseScrollIndex.value = index
            _favoritesExerciseScrollOffset.value = offset
        } else {
            _favoritesChapterScrollIndex.value = index
            _favoritesChapterScrollOffset.value = offset
        }
    }

    // Detail screens
    fun saveChapterDetailScroll(chapterId: String, index: Int, offset: Int) {
        val prev = _chapterDetailScrollMap.value[chapterId]
        if (prev?.first != index || prev?.second != offset) {
            _chapterDetailScrollMap.value = _chapterDetailScrollMap.value + (chapterId to (index to offset))
        }
    }

    fun saveExerciseDetailScroll(exerciseId: String, index: Int, offset: Int) {
        val prev = _exerciseDetailScrollMap.value[exerciseId]
        if (prev?.first != index || prev?.second != offset) {
            _exerciseDetailScrollMap.value = _exerciseDetailScrollMap.value + (exerciseId to (index to offset))
        }
    }
}

