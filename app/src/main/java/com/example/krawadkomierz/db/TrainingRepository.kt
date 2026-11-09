package com.example.krawadkomierz.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrainingRepository (private val dao: TrainingDAO){
    val maxForcePoints: Flow<List<MaxForcePoint>> = dao.getMaxForcePointsOverTime()

    suspend fun saveMaxForceTraining(name: String, notes: String?, maxForceValue: Float) = withContext(Dispatchers.IO) {
        dao.insertFullMaxForceLog(name, notes, maxForceValue)
    }

    suspend fun deleteTrainingSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteMaxForceLogsForSession(sessionId)
        dao.deleteRepeaterSetsForSession(sessionId)
        dao.deleteSessionExercisesForSession(sessionId)
        dao.deleteTrainingSessionRecord(sessionId)
    }
}