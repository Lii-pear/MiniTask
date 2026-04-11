package com.example.minitask.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

// 1. 任务优先级枚举
enum class TaskPriority(
    val colorValue: Long,
    val weight: Int,
    val title: String,
    val shortName: String
) {
    LEVEL_1(0xFFFF5252, 1, "重要且紧急", "紧急"),
    LEVEL_2(0xFFFFAB40, 2, "重要不紧急", "重要"),
    LEVEL_3(0xFF40C4FF, 3, "紧急不重要", "琐碎"),
    LEVEL_4(0xFF69F0AE, 4, "不重要不紧急", "常规")
}

// 2. 每日任务表
@Entity(tableName = "tasks")
data class DailyTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: TaskPriority = TaskPriority.LEVEL_4,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val targetDate: LocalDate = LocalDate.now(),
    val orderWeight: Long = System.nanoTime()
)

// 3. 备忘录表
@Entity(tableName = "memos")
data class CalendarMemo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetDate: LocalDate = LocalDate.now(),
    val colorValue: Long = 0xFF40C4FF,
    val isCompleted: Boolean = false,
    val orderWeight: Long = System.nanoTime()
)

// 4. 每日必做（习惯）表
@Entity(tableName = "routines")
data class DailyRoutine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val routineType: String = "DAILY", // DAILY, INTERVAL, WEEKLY
    val repeatValue: String = "",
    val startDate: LocalDate = LocalDate.now(),
    // ★ 新增：用于控制习惯终止的日期
    val endDate: LocalDate? = null,
    val lastCompletedDate: LocalDate? = null,
    val orderWeight: Long = System.nanoTime()
)