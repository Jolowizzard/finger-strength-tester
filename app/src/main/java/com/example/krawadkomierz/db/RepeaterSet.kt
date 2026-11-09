package com.example.krawadkomierz.db
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "repeater_sets")
data class RepeaterSet(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "session_exercise_id")
    val sessionExerciseId: Long,

    @ColumnInfo(name = "target_force")
    val targetForce: Float,

    @ColumnInfo(name = "avg_force")
    val avgForce: Float,

    @ColumnInfo(name = "work_seconds")
    val workSeconds: Int,

    @ColumnInfo(name = "rest_seconds")
    val restSeconds: Int,

    @ColumnInfo(name = "repetitions")
    val repetitions: Int
)