package com.ilunyu.lunyu

import android.app.Application
import com.ilunyu.lunyu.data.repository.AnalectsRepository
import com.ilunyu.lunyu.data.repository.ExerciseRepository
import com.ilunyu.lunyu.data.repository.UserPreferencesRepository

class LunyuApplication : Application() {
    lateinit var analectsRepository: AnalectsRepository
        private set
    lateinit var exerciseRepository: ExerciseRepository
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        analectsRepository = AnalectsRepository(this)
        exerciseRepository = ExerciseRepository(this)
        userPreferencesRepository = UserPreferencesRepository(this)
    }
}
