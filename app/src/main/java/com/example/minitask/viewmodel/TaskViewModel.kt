package com.example.minitask.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitask.data.local.AppDatabase
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.data.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(dao)
    }

    val tasks = _selectedDate.flatMapLatest { date -> repository.getTasks(date) }
    val memos = _selectedDate.flatMapLatest { date -> repository.getMemos(date) }
    val allActiveMemos = repository.getAllActiveMemos()

    private fun isRoutineActiveOnDate(routine: DailyRoutine, date: LocalDate): Boolean {
        if (date.isBefore(routine.startDate)) return false
        // ★ 核心逻辑：如果日期超过了 endDate，就不再显示！
        if (routine.endDate != null && date.isAfter(routine.endDate)) return false

        return when (routine.routineType) {
            "DAILY" -> true
            "INTERVAL" -> {
                val daysBetween = ChronoUnit.DAYS.between(routine.startDate, date)
                val interval = routine.repeatValue.toLongOrNull() ?: 1L
                daysBetween % interval == 0L
            }

            "WEEKLY" -> {
                val targetWeekDay = date.dayOfWeek.value
                val selectedDays = routine.repeatValue.split(",").mapNotNull { it.toIntOrNull() }
                selectedDays.contains(targetWeekDay)
            }

            else -> true
        }
    }

    val routines = combine(repository.getRoutines(), _selectedDate) { allRoutines, date ->
        allRoutines.filter { isRoutineActiveOnDate(it, date) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyStats = flow {
        val today = LocalDate.now()
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        emit(monday)
    }.flatMapLatest { monday ->
        val tasksFlows = (0..6).map { daysOffset ->
            val date = monday.plusDays(daysOffset.toLong())
            repository.getTasks(date).map { tasks -> date to tasks }
        }
        val allTasksFlow = combine(tasksFlows) { it.toList() }

        combine(allTasksFlow, repository.getRoutines()) { tasksList, allRoutines ->
            tasksList.map { (date, tasks) ->
                val activeRoutines = allRoutines.filter { isRoutineActiveOnDate(it, date) }

                val totalItems = tasks.size + activeRoutines.size
                if (totalItems == 0) {
                    0f
                } else {
                    val completedTasks = tasks.count { it.isCompleted }
                    val completedRoutines = activeRoutines.count { it.lastCompletedDate == date }
                    (completedTasks + completedRoutines).toFloat() / totalItems
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(7) { 0f })

    val calendarMemoData: StateFlow<Pair<Map<LocalDate, List<CalendarMemo>>, Map<String, Long>>> =
        allActiveMemos.map { memos ->
            val activeMemos = memos.filter { !it.isCompleted }
            val groupedByDate = activeMemos.groupBy { it.targetDate }

            val colorMap = mutableMapOf<String, Long>()
            val palette = listOf(
                0xFF81D4FA, 0xFFA5D6A7, 0xFFFFCC80, 0xFFF48FB1,
                0xFFFFF59D, 0xFFB0BEC5, 0xFFB39DDB, 0xFFFFAB91,
                0xFF80CBC4, 0xFF9FA8DA, 0xFFE6EE9C, 0xFFBCAAA4
            )
            val sortedDates = groupedByDate.keys.sorted()
            var previousDayColors = setOf<Long>()

            for (date in sortedDates) {
                val dayMemos =
                    groupedByDate[date]?.sortedBy { it.orderWeight }?.take(4) ?: emptyList()
                val currentDayColors = mutableSetOf<Long>()
                var availableColors = (palette - previousDayColors).shuffled()

                for (memo in dayMemos) {
                    if (availableColors.isNotEmpty()) {
                        val pickedColor = availableColors.first()
                        colorMap[memo.id] = pickedColor
                        currentDayColors.add(pickedColor)
                        availableColors = availableColors.drop(1)
                    }
                }
                previousDayColors = currentDayColors
            }
            Pair(groupedByDate, colorMap)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(emptyMap(), emptyMap())
        )

    fun changeDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addTask(title: String, priority: TaskPriority) {
        viewModelScope.launch {
            repository.insertTask(
                DailyTask(
                    title = title,
                    priority = priority,
                    targetDate = _selectedDate.value
                )
            )
        }
    }

    fun toggleTaskComplete(task: DailyTask) {
        viewModelScope.launch { repository.updateTask(task.copy(isCompleted = !task.isCompleted)) }
    }

    fun toggleTaskPinned(task: DailyTask) {
        viewModelScope.launch { repository.updateTask(task.copy(isPinned = !task.isPinned)) }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun addMemo(title: String, colorValue: Long) {
        viewModelScope.launch {
            val date = _selectedDate.value
            repository.insertMemo(
                CalendarMemo(
                    title = title,
                    targetDate = date,
                    colorValue = colorValue
                )
            )
        }
    }

    fun updateMemo(memo: CalendarMemo) {
        viewModelScope.launch { repository.updateMemo(memo) }
    }

    fun deleteMemo(memo: CalendarMemo) {
        viewModelScope.launch { repository.deleteMemo(memo) }
    }

    fun addRoutine(title: String, type: String, value: String, startDate: LocalDate) {
        viewModelScope.launch {
            repository.insertRoutine(
                DailyRoutine(
                    title = title,
                    routineType = type,
                    repeatValue = value,
                    startDate = startDate
                )
            )
        }
    }

    fun updateRoutine(routine: DailyRoutine) {
        viewModelScope.launch { repository.updateRoutine(routine) }
    }

    // ★ 核心修改：拦截物理删除，转为软删除（停止习惯）
    fun deleteRoutine(routine: DailyRoutine) {
        viewModelScope.launch {
            val currentDate = _selectedDate.value
            // 如果你是在“开始的那天”或者以前就删除了，那说明一次没做过，直接物理毁灭
            if (!currentDate.isAfter(routine.startDate)) {
                repository.deleteRoutine(routine)
            } else {
                // 如果是后来的日子删除的，进行“软删除”：将它的寿命终结在昨天！
                // 这样以前的日历依然保留打卡记录，但今天和以后都不会再出现了。
                repository.updateRoutine(routine.copy(endDate = currentDate.minusDays(1)))
            }
        }
    }
}