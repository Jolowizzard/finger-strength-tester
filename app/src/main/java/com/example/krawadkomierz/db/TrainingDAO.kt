package com.example.krawadkomierz.db
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingDAO {
    @Insert
    suspend fun insertTrainingSession(session: TrainingSession): Long

    @Insert
    suspend fun insertSessionExercise(exercise: SessionExercise): Long

    @Insert
    suspend fun insertRepeaterSet(set: RepeaterSet): Long

    @Insert
    suspend fun insertMaxForceLog(log: MaxForceLog): Long

    @Transaction
    suspend fun insertFullMaxForceLog(
        sessionName: String,
        notes: String?,
        maxForceValue: Float
    ){
        val sessionId = insertTrainingSession(TrainingSession(name = sessionName, date = System.currentTimeMillis(), notes = notes))
        val exerciseId = insertSessionExercise(SessionExercise(trainingSessionId = sessionId, exerciseType = "max_force"))
        insertMaxForceLog(MaxForceLog(sessionExerciseId = exerciseId, maxForce = maxForceValue))
    }

    @Transaction
    suspend fun insertFullRepeaterSet(
        sessionName: String,
        notes: String?,
        targetForce: Float,
        avgForce: Float,
        workSeconds: Int,
        restSeconds: Int,
        repetitions: Int
    ){
        val sessionId = insertTrainingSession(TrainingSession(name = sessionName, date = System.currentTimeMillis(), notes = notes))
        val exerciseId = insertSessionExercise(SessionExercise(trainingSessionId = sessionId, exerciseType = "repeater"))
        insertRepeaterSet(RepeaterSet(
            sessionExerciseId = exerciseId,
            targetForce = targetForce,
            avgForce = avgForce,
            workSeconds = workSeconds,
            restSeconds = restSeconds,
            repetitions = repetitions
        ))
    }

    @Query("""
        SELECT 
            ts.id AS session_id,
            ts.date AS date,
            mfl.max_force AS max_force,
            ts.name AS name,
            ts.notes AS notes
        FROM max_force_logs mfl
        INNER JOIN session_exercises se ON mfl.session_exercise_id = se.id
        INNER JOIN training_sessions ts ON se.training_session_id = ts.id
        ORDER BY ts.date ASC
    """)
    fun getMaxForcePointsOverTime(): Flow<List<MaxForcePoint>>

    @Query("DELETE FROM max_force_logs WHERE session_exercise_id IN (SELECT id FROM session_exercises WHERE training_session_id = :sessionId)")
    suspend fun deleteMaxForceLogsForSession(sessionId: Long)

    @Query("DELETE FROM repeater_sets WHERE session_exercise_id IN (SELECT id FROM session_exercises WHERE training_session_id = :sessionId)")
    suspend fun deleteRepeaterSetsForSession(sessionId: Long)

    @Query("DELETE FROM session_exercises WHERE training_session_id = :sessionId")
    suspend fun deleteSessionExercisesForSession(sessionId: Long)

    @Query("DELETE FROM training_sessions WHERE id = :sessionId")
    suspend fun deleteTrainingSessionRecord(sessionId: Long)
}