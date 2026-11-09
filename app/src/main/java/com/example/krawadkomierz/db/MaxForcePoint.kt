package com.example.krawadkomierz.db
import androidx.room.ColumnInfo

data class MaxForcePoint(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "max_force") val maxForce: Float,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "notes") val notes: String?
)
