package com.example.krawadkomierz.db

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel (private val repository: TrainingRepository) : ViewModel() {

    val maxForcePoints: Flow<List<MaxForcePoint>> = repository.maxForcePoints
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500),
            initialValue = emptyList()
        )

    fun saveMaxForceTraining(name: String, notes: String?, maxForceValue: Float) {
        viewModelScope.launch {
            repository.saveMaxForceTraining(name, notes, maxForceValue)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteTrainingSession(sessionId)
        }
    }
}