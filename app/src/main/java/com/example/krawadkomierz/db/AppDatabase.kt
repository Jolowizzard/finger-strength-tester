package com.example.krawadkomierz.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrainingSession::class,
        SessionExercise::class,
        RepeaterSet::class,
        MaxForceLog::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trainingDao(): TrainingDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "krawadkomierz_database"
                )
                    .addCallback(AppDatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Uruchamiamy wstawianie danych testowych w tle tylko przy PIERWSZYM utworzeniu pliku bazy
            CoroutineScope(Dispatchers.IO).launch {
                populateDatabase(getDatabase(context).trainingDao())
            }
        }

        private suspend fun populateDatabase(dao: TrainingDAO) {
            val now = System.currentTimeMillis()
            val dayMs = 24 * 60 * 60 * 1000L
            val hourMs = 60 * 60 * 1000L

            // Przykładowy zestaw pomiarów rozłożonych w czasie (ostatni tydzień)
            val sampleData = listOf(
                Triple("Rozgrzewka", 25.0f, now - 7 * dayMs),
                Triple("Max Force Test #1", 30.5f, now - 5 * dayMs),
                Triple("Max Force Test #2", 33.2f, now - 3 * dayMs),
                Triple("Mocny trening", 32.0f, now - 2 * dayMs),
                Triple("Nowy rekord!", 36.8f, now - 1 * dayMs),
                Triple("Test w ten sam dzień", 28.0f, now - 7 * dayMs + 2 * hourMs)
            )

            for ((name, force, timestamp) in sampleData) {
                val sessionId = dao.insertTrainingSession(
                    TrainingSession(name = name, date = timestamp, notes = "Wpis testowy")
                )
                val exerciseId = dao.insertSessionExercise(
                    SessionExercise(trainingSessionId = sessionId, exerciseType = "max_force")
                )
                dao.insertMaxForceLog(
                    MaxForceLog(sessionExerciseId = exerciseId, maxForce = force)
                )
            }
        }
    }
}