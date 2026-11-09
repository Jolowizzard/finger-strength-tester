package com.example.krawadkomierz.db
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "session_exercises")
data class SessionExercise(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "training_session_id")
    val trainingSessionId: Long,
    
    @ColumnInfo(name = "exercise_type")
    val exerciseType: String
)
