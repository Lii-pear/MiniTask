package com.example.minitask.domain.calendar

import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarGridRulesTest {
    private val monday = LocalDate.of(2026, 5, 18)

    @Test
    fun monthModeBuildsStableSixWeekGrid() {
        val month = YearMonth.of(2026, 5)
        val cells = CalendarGridRules.buildCells(
            mode = CalendarDisplayMode.MONTH,
            selectedDate = monday,
            visibleMonth = month,
            markers = emptyMap(),
            today = monday
        )

        assertEquals(42, cells.size)
        assertEquals(LocalDate.of(2026, 4, 27), cells.first().date)
        assertEquals(LocalDate.of(2026, 6, 7), cells.last().date)
        assertFalse(cells.first().isCurrentMonth)
        assertTrue(cells[21].isCurrentMonth)
    }

    @Test
    fun weekModeStartsOnMonday() {
        val cells = CalendarGridRules.buildCells(
            mode = CalendarDisplayMode.WEEK,
            selectedDate = monday.plusDays(3),
            visibleMonth = YearMonth.from(monday),
            markers = emptyMap(),
            today = monday
        )

        assertEquals(7, cells.size)
        assertEquals(monday, cells.first().date)
        assertEquals(monday.plusDays(6), cells.last().date)
    }

    @Test
    fun markersCombineTaskCountsAndVisibleMemoColors() {
        val memoColor = 0xFF123456
        val markers = CalendarGridRules.buildMarkers(
            tasks = listOf(
                DailyTask(title = "A", targetDate = monday),
                DailyTask(title = "B", targetDate = monday)
            ),
            memos = listOf(
                CalendarMemo(id = "memo-1", title = "M1", targetDate = monday),
                CalendarMemo(id = "memo-2", title = "M2", targetDate = monday.plusDays(1), isCompleted = true)
            ),
            memoColorsById = mapOf("memo-1" to memoColor)
        )

        assertEquals(2, markers.getValue(monday).taskCount)
        assertEquals(1, markers.getValue(monday).memoCount)
        assertEquals(listOf(memoColor), markers.getValue(monday).memoColors)
        assertFalse(markers.containsKey(monday.plusDays(1)))
    }
}
