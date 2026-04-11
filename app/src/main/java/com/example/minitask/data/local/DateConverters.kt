package com.example.minitask.data.local

import androidx.room.TypeConverter
import com.example.minitask.data.model.TaskPriority
import java.time.LocalDate

class DateConverters {
    // 处理日期：LocalDate <-> String (2024-05-20)
    @TypeConverter
    fun fromDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }

    // 处理优先级：TaskPriority <-> String (LEVEL_1)
    @TypeConverter
    fun fromPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toPriority(name: String): TaskPriority = TaskPriority.valueOf(name)
}