package com.example.minitask.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask

@Database(
    entities = [DailyTask::class, CalendarMemo::class, DailyRoutine::class],
    version = 4, // ★ 升级版本号到 4：触发重构，加载 endDate 字段
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minitask_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}