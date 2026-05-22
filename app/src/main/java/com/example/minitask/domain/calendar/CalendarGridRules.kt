package com.example.minitask.domain.calendar

import androidx.compose.runtime.Immutable
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyTask
import java.time.LocalDate
import java.time.YearMonth

enum class CalendarDisplayMode {
    WEEK,
    MONTH
}

@Immutable
data class CalendarDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate
)

@Immutable
data class CalendarDayMarker(
    val taskCount: Int = 0,
    val memoCount: Int = 0,
    val memoColors: List<Long> = emptyList()
) {
    val hasContent: Boolean = taskCount > 0 || memoCount > 0
}

@Immutable
data class CalendarDayCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val marker: CalendarDayMarker = CalendarDayMarker()
)

object CalendarGridRules {
    fun weekStart(anchorDate: LocalDate): LocalDate {
        return anchorDate.minusDays(anchorDate.dayOfWeek.value.toLong() - 1L)
    }

    fun rangeFor(
        mode: CalendarDisplayMode,
        selectedDate: LocalDate,
        visibleMonth: YearMonth
    ): CalendarDateRange {
        return when (mode) {
            CalendarDisplayMode.WEEK -> {
                val start = weekStart(selectedDate)
                CalendarDateRange(start = start, endInclusive = start.plusDays(6))
            }

            CalendarDisplayMode.MONTH -> {
                val firstDay = visibleMonth.atDay(1)
                val start = firstDay.minusDays(firstDay.dayOfWeek.value.toLong() - 1L)
                CalendarDateRange(start = start, endInclusive = start.plusDays(41))
            }
        }
    }

    fun buildCells(
        mode: CalendarDisplayMode,
        selectedDate: LocalDate,
        visibleMonth: YearMonth,
        markers: Map<LocalDate, CalendarDayMarker>,
        today: LocalDate = LocalDate.now()
    ): List<CalendarDayCell> {
        val range = rangeFor(
            mode = mode,
            selectedDate = selectedDate,
            visibleMonth = visibleMonth
        )
        val cellCount = when (mode) {
            CalendarDisplayMode.WEEK -> 7
            CalendarDisplayMode.MONTH -> 42
        }

        return List(cellCount) { index ->
            val date = range.start.plusDays(index.toLong())
            CalendarDayCell(
                date = date,
                isCurrentMonth = mode == CalendarDisplayMode.WEEK || YearMonth.from(date) == visibleMonth,
                isToday = date == today,
                marker = markers[date] ?: CalendarDayMarker()
            )
        }
    }

    fun buildMarkers(
        tasks: List<DailyTask>,
        memos: List<CalendarMemo>,
        memoColorsById: Map<String, Long>,
        maxMemoDots: Int = 3
    ): Map<LocalDate, CalendarDayMarker> {
        val tasksByDate = tasks.groupingBy { it.targetDate }.eachCount()
        val memosByDate = memos
            .filter { !it.isCompleted }
            .groupBy { it.targetDate }
            .mapValues { (_, dayMemos) -> dayMemos.sortedBy { it.orderWeight } }
        val allDates = tasksByDate.keys + memosByDate.keys

        return allDates.associateWith { date ->
            val dayMemos = memosByDate[date].orEmpty()
            CalendarDayMarker(
                taskCount = tasksByDate[date] ?: 0,
                memoCount = dayMemos.size,
                memoColors = dayMemos
                    .take(maxMemoDots)
                    .map { memo -> memoColorsById[memo.id] ?: memo.colorValue }
            )
        }
    }
}
