package com.example.minitask.domain.stats

import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import com.example.minitask.domain.routine.RoutineRules
import java.time.LocalDate

object WeeklyStatsCalculator {
    fun weekDatesFor(anchorDate: LocalDate): List<LocalDate> {
        val monday = anchorDate.minusDays(anchorDate.dayOfWeek.value.toLong() - 1)
        return (0..6).map { offset -> monday.plusDays(offset.toLong()) }
    }

    fun calculate(
        weekDates: List<LocalDate>,
        tasksByDate: Map<LocalDate, List<DailyTask>>,
        routines: List<DailyRoutine>
    ): List<Float> {
        return weekDates.map { date ->
            val tasks = tasksByDate[date].orEmpty()
            val activeRoutines = routines.filter { RoutineRules.isActiveOnDate(it, date) }
            val totalItems = tasks.size + activeRoutines.size
            if (totalItems == 0) {
                0f
            } else {
                val completedTasks = tasks.count { it.isCompleted }
                val completedRoutines = activeRoutines.count { it.lastCompletedDate == date }
                (completedTasks + completedRoutines).toFloat() / totalItems
            }
        }
    }
}
