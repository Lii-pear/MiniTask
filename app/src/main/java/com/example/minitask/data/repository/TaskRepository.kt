package com.example.minitask.data.repository

import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getTasks(date: LocalDate): Flow<List<DailyTask>>

    fun getTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyTask>>

    fun getMemos(date: LocalDate): Flow<List<CalendarMemo>>

    fun getAllActiveMemos(): Flow<List<CalendarMemo>>

    fun getActiveMemosBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarMemo>>

    fun getRoutines(): Flow<List<DailyRoutine>>

    suspend fun insertTask(task: DailyTask)

    suspend fun updateTask(task: DailyTask)

    suspend fun deleteTask(task: DailyTask)

    suspend fun insertMemo(memo: CalendarMemo)

    suspend fun updateMemo(memo: CalendarMemo)

    suspend fun deleteMemo(memo: CalendarMemo)

    suspend fun insertRoutine(routine: DailyRoutine)

    suspend fun updateRoutine(routine: DailyRoutine)

    suspend fun deleteRoutine(routine: DailyRoutine)
}
