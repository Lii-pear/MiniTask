package com.example.minitask.domain.memo

import com.example.minitask.data.model.CalendarMemo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MemoColorAllocatorTest {
    private val monday = LocalDate.of(2026, 5, 18)

    @Test
    fun groupsActiveMemosByDateAndKeepsOrderWeight() {
        val later = CalendarMemo(id = "later", title = "Later", targetDate = monday, orderWeight = 2)
        val earlier = CalendarMemo(id = "earlier", title = "Earlier", targetDate = monday, orderWeight = 1)
        val completed = CalendarMemo(
            id = "completed",
            title = "Completed",
            targetDate = monday,
            isCompleted = true,
            orderWeight = 0
        )

        val data = MemoColorAllocator.buildCalendarData(listOf(later, completed, earlier))

        assertEquals(listOf(earlier, later), data.memosByDate[monday])
        assertFalse(data.dynamicMemoColors.containsKey(completed.id))
    }

    @Test
    fun avoidsRepeatingPreviousDayVisibleColorsWhenPossible() {
        val firstDay = listOf(
            CalendarMemo(id = "d1a", title = "A", targetDate = monday, orderWeight = 1),
            CalendarMemo(id = "d1b", title = "B", targetDate = monday, orderWeight = 2)
        )
        val secondDay = listOf(
            CalendarMemo(id = "d2a", title = "C", targetDate = monday.plusDays(1), orderWeight = 1),
            CalendarMemo(id = "d2b", title = "D", targetDate = monday.plusDays(1), orderWeight = 2)
        )

        val data = MemoColorAllocator.buildCalendarData(firstDay + secondDay)
        val firstDayColors = firstDay.mapNotNull { data.dynamicMemoColors[it.id] }.toSet()
        val secondDayColors = secondDay.mapNotNull { data.dynamicMemoColors[it.id] }.toSet()

        assertTrue(firstDayColors.isNotEmpty())
        assertTrue(secondDayColors.isNotEmpty())
        assertTrue(firstDayColors.intersect(secondDayColors).isEmpty())
    }

    @Test
    fun assignsColorsOnlyToVisibleMemos() {
        val memos = (0..4).map { index ->
            CalendarMemo(
                id = "memo-$index",
                title = "Memo $index",
                targetDate = monday,
                orderWeight = index.toLong()
            )
        }

        val data = MemoColorAllocator.buildCalendarData(memos, maxVisiblePerDay = 4)

        assertEquals(4, data.dynamicMemoColors.size)
        assertFalse(data.dynamicMemoColors.containsKey("memo-4"))
    }
}
