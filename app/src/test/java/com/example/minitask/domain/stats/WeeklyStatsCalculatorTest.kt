package com.example.minitask.domain.stats

import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import com.example.minitask.domain.routine.RoutineType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WeeklyStatsCalculatorTest {
    private val monday = LocalDate.of(2026, 5, 18)

    @Test
    fun weekDatesStartOnMonday() {
        val dates = WeeklyStatsCalculator.weekDatesFor(monday.plusDays(3))

        assertEquals(monday, dates.first())
        assertEquals(monday.plusDays(6), dates.last())
        assertEquals(7, dates.size)
    }

    @Test
    fun calculatesTaskAndRoutineCompletionRatio() {
        val routine = DailyRoutine(
            title = "Read",
            routineType = RoutineType.DAILY,
            startDate = monday,
            lastCompletedDate = monday
        )
        val tasksByDate = mapOf(
            monday to listOf(
                DailyTask(title = "Done", isCompleted = true, targetDate = monday),
                DailyTask(title = "Todo", isCompleted = false, targetDate = monday)
            )
        )

        val stats = WeeklyStatsCalculator.calculate(
            weekDates = WeeklyStatsCalculator.weekDatesFor(monday),
            tasksByDate = tasksByDate,
            routines = listOf(routine)
        )

        assertEquals(2f / 3f, stats[0], 0.0001f)
        assertEquals(0f, stats[1], 0.0001f)
    }
}
