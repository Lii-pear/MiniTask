package com.example.minitask.ui.home

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.minitask.domain.routine.RoutineScheduleInput
import com.example.minitask.ui.components.AddTaskContent
import com.example.minitask.ui.components.FlexibleCalendarGrid
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
        onAddTask = viewModel::addTask,
        onAddRoutine = { title, scheduleInput, startDate ->
            viewModel.addRoutine(title, scheduleInput, startDate)
        },
        onAddMemo = viewModel::addMemo,
        onToggleTaskComplete = viewModel::toggleTaskComplete,
        onToggleTaskPinned = viewModel::toggleTaskPinned,
        onDeleteTask = viewModel::deleteTask,
        onUpdateMemo = viewModel::updateMemo,
        onDeleteMemo = viewModel::deleteMemo,
        onUpdateRoutine = viewModel::updateRoutine,
        onDeleteRoutine = viewModel::deleteRoutine
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    weeklyStats: List<Float>,
    screenState: HomeScreenState,
    onDateSelected: (LocalDate) -> Unit,
    onAddTask: (String, com.example.minitask.data.model.TaskPriority) -> Unit,
    onAddRoutine: (String, RoutineScheduleInput, LocalDate) -> Unit,
    onAddMemo: (String, Long) -> Unit,
    onToggleTaskComplete: (com.example.minitask.data.model.DailyTask) -> Unit,
    onToggleTaskPinned: (com.example.minitask.data.model.DailyTask) -> Unit,
    onDeleteTask: (com.example.minitask.data.model.DailyTask) -> Unit,
    onUpdateMemo: (com.example.minitask.data.model.CalendarMemo) -> Unit,
    onDeleteMemo: (com.example.minitask.data.model.CalendarMemo) -> Unit,
    onUpdateRoutine: (com.example.minitask.data.model.DailyRoutine) -> Unit,
    onDeleteRoutine: (com.example.minitask.data.model.DailyRoutine) -> Unit
) {
    val selectedDate = uiState.selectedDate
    val dateTitle = remember(selectedDate) {
        val weekday = when (selectedDate.dayOfWeek.value) {
            1 -> "周一"
            2 -> "周二"
            3 -> "周三"
            4 -> "周四"
            5 -> "周五"
            6 -> "周六"
            else -> "周日"
        }
        "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 $weekday"
    }

    val sortedRoutines = remember(uiState.routines, selectedDate) {
        uiState.routines.sortedWith(
            compareBy(
                { it.lastCompletedDate == selectedDate },
                { it.orderWeight }
            )
        )
    }

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
                .padding(paddingValues)
        ) {
            item {
                FlexibleCalendarGrid(
                    selectedDate = selectedDate,
                    memosByDate = uiState.memosByDate,
                    dynamicMemoColors = uiState.dynamicMemoColors,
                    holidayBadgeMap = emptyMap(),
                    isExpanded = screenState.isCalendarExpanded,
                    onExpandedChange = screenState::updateCalendarExpanded,
                    onDateSelected = onDateSelected,
                    onMemoAreaSelected = { screenState.openMemoDialog() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            screenState.openStatsDialog()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.BarChart, "统计", tint = Color(0xFFBDBDBD))
                    }
                }
            }

            item(key = "memos_section") {
                AnimatedVisibility(
                    visible = uiState.memos.isNotEmpty(),
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(120))
                ) {
                    Column {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            items(uiState.memos, key = { it.id }) { memo ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE3F2FD),
                                    modifier = Modifier
                                        .animateItem()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            screenState.openMemoDialog()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF29B6F6))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = memo.title,
                                            color = Color(0xFF1565C0),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            item {
                SectionTitle(text = "每日必做", top = 8.dp, bottom = 4.dp)
            }

            if (sortedRoutines.isEmpty()) {
                item {
                    Text(
                        "今日暂无打卡习惯",
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            } else {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(sortedRoutines, key = { _, item -> item.id }) { _, routine ->
                            RoutineChip(
                                routine = routine,
                                isDone = routine.lastCompletedDate == selectedDate,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    val isDone = routine.lastCompletedDate == selectedDate
                                    onUpdateRoutine(
                                        routine.copy(lastCompletedDate = if (isDone) null else selectedDate)
                                    )
                                },
                                onLongClick = { onDeleteRoutine(routine) }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle(text = "每日任务", top = 16.dp, bottom = 12.dp)
            }

            if (uiState.tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("今日暂无任务", color = Color.LightGray, fontSize = 15.sp)
                    }
                }
            } else {
                items(items = uiState.tasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        modifier = Modifier.animateItem(),
                        onClick = { onToggleTaskComplete(task) },
                        onPinClick = { onToggleTaskPinned(task) },
                        onDeleteClick = { onDeleteTask(task) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }

        if (screenState.showAddTaskSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = screenState::dismissAddTaskSheet,
                sheetState = sheetState
            ) {
                AddTaskContent(
                    memosCountToday = uiState.memos.size,
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
                onAddMemo = { title -> onAddMemo(title, 0xFF40C4FF) },
                onTogglePin = { memo ->
                    onUpdateMemo(memo.copy(orderWeight = if (memo.orderWeight == 0L) System.nanoTime() else 0L))
                },
                onToggleComplete = { memo -> onUpdateMemo(memo.copy(isCompleted = !memo.isCompleted)) },
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
private fun SectionTitle(
    text: String,
    top: androidx.compose.ui.unit.Dp,
    bottom: androidx.compose.ui.unit.Dp
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color.LightGray,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = top, bottom = bottom)
    )
}

@Composable
private fun StatisticsDialog(
    weeklyData: List<Float>,
    onDismiss: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    val animationProgress by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
        label = "chartAnimation"
    )

    val weekDayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val todayIndex = LocalDate.now().dayOfWeek.value - 1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("本周成就", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "本周完成率",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyData.forEachIndexed { index, dataValue ->
                        val displayValue = dataValue.coerceAtLeast(0.05f)
                        val isToday = index == todayIndex
                        val isFuture = index > todayIndex
                        val is100Percent = dataValue >= 1f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!isFuture) {
                                Text(
                                    text = "${(dataValue * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        is100Percent -> Color(0xFF4CAF50)
                                        isToday -> Color.Black
                                        else -> Color.Gray
                                    },
                                    modifier = Modifier
                                        .alpha(animationProgress)
                                        .padding(bottom = 6.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(120.dp * displayValue * animationProgress)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isFuture -> Color(0xFFF5F5F5)
                                            is100Percent -> Color(0xFF4CAF50)
                                            isToday -> Color.Black
                                            else -> Color(0xFFE0E0E0)
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
                                    text = weekDayNames[index],
                                    fontSize = 11.sp,
                                    color = when {
                                        isToday -> Color.White
                                        isFuture -> Color(0xFFDDDDDD)
                                        else -> Color.Gray
                                    },
                                    fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("继续保持", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
