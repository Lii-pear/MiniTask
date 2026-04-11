package com.example.minitask.data.repository

import com.example.minitask.data.local.TaskDao
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class TaskRepository(private val taskDao: TaskDao) {
    fun getTasks(date: LocalDate) = taskDao.getTasksByDate(date)
    fun getMemos(date: LocalDate) = taskDao.getMemosByDate(date)
    fun getAllActiveMemos() = taskDao.getAllActiveMemos() // ★ 新增：获取全部全局备忘录
    fun getRoutines() = taskDao.getAllRoutines()

    suspend fun insertTask(task: DailyTask) =
        withContext(Dispatchers.IO) { taskDao.insertTask(task) }

    suspend fun updateTask(task: DailyTask) =
        withContext(Dispatchers.IO) { taskDao.updateTask(task) }

    suspend fun deleteTask(task: DailyTask) =
        withContext(Dispatchers.IO) { taskDao.deleteTask(task) }

    suspend fun insertMemo(memo: CalendarMemo) =
        withContext(Dispatchers.IO) { taskDao.insertMemo(memo) }

    suspend fun updateMemo(memo: CalendarMemo) =
        withContext(Dispatchers.IO) { taskDao.updateMemo(memo) }

    suspend fun deleteMemo(memo: CalendarMemo) =
        withContext(Dispatchers.IO) { taskDao.deleteMemo(memo) }

    suspend fun insertRoutine(routine: DailyRoutine) =
        withContext(Dispatchers.IO) { taskDao.insertRoutine(routine) }

    suspend fun updateRoutine(routine: DailyRoutine) =
        withContext(Dispatchers.IO) { taskDao.updateRoutine(routine) }

    suspend fun deleteRoutine(routine: DailyRoutine) =
        withContext(Dispatchers.IO) { taskDao.deleteRoutine(routine) }
}