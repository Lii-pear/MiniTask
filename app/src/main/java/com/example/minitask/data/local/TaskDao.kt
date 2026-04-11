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
    // === 每日任务 ===
    @Query("SELECT * FROM tasks WHERE targetDate = :date ORDER BY isPinned DESC, isCompleted ASC, priority ASC, orderWeight ASC")
    fun getTasksByDate(date: LocalDate): Flow<List<DailyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: DailyTask)  // <--- 删除了 suspend 和返回值，绕过 KSP Bug

    @Update
    fun updateTask(task: DailyTask)

    @Delete
    fun deleteTask(task: DailyTask)


    // === 备忘录 ===
    @Query("SELECT * FROM memos WHERE targetDate = :date ORDER BY orderWeight ASC")
    fun getMemosByDate(date: LocalDate): Flow<List<CalendarMemo>>

    @Query("SELECT * FROM memos WHERE isCompleted = 0")
    fun getAllActiveMemos(): Flow<List<CalendarMemo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMemo(memo: CalendarMemo)

    @Update
    fun updateMemo(memo: CalendarMemo)

    @Delete
    fun deleteMemo(memo: CalendarMemo)


    // === 每日必做 ===
    @Query("SELECT * FROM routines ORDER BY orderWeight ASC")
    fun getAllRoutines(): Flow<List<DailyRoutine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoutine(routine: DailyRoutine)

    @Update
    fun updateRoutine(routine: DailyRoutine)

    @Delete
    fun deleteRoutine(routine: DailyRoutine)
}