package ru.caola.apla.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LearningEventDao {
    @Insert suspend fun insert(event: LearningEvent): Long
    @Query("SELECT * FROM learning_events ORDER BY createdAt DESC") suspend fun all(): List<LearningEvent>
    @Query("SELECT * FROM learning_events WHERE synced = 0 ORDER BY createdAt") suspend fun pending(): List<LearningEvent>
}
