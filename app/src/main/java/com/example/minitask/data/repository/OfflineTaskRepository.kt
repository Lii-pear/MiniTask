package com.example.minitask.data.repository

import com.example.minitask.core.dispatchers.AppCoroutineDispatchers
import com.example.minitask.data.local.TaskDao
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import kotlinx.coroutines.withContext
import java.time.LocalDate

class OfflineTaskRepository(
    private val taskDao: TaskDao,
    private val dispatchers: AppCoroutineDispatchers
) : TaskRepository {

    override fun getTasks(date: LocalDate) = taskDao.getTasksByDate(date)

    override fun getTasksBetween(startDate: LocalDate, endDate: LocalDate) =
        taskDao.getTasksBetween(startDate, endDate)

    override fun getMemos(date: LocalDate) = taskDao.getMemosByDate(date)

    override fun getAllActiveMemos() = taskDao.getAllActiveMemos()

    override fun getActiveMemosBetween(startDate: LocalDate, endDate: LocalDate) =
        taskDao.getActiveMemosBetween(startDate, endDate)

    override fun getRoutines() = taskDao.getAllRoutines()

    override suspend fun insertTask(task: DailyTask) = withContext(dispatchers.io) {
        taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: DailyTask) = withContext(dispatchers.io) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: DailyTask) = withContext(dispatchers.io) {
        taskDao.deleteTask(task)
    }

    override suspend fun insertMemo(memo: CalendarMemo) = withContext(dispatchers.io) {
        taskDao.insertMemo(memo)
    }

    override suspend fun updateMemo(memo: CalendarMemo) = withContext(dispatchers.io) {
        taskDao.updateMemo(memo)
    }

    override suspend fun deleteMemo(memo: CalendarMemo) = withContext(dispatchers.io) {
        taskDao.deleteMemo(memo)
    }

    override suspend fun insertRoutine(routine: DailyRoutine) = withContext(dispatchers.io) {
        taskDao.insertRoutine(routine)
    }

    override suspend fun updateRoutine(routine: DailyRoutine) = withContext(dispatchers.io) {
        taskDao.updateRoutine(routine)
    }

    override suspend fun deleteRoutine(routine: DailyRoutine) = withContext(dispatchers.io) {
        taskDao.deleteRoutine(routine)
    }
}
