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
import com.example.minitask.domain.calendar.CalendarDateRange
import com.example.minitask.domain.calendar.CalendarDayCell
import com.example.minitask.domain.calendar.CalendarDisplayMode
import com.example.minitask.domain.calendar.CalendarGridRules
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val calendarMode: CalendarDisplayMode = CalendarDisplayMode.WEEK,
    val visibleMonth: YearMonth = YearMonth.from(LocalDate.now()),
    val calendarCells: List<CalendarDayCell> = emptyList(),
    val tasks: List<DailyTask> = emptyList(),
    val memos: List<CalendarMemo> = emptyList(),
    val routines: List<DailyRoutine> = emptyList(),
    val memoColorsById: Map<String, Long> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModel(
    private val repository: TaskRepository,
    private val dispatchers: AppCoroutineDispatchers
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _calendarMode = MutableStateFlow(CalendarDisplayMode.WEEK)
    private val _visibleMonth = MutableStateFlow(YearMonth.from(LocalDate.now()))

    private val tasksFlow: Flow<List<DailyTask>> =
        _selectedDate.flatMapLatest { date -> repository.getTasks(date) }
    val tasks = tasksFlow

    private val memosFlow: Flow<List<CalendarMemo>> =
        _selectedDate.flatMapLatest { date -> repository.getMemos(date) }
    val memos = memosFlow

    private val routinesFlow: StateFlow<List<DailyRoutine>> =
        combine(repository.getRoutines(), _selectedDate) { allRoutines, date ->
            allRoutines.filter { RoutineRules.isActiveOnDate(it, date) }
        }.flowOn(dispatchers.default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routines = routinesFlow

    private data class CalendarRequest(
        val selectedDate: LocalDate,
        val mode: CalendarDisplayMode,
        val visibleMonth: YearMonth,
        val range: CalendarDateRange
    )

    private data class CalendarPart(
        val cells: List<CalendarDayCell>,
        val memoColorsById: Map<String, Long>
    )

    private val calendarRequestFlow = combine(
        _selectedDate,
        _calendarMode,
        _visibleMonth
    ) { selectedDate, mode, visibleMonth ->
        CalendarRequest(
            selectedDate = selectedDate,
            mode = mode,
            visibleMonth = visibleMonth,
            range = CalendarGridRules.rangeFor(
                mode = mode,
                selectedDate = selectedDate,
                visibleMonth = visibleMonth
            )
        )
    }

    private val calendarPartFlow: StateFlow<CalendarPart> =
        calendarRequestFlow.flatMapLatest { request ->
            combine(
                repository.getTasksBetween(request.range.start, request.range.endInclusive),
                repository.getActiveMemosBetween(request.range.start, request.range.endInclusive)
            ) { tasks, memos ->
                val memoData = MemoColorAllocator.buildCalendarData(
                    allMemos = memos,
                    maxVisiblePerDay = 3
                )
                val markers = CalendarGridRules.buildMarkers(
                    tasks = tasks,
                    memos = memos,
                    memoColorsById = memoData.dynamicMemoColors
                )
                CalendarPart(
                    cells = CalendarGridRules.buildCells(
                        mode = request.mode,
                        selectedDate = request.selectedDate,
                        visibleMonth = request.visibleMonth,
                        markers = markers
                    ),
                    memoColorsById = memoData.dynamicMemoColors
                )
            }
        }.flowOn(dispatchers.default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CalendarPart(cells = emptyList(), memoColorsById = emptyMap())
            )

    private data class CalendarChrome(
        val selectedDate: LocalDate,
        val mode: CalendarDisplayMode,
        val visibleMonth: YearMonth
    )

    private data class DayContent(
        val tasks: List<DailyTask>,
        val memos: List<CalendarMemo>,
        val routines: List<DailyRoutine>
    )

    private val calendarChromeFlow = combine(
        _selectedDate,
        _calendarMode,
        _visibleMonth
    ) { selectedDate, mode, visibleMonth ->
        CalendarChrome(
            selectedDate = selectedDate,
            mode = mode,
            visibleMonth = visibleMonth
        )
    }

    private val dayContentFlow = combine(
        tasksFlow,
        memosFlow,
        routinesFlow
    ) { tasks, memos, routines ->
        DayContent(tasks = tasks, memos = memos, routines = routines)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        calendarChromeFlow,
        dayContentFlow,
        calendarPartFlow
    ) { chrome, dayContent, calendarPart ->
        HomeUiState(
            selectedDate = chrome.selectedDate,
            calendarMode = chrome.mode,
            visibleMonth = chrome.visibleMonth,
            calendarCells = calendarPart.cells,
            tasks = dayContent.tasks,
            memos = dayContent.memos,
            routines = dayContent.routines,
            memoColorsById = calendarPart.memoColorsById
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    private val weeklyStatsFlow = run {
        val weekDates = WeeklyStatsCalculator.weekDatesFor(LocalDate.now())
        val tasksFlows = weekDates.map { date ->
            repository.getTasks(date).map { tasks -> date to tasks }
        }
        val allTasksFlow = combine(tasksFlows) { pairs -> pairs.toList() }

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

    fun changeDate(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun toggleCalendarMode() {
        val nextMode = when (_calendarMode.value) {
            CalendarDisplayMode.WEEK -> CalendarDisplayMode.MONTH
            CalendarDisplayMode.MONTH -> CalendarDisplayMode.WEEK
        }
        _calendarMode.value = nextMode
        _visibleMonth.value = YearMonth.from(_selectedDate.value)
    }

    fun goToPreviousPeriod() {
        when (_calendarMode.value) {
            CalendarDisplayMode.WEEK -> changeDate(_selectedDate.value.minusWeeks(1))
            CalendarDisplayMode.MONTH -> moveMonth(-1)
        }
    }

    fun goToNextPeriod() {
        when (_calendarMode.value) {
            CalendarDisplayMode.WEEK -> changeDate(_selectedDate.value.plusWeeks(1))
            CalendarDisplayMode.MONTH -> moveMonth(1)
        }
    }

    fun goToToday() {
        changeDate(LocalDate.now())
    }

    private fun moveMonth(offset: Long) {
        val targetMonth = _visibleMonth.value.plusMonths(offset)
        val targetDay = _selectedDate.value.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
        _visibleMonth.value = targetMonth
        _selectedDate.value = targetMonth.atDay(targetDay)
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

    fun addMemo(title: String) {
        viewModelScope.launch {
            val colorValue = MemoColorAllocator.nextColorValue(
                uiState.value.memos.count { !it.isCompleted }
            )
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

    fun toggleRoutineDone(routine: DailyRoutine) {
        viewModelScope.launch {
            val selectedDate = _selectedDate.value
            val isDone = routine.lastCompletedDate == selectedDate
            repository.updateRoutine(
                routine.copy(lastCompletedDate = if (isDone) null else selectedDate)
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
