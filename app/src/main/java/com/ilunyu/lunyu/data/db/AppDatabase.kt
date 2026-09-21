package com.ilunyu.lunyu.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TagEntity::class,
        ItemTagCrossRef::class,
        InstalledResourceEntity::class,
        ResourceActivationEntity::class,
        ResourcePreferenceEntity::class,
        ChapterExerciseCrossRefEntity::class,
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tagDao(): TagDao
    abstract fun resourceDao(): ResourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lunyu_knowledge.db"
                ).addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS installed_resources (
                        package_id TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        name TEXT NOT NULL,
                        version_code INTEGER NOT NULL,
                        version_name TEXT NOT NULL,
                        location_type TEXT NOT NULL,
                        root_path TEXT NOT NULL,
                        sha256 TEXT NOT NULL,
                        installed_at INTEGER NOT NULL,
                        PRIMARY KEY(package_id, version_code, location_type)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_installed_resources_kind ON installed_resources(kind)")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS resource_activations (
                        package_id TEXT NOT NULL PRIMARY KEY,
                        active_version_code INTEGER NOT NULL,
                        active_location_type TEXT NOT NULL,
                        enabled INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS resource_preferences (
                        id INTEGER NOT NULL PRIMARY KEY,
                        active_edition_package_id TEXT NOT NULL,
                        content_generation INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chapter_exercise_cross_ref (
                        chapter_id TEXT NOT NULL,
                        exercise_package_id TEXT NOT NULL,
                        exercise_id TEXT NOT NULL,
                        PRIMARY KEY(chapter_id, exercise_package_id, exercise_id)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chapter_exercise_cross_ref_chapter_id " +
                        "ON chapter_exercise_cross_ref(chapter_id)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chapter_exercise_cross_ref_exercise_id " +
                        "ON chapter_exercise_cross_ref(exercise_id)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE installed_resources ADD COLUMN origin_url TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
