package ru.caola.apla.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_events")
data class LearningEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
