package com.example.minitask.core.calendar

import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.YearMonth
import java.util.Collections
import java.util.LinkedHashMap

object LunarDateCache {
    private const val MAX_CACHE_SIZE = 4096
    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<LocalDate, String>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<LocalDate, String>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    )

    fun peek(date: LocalDate): String? = cache[date]

    fun getText(date: LocalDate): String = peek(date) ?: computeAndCache(date)

    fun prewarmMonth(yearMonth: YearMonth) {
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = yearMonth.atDay(day)
            if (peek(date) == null) {
                computeAndCache(date)
            }
        }
    }

    private fun computeAndCache(date: LocalDate): String {
        val value = run {
            val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.lunar
            if (solar.festivals.isNotEmpty()) return@run solar.festivals[0]
            if (lunar.festivals.isNotEmpty()) return@run lunar.festivals[0]
            if (lunar.jieQi.isNotEmpty()) return@run lunar.jieQi
            if (lunar.day == 1) "${lunar.monthInChinese}月" else lunar.dayInChinese
        }
        cache[date] = value
        return value
    }
}
