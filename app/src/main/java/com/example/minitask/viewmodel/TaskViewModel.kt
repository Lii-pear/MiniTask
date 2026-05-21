package com.example.minitask.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitask.core.dispatchers.AppCoroutineDispatchers
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.data.repository.TaskRepository
import com.example.minitask.domain.memo.CalendarMemoPresentationData
import com.example.minitask.domain.memo.MemoColorAllocator
import com.example.minitask.domain.routine.RoutineRules
import com.example.minitask.domain.routine.RoutineScheduleInput
import com.example.minitask.domain.stats.WeeklyStatsCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<DailyTask> = emptyList(),
    val memos: List<CalendarMemo> = emptyList(),
    val allActiveMemos: List<CalendarMemo> = emptyList(),
    val routines: List<DailyRoutine> = emptyList(),
    val memosByDate: Map<LocalDate, List<CalendarMemo>> = emptyMap(),
    val dynamicMemoColors: Map<String, Long> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val dispatchers: AppCoroutineDispatchers
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val tasksFlow: Flow<List<DailyTask>> =
        _selectedDate.flatMapLatest { date -> repository.getTasks(date) }
    val tasks = tasksFlow

    private val memosFlow: Flow<List<CalendarMemo>> =
        _selectedDate.flatMapLatest { date -> repository.getMemos(date) }
    val memos = memosFlow

    private val allActiveMemosFlow = repository.getAllActiveMemos()
    val allActiveMemos = allActiveMemosFlow

    private val routinesFlow = combine(repository.getRoutines(), _selectedDate) { allRoutines, date ->
        allRoutines.filter { RoutineRules.isActiveOnDate(it, date) }
    }.flowOn(dispatchers.default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routines = routinesFlow

    private val weeklyStatsFlow = flowOf(Unit).flatMapLatest {
        val weekDates = WeeklyStatsCalculator.weekDatesFor(LocalDate.now())

        val tasksFlows = weekDates.map { date ->
            repository.getTasks(date).map { tasks -> date to tasks }
        }
        val allTasksFlow = combine(tasksFlows) { it.toList() }

        combine(allTasksFlow, repository.getRoutines()) { tasksList, allRoutines ->
            WeeklyStatsCalculator.calculate(
                weekDates = weekDates,
                tasksByDate = tasksList.toMap(),
                routines = allRoutines
            )
        }
    }.flowOn(dispatchers.default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), List(7) { 0f })
    val weeklyStats = weeklyStatsFlow

    private val calendarMemoDataFlow: StateFlow<CalendarMemoPresentationData> =
        allActiveMemosFlow.map { memos ->
            MemoColorAllocator.buildCalendarData(memos)
        }.flowOn(dispatchers.default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarMemoPresentationData(emptyMap(), emptyMap())
        )
    val calendarMemoData = calendarMemoDataFlow

    private data class PrimaryUiPart(
        val selectedDate: LocalDate,
        val tasks: List<DailyTask>,
        val memos: List<CalendarMemo>,
        val allActiveMemos: List<CalendarMemo>
    )

    private data class SecondaryUiPart(
        val routines: List<DailyRoutine>,
        val memosByDate: Map<LocalDate, List<CalendarMemo>>,
        val dynamicMemoColors: Map<String, Long>
    )

    private val primaryUiFlow = combine(
        _selectedDate,
        tasksFlow,
        memosFlow,
        allActiveMemosFlow
    ) { selectedDate, tasks, memos, allActiveMemos ->
        PrimaryUiPart(
            selectedDate = selectedDate,
            tasks = tasks,
            memos = memos,
            allActiveMemos = allActiveMemos
        )
    }

    private val secondaryUiFlow = combine(
        routinesFlow,
        calendarMemoDataFlow
    ) { routines, calendarData ->
        SecondaryUiPart(
            routines = routines,
            memosByDate = calendarData.memosByDate,
            dynamicMemoColors = calendarData.dynamicMemoColors
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        primaryUiFlow,
        secondaryUiFlow
    ) { primary, secondary ->
        HomeUiState(
            selectedDate = primary.selectedDate,
            tasks = primary.tasks,
            memos = primary.memos,
            allActiveMemos = primary.allActiveMemos,
            routines = secondary.routines,
            memosByDate = secondary.memosByDate,
            dynamicMemoColors = secondary.dynamicMemoColors
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
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
            repository.insertMemo(
                CalendarMemo(
                    title = title,
                    targetDate = _selectedDate.value,
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

    fun addRoutine(title: String, scheduleInput: RoutineScheduleInput, startDate: LocalDate) {
        viewModelScope.launch {
            repository.insertRoutine(
                DailyRoutine(
                    title = title,
                    routineType = scheduleInput.storageType,
                    repeatValue = scheduleInput.storageRepeatValue,
                    startDate = startDate
                )
            )
        }
    }

    fun updateRoutine(routine: DailyRoutine) {
        viewModelScope.launch { repository.updateRoutine(routine) }
    }

    fun deleteRoutine(routine: DailyRoutine) {
        viewModelScope.launch {
            val currentDate = _selectedDate.value
            if (RoutineRules.shouldDeleteImmediately(routine, currentDate)) {
                repository.deleteRoutine(routine)
            } else {
                repository.updateRoutine(
                    routine.copy(endDate = RoutineRules.endDateBefore(currentDate))
                )
            }
        }
    }
}
