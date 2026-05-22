package com.example.minitask.data.model

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.minitask.domain.routine.RoutineType
import java.time.LocalDate
import java.util.UUID

@Immutable
enum class TaskPriority(
    val colorValue: Long,
    val weight: Int,
    val title: String,
    val shortName: String
) {
    LEVEL_1(0xFFE53935, 1, "重要且紧急", "紧急"),
    LEVEL_2(0xFFFFA726, 2, "重要不紧急", "重要"),
    LEVEL_3(0xFF29B6F6, 3, "紧急不重要", "琐事"),
    LEVEL_4(0xFF66BB6A, 4, "不重要不紧急", "常规")
}

@Immutable
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["targetDate"]),
        Index(value = ["targetDate", "isPinned", "isCompleted", "priority", "orderWeight"])
    ]
)
data class DailyTask(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val priority: TaskPriority = TaskPriority.LEVEL_4,
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val targetDate: LocalDate = LocalDate.now(),
    val orderWeight: Long = System.nanoTime()
)

@Immutable
@Entity(
    tableName = "memos",
    indices = [
        Index(value = ["targetDate"]),
        Index(value = ["isCompleted"])
    ]
)
data class CalendarMemo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val targetDate: LocalDate = LocalDate.now(),
    val colorValue: Long = 0xFF29B6F6,
    val isCompleted: Boolean = false,
    val orderWeight: Long = System.nanoTime()
)

@Immutable
@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["orderWeight"]),
        Index(value = ["startDate", "endDate"])
    ]
)
data class DailyRoutine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val routineType: RoutineType = RoutineType.DAILY,
    val repeatValue: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val lastCompletedDate: LocalDate? = null,
    val orderWeight: Long = System.nanoTime()
)
