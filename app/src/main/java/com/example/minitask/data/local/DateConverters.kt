package com.example.minitask.data.local

import androidx.room.TypeConverter
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.domain.routine.RoutineType
import java.time.LocalDate

class DateConverters {
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(name: String?): TaskPriority {
        return name?.let { runCatching { TaskPriority.valueOf(it) }.getOrNull() }
            ?: TaskPriority.LEVEL_4
    }

    @TypeConverter
    fun fromRoutineType(type: RoutineType): String = type.storageValue

    @TypeConverter
    fun toRoutineType(value: String?): RoutineType = RoutineType.fromStorage(value)
}
