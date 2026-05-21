package com.example.minitask.domain.routine

import com.example.minitask.data.model.DailyRoutine
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object RoutineRules {
    fun isActiveOnDate(routine: DailyRoutine, date: LocalDate): Boolean {
        if (date.isBefore(routine.startDate)) return false
        if (routine.endDate != null && date.isAfter(routine.endDate)) return false

        return when (RoutineType.fromStorage(routine.routineType)) {
            RoutineType.DAILY -> true
            RoutineType.INTERVAL -> {
                val daysBetween = ChronoUnit.DAYS.between(routine.startDate, date)
                val interval = (routine.repeatValue.toLongOrNull() ?: 1L).coerceAtLeast(1L)
                daysBetween % interval == 0L
            }

            RoutineType.WEEKLY -> {
                val selectedDays = routine.repeatValue
                    .split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it in 1..7 }
                    .toSet()
                date.dayOfWeek.value in selectedDays
            }
        }
    }

    fun shouldDeleteImmediately(routine: DailyRoutine, selectedDate: LocalDate): Boolean {
        return !selectedDate.isAfter(routine.startDate)
    }

    fun endDateBefore(selectedDate: LocalDate): LocalDate {
        return selectedDate.minusDays(1)
    }
}
