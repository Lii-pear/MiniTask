package com.example.minitask.ui.home

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minitask.core.di.appContainer
import com.example.minitask.data.model.CalendarMemo
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.domain.routine.RoutineScheduleInput
import com.example.minitask.ui.components.AddTaskContent
import com.example.minitask.ui.components.LightweightCalendar
import com.example.minitask.ui.components.MemoManagerDialog
import com.example.minitask.ui.components.RoutineChip
import com.example.minitask.ui.components.TaskListItem
import com.example.minitask.viewmodel.HomeUiState
import com.example.minitask.viewmodel.TaskViewModel
import com.example.minitask.viewmodel.TaskViewModelFactory
import java.time.LocalDate

@Composable
fun HomeRoute(
    screenState: HomeScreenState = rememberHomeScreenState()
) {
    val context = LocalContext.current
    val container = context.appContainer
    val factory = remember(container) {
        TaskViewModelFactory(
            repository = container.taskRepository,
            dispatchers = container.dispatchers
        )
    }
    val viewModel: TaskViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val weeklyStats = if (screenState.showStatsDialog) {
        viewModel.weeklyStats.collectAsStateWithLifecycle(initialValue = List(7) { 0f }).value
    } else {
        emptyList()
    }

    HomeScreen(
        uiState = uiState,
        weeklyStats = weeklyStats,
        screenState = screenState,
        onDateSelected = viewModel::changeDate,
        onPreviousCalendar = viewModel::goToPreviousPeriod,
        onNextCalendar = viewModel::goToNextPeriod,
        onToday = viewModel::goToToday,
        onToggleCalendarMode = viewModel::toggleCalendarMode,
        onAddTask = viewModel::addTask,
        onAddRoutine = viewModel::addRoutine,
        onAddMemo = viewModel::addMemo,
        onToggleTaskComplete = viewModel::toggleTaskComplete,
        onToggleTaskPinned = viewModel::toggleTaskPinned,
        onDeleteTask = viewModel::deleteTask,
        onToggleRoutineDone = viewModel::toggleRoutineDone,
        onDeleteRoutine = viewModel::deleteRoutine,
        onUpdateMemo = viewModel::updateMemo,
        onDeleteMemo = viewModel::deleteMemo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    weeklyStats: List<Float>,
    screenState: HomeScreenState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousCalendar: () -> Unit,
    onNextCalendar: () -> Unit,
    onToday: () -> Unit,
    onToggleCalendarMode: () -> Unit,
    onAddTask: (String, TaskPriority) -> Unit,
    onAddRoutine: (String, RoutineScheduleInput, LocalDate) -> Unit,
    onAddMemo: (String) -> Unit,
    onToggleTaskComplete: (DailyTask) -> Unit,
    onToggleTaskPinned: (DailyTask) -> Unit,
    onDeleteTask: (DailyTask) -> Unit,
    onToggleRoutineDone: (DailyRoutine) -> Unit,
    onDeleteRoutine: (DailyRoutine) -> Unit,
    onUpdateMemo: (CalendarMemo) -> Unit,
    onDeleteMemo: (CalendarMemo) -> Unit
) {
    val selectedDate = uiState.selectedDate
    val activeMemos = remember(uiState.memos) { uiState.memos.filter { !it.isCompleted } }
    val sortedRoutines = remember(uiState.routines, selectedDate) {
        uiState.routines.sortedWith(
            compareBy(
                { it.lastCompletedDate == selectedDate },
                { it.orderWeight }
            )
        )
    }
    val dateTitle = remember(selectedDate) { selectedDate.toDisplayTitle() }
    val view = LocalView.current

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    screenState.openAddTaskSheet()
                },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item(key = "calendar") {
                LightweightCalendar(
                    selectedDate = selectedDate,
                    visibleMonth = uiState.visibleMonth,
                    mode = uiState.calendarMode,
                    cells = uiState.calendarCells,
                    onPrevious = onPreviousCalendar,
                    onNext = onNextCalendar,
                    onToday = onToday,
                    onToggleMode = onToggleCalendarMode,
                    onDateSelected = onDateSelected,
                    onMemoAreaSelected = { date ->
                        onDateSelected(date)
                        screenState.openMemoDialog()
                    }
                )
            }

            item(key = "date_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = dateTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = daySummary(uiState.tasks, activeMemos, sortedRoutines, selectedDate),
                            fontSize = 12.sp,
                            color = Color(0xFF777777),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            screenState.openStatsDialog()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "统计",
                            tint = Color(0xFF777777)
                        )
                    }
                }
            }

            if (activeMemos.isNotEmpty()) {
                item(key = "memos") {
                    MemoStrip(
                        memos = activeMemos,
                        memoColorsById = uiState.memoColorsById,
                        onOpen = screenState::openMemoDialog
                    )
                }
            }

            item(key = "routines_title") {
                SectionTitle(text = "每日必做")
            }

            if (sortedRoutines.isEmpty()) {
                item(key = "empty_routines") {
                    EmptyHint(text = "今天没有需要打卡的习惯")
                }
            } else {
                item(key = "routines") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortedRoutines.forEach { routine ->
                            RoutineChip(
                                routine = routine,
                                isDone = routine.lastCompletedDate == selectedDate,
                                onClick = { onToggleRoutineDone(routine) },
                                onLongClick = { onDeleteRoutine(routine) }
                            )
                        }
                    }
                }
            }

            item(key = "tasks_title") {
                SectionTitle(text = "今日任务")
            }

            if (uiState.tasks.isEmpty()) {
                item(key = "empty_tasks") {
                    EmptyHint(text = "今天暂时没有任务")
                }
            } else {
                items(items = uiState.tasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onToggleTaskComplete(task) },
                        onPinClick = { onToggleTaskPinned(task) },
                        onDeleteClick = { onDeleteTask(task) }
                    )
                }
            }
        }

        if (screenState.showAddTaskSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = screenState::dismissAddTaskSheet,
                sheetState = sheetState
            ) {
                AddTaskContent(
                    onSaveTask = onAddTask,
                    onSaveRoutine = { title, scheduleInput ->
                        onAddRoutine(title, scheduleInput, selectedDate)
                    },
                    onSaveMemo = onAddMemo
                )
            }
        }

        if (screenState.showMemoDialog) {
            MemoManagerDialog(
                selectedDate = selectedDate,
                currentMemos = uiState.memos,
                onDismiss = screenState::dismissMemoDialog,
                onAddMemo = onAddMemo,
                onTogglePin = { memo ->
                    onUpdateMemo(memo.copy(orderWeight = if (memo.orderWeight == 0L) System.nanoTime() else 0L))
                },
                onToggleComplete = { memo ->
                    onUpdateMemo(memo.copy(isCompleted = !memo.isCompleted))
                },
                onDelete = onDeleteMemo
            )
        }

        if (screenState.showStatsDialog) {
            StatisticsDialog(
                weeklyData = weeklyStats,
                onDismiss = screenState::dismissStatsDialog
            )
        }
    }
}

@Composable
private fun MemoStrip(
    memos: List<CalendarMemo>,
    memoColorsById: Map<String, Long>,
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        memos.forEach { memo ->
            val color = Color(memoColorsById[memo.id] ?: memo.colorValue)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.16f),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpen() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 4.dp, height = 16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = memo.title,
                        color = Color(0xFF24343D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF9E9E9E),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color(0xFFC2C2C2), fontSize = 14.sp)
    }
}

@Composable
private fun StatisticsDialog(
    weeklyData: List<Float>,
    onDismiss: () -> Unit
) {
    val values = if (weeklyData.isEmpty()) List(7) { 0f } else weeklyData
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    val todayIndex = LocalDate.now().dayOfWeek.value - 1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "本周完成率",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                values.take(7).forEachIndexed { index, value ->
                    val normalized = value.coerceIn(0f, 1f)
                    val displayHeight = (110 * normalized.coerceAtLeast(0.04f)).dp
                    val isToday = index == todayIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = "${(normalized * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = if (isToday) Color.Black else Color(0xFF777777),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(displayHeight)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        normalized >= 1f -> Color(0xFF43A047)
                                        isToday -> Color.Black
                                        else -> Color(0xFFDADADA)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isToday) Color.Black else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels[index],
                                fontSize = 11.sp,
                                color = if (isToday) Color.White else Color(0xFF777777),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "关闭", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun LocalDate.toDisplayTitle(): String {
    val weekday = when (dayOfWeek.value) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }
    return "${year}年${monthValue}月${dayOfMonth}日 $weekday"
}

private fun daySummary(
    tasks: List<DailyTask>,
    memos: List<CalendarMemo>,
    routines: List<DailyRoutine>,
    selectedDate: LocalDate
): String {
    val doneTasks = tasks.count { it.isCompleted }
    val doneRoutines = routines.count { it.lastCompletedDate == selectedDate }
    return "${doneTasks}/${tasks.size} 任务 · ${doneRoutines}/${routines.size} 习惯 · ${memos.size} 备忘"
}
