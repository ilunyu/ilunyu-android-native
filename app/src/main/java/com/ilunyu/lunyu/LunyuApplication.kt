package com.ilunyu.lunyu

import android.app.Application
import com.ilunyu.lunyu.data.db.AppDatabase
import com.ilunyu.lunyu.data.repository.AnalectsRepository
import com.ilunyu.lunyu.data.repository.ExerciseRepository
import com.ilunyu.lunyu.data.repository.TagRepository
import com.ilunyu.lunyu.data.repository.UserPreferencesRepository
import com.ilunyu.lunyu.data.resource.ResourceManager
import com.ilunyu.lunyu.data.resource.ResourcePackageStorage
import com.ilunyu.lunyu.data.resource.ResourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LunyuApplication : Application() {
    lateinit var analectsRepository: AnalectsRepository
        private set
    lateinit var exerciseRepository: ExerciseRepository
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var tagRepository: TagRepository
        private set
    lateinit var resourceRepository: ResourceRepository
        private set
    lateinit var resourceManager: ResourceManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        tagRepository = TagRepository(database.tagDao())
        resourceRepository = ResourceRepository(this, database)
        val packageStorage = ResourcePackageStorage(this)
        resourceManager = ResourceManager(this, resourceRepository)
        analectsRepository = AnalectsRepository(packageStorage, resourceRepository)
        exerciseRepository = ExerciseRepository(packageStorage, resourceRepository)
        userPreferencesRepository = UserPreferencesRepository(this)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            tagRepository.migrateLegacyExerciseIds()
            userPreferencesRepository.migrateLegacyExerciseIds()
        }
    }
}
