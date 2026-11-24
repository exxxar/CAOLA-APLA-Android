package ru.caola.apla.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LearningEvent::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun learningEventDao(): LearningEventDao
}
