package com.example.minitask.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE targetDate = :date ORDER BY isPinned DESC, isCompleted ASC, priority ASC, orderWeight ASC")
    fun getTasksByDate(date: LocalDate): Flow<List<DailyTask>>

    @Query("SELECT * FROM tasks WHERE targetDate BETWEEN :startDate AND :endDate ORDER BY targetDate ASC, isPinned DESC, isCompleted ASC, priority ASC, orderWeight ASC")
    fun getTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: DailyTask)

    @Update
    fun updateTask(task: DailyTask)

    @Delete
    fun deleteTask(task: DailyTask)

    @Query("SELECT * FROM memos WHERE targetDate = :date ORDER BY orderWeight ASC")
    fun getMemosByDate(date: LocalDate): Flow<List<CalendarMemo>>

    @Query("SELECT * FROM memos WHERE isCompleted = 0")
    fun getAllActiveMemos(): Flow<List<CalendarMemo>>

    @Query("SELECT * FROM memos WHERE isCompleted = 0 AND targetDate BETWEEN :startDate AND :endDate ORDER BY targetDate ASC, orderWeight ASC")
    fun getActiveMemosBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<CalendarMemo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMemo(memo: CalendarMemo)

    @Update
    fun updateMemo(memo: CalendarMemo)

    @Delete
    fun deleteMemo(memo: CalendarMemo)

    @Query("SELECT * FROM routines ORDER BY orderWeight ASC")
    fun getAllRoutines(): Flow<List<DailyRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoutine(routine: DailyRoutine)

    @Update
    fun updateRoutine(routine: DailyRoutine)

    @Delete
    fun deleteRoutine(routine: DailyRoutine)
}
