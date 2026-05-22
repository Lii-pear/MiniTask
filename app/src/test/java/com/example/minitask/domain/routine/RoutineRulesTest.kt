package com.example.minitask.domain.routine

import com.example.minitask.data.model.DailyRoutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoutineRulesTest {
    private val monday = LocalDate.of(2026, 5, 18)

    @Test
    fun dailyRoutineRespectsStartAndEndDates() {
        val routine = DailyRoutine(
            title = "Read",
            routineType = RoutineType.DAILY,
            startDate = monday,
            endDate = monday.plusDays(2)
        )

        assertFalse(RoutineRules.isActiveOnDate(routine, monday.minusDays(1)))
        assertTrue(RoutineRules.isActiveOnDate(routine, monday))
        assertTrue(RoutineRules.isActiveOnDate(routine, monday.plusDays(2)))
        assertFalse(RoutineRules.isActiveOnDate(routine, monday.plusDays(3)))
    }

    @Test
    fun intervalRoutineUsesPositiveRepeatValue() {
        val routine = DailyRoutine(
            title = "Workout",
            routineType = RoutineType.INTERVAL,
            repeatValue = "2",
            startDate = monday
        )

        assertTrue(RoutineRules.isActiveOnDate(routine, monday))
        assertFalse(RoutineRules.isActiveOnDate(routine, monday.plusDays(1)))
        assertTrue(RoutineRules.isActiveOnDate(routine, monday.plusDays(2)))
    }

    @Test
    fun weeklyRoutineUsesSelectedWeekdaysOnly() {
        val routine = DailyRoutine(
            title = "Piano",
            routineType = RoutineType.WEEKLY,
            repeatValue = "1,3,5",
            startDate = monday
        )

        assertTrue(RoutineRules.isActiveOnDate(routine, monday))
        assertFalse(RoutineRules.isActiveOnDate(routine, monday.plusDays(1)))
        assertTrue(RoutineRules.isActiveOnDate(routine, monday.plusDays(2)))
    }

    @Test
    fun routineDeleteBoundaryMatchesSoftDeletePolicy() {
        val routine = DailyRoutine(
            title = "Stretch",
            startDate = monday
        )

        assertTrue(RoutineRules.shouldDeleteImmediately(routine, monday))
        assertFalse(RoutineRules.shouldDeleteImmediately(routine, monday.plusDays(1)))
        assertEquals(monday, RoutineRules.endDateBefore(monday.plusDays(1)))
    }

    @Test
    fun scheduleInputSerializesToStorageShape() {
        val weeklyInput = RoutineScheduleInput.weekly(listOf(3, 1, 9, 3))

        assertEquals(RoutineType.WEEKLY, weeklyInput.storageType)
        assertEquals("1,3", weeklyInput.storageRepeatValue)
        assertEquals("3", RoutineScheduleInput.interval(3).storageRepeatValue)
        assertEquals("", RoutineScheduleInput.daily().storageRepeatValue)
    }
}
