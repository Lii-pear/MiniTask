package com.example.minitask.domain.memo

import com.example.minitask.data.model.CalendarMemo
import java.time.LocalDate

data class CalendarMemoPresentationData(
    val memosByDate: Map<LocalDate, List<CalendarMemo>>,
    val dynamicMemoColors: Map<String, Long>
)

object MemoColorAllocator {
    val palette = listOf(
        0xFF81D4FA, 0xFFA5D6A7, 0xFFFFCC80, 0xFFF48FB1,
        0xFFFFF59D, 0xFFB0BEC5, 0xFFB39DDB, 0xFFFFAB91,
        0xFF80CBC4, 0xFF9FA8DA, 0xFFE6EE9C, 0xFFBCAAA4
    )

    fun nextColorValue(currentCount: Int): Long {
        return palette[currentCount.coerceAtLeast(0) % palette.size]
    }

    fun buildCalendarData(
        allMemos: List<CalendarMemo>,
        maxVisiblePerDay: Int = 4
    ): CalendarMemoPresentationData {
        val activeMemos = allMemos.filter { !it.isCompleted }
        val groupedByDate = activeMemos
            .groupBy { it.targetDate }
            .mapValues { (_, dayMemos) -> dayMemos.sortedBy { it.orderWeight } }

        val colorMap = mutableMapOf<String, Long>()
        var previousDayColors = emptySet<Long>()

        for (date in groupedByDate.keys.sorted()) {
            val dayMemos = groupedByDate[date].orEmpty().take(maxVisiblePerDay)
            val currentDayColors = mutableSetOf<Long>()
            val preferredColors = palette.filterNot { previousDayColors.contains(it) }

            dayMemos.forEachIndexed { index, memo ->
                val pickedColor = if (preferredColors.isNotEmpty()) {
                    preferredColors[index % preferredColors.size]
                } else {
                    palette[index % palette.size]
                }
                colorMap[memo.id] = pickedColor
                currentDayColors.add(pickedColor)
            }

            previousDayColors = currentDayColors
        }

        return CalendarMemoPresentationData(
            memosByDate = groupedByDate,
            dynamicMemoColors = colorMap
        )
    }
}
