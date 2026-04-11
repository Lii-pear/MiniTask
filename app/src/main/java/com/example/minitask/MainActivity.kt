package com.example.minitask

import android.os.Bundle
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.minitask.ui.components.AddTaskContent
import com.example.minitask.ui.components.FlexibleCalendarGrid
import com.example.minitask.ui.components.MemoManagerDialog
import com.example.minitask.ui.components.RoutineChip
import com.example.minitask.ui.components.TaskListItem
import com.example.minitask.ui.theme.MiniTaskTheme
import com.example.minitask.viewmodel.TaskViewModel
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MiniTaskTheme { TaskAppMainScreen() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAppMainScreen(viewModel: TaskViewModel = viewModel()) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val memos by viewModel.memos.collectAsState(initial = emptyList())
    val allActiveMemos by viewModel.allActiveMemos.collectAsState(initial = emptyList())
    val routines by viewModel.routines.collectAsState(initial = emptyList())
    val realWeeklyStats by viewModel.weeklyStats.collectAsState()

    val calendarData by viewModel.calendarMemoData.collectAsState()
    val memosByDate = calendarData.first
    val dynamicMemoColors = calendarData.second

    var displayMemos by remember { mutableStateOf(memos) }
    LaunchedEffect(memos) {
        if (memos.isNotEmpty()) {
            displayMemos = memos
        }
    }

    var showMemoDialog by remember { mutableStateOf(false) }
    var showAddTaskSheet by remember { mutableStateOf(false) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }

    val view = LocalView.current

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

                    showAddTaskSheet = true
                },
                containerColor = Color.Black, contentColor = Color.White, shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "添加") }
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
                    memosByDate = memosByDate,
                    dynamicMemoColors = dynamicMemoColors,
                    holidayBadgeMap = emptyMap(),
                    isExpanded = isCalendarExpanded,
                    onExpandedChange = { isCalendarExpanded = it },
                    onDateSelected = { viewModel.changeDate(it) },
                    onMemoAreaSelected = { showMemoDialog = true }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                val dateTitle =
                    "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日 " +
                            when (selectedDate.dayOfWeek.value) {
                                1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; else -> "周日"
                            }

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
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); showStatsDialog =
                        true
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.BarChart, "统计", tint = Color(0xFFBDBDBD))
                    }
                }
            }

            item(key = "memos_section") {
                AnimatedVisibility(
                    visible = memos.isNotEmpty(),
                    enter = expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(tween(200)),
                    exit = shrinkVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + fadeOut(tween(150))
                ) {
                    Column {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp)
                        ) {
                            items(displayMemos, key = { it.id }) { memo ->
                                val bgSurfaceColor = Color(0xFFE3F2FD)
                                val indicatorColor = Color(0xFF29B6F6)
                                val textColor = Color(0xFF1565C0)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = bgSurfaceColor,
                                    modifier = Modifier
                                        .animateItem()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); showMemoDialog =
                                            true
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(indicatorColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = memo.title,
                                            color = textColor,
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
                Text(
                    "每日必做",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 4.dp
                    )
                )
            }

            if (routines.isEmpty()) {
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
                    val sortedRoutines = remember(
                        routines,
                        selectedDate
                    ) {
                        routines.sortedWith(
                            compareBy(
                                { it.lastCompletedDate == selectedDate },
                                { it.orderWeight })
                        )
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        itemsIndexed(sortedRoutines, key = { _, it -> it.id }) { _, routine ->
                            val isDone = routine.lastCompletedDate == selectedDate
                            RoutineChip(
                                routine = routine,
                                isDone = isDone,
                                modifier = Modifier.animateItem(),
                                onClick = { viewModel.updateRoutine(routine.copy(lastCompletedDate = if (isDone) null else selectedDate)) },
                                onLongClick = { viewModel.deleteRoutine(routine) })
                        }
                    }
                }
            }

            item {
                Text(
                    "每日任务",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 16.dp,
                        bottom = 12.dp
                    )
                )
            }

            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("今日暂无任务", color = Color.LightGray, fontSize = 15.sp) }
                }
            } else {
                items(items = tasks, key = { it.id }) { task ->
                    TaskListItem(
                        task = task,
                        modifier = Modifier.animateItem(),
                        onClick = { viewModel.toggleTaskComplete(task) },
                        onPinClick = { viewModel.toggleTaskPinned(task) },
                        onDeleteClick = { viewModel.deleteTask(task) })
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (showAddTaskSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showAddTaskSheet = false },
                sheetState = sheetState
            ) {
                AddTaskContent(
                    memosCountToday = allActiveMemos.size,
                    onSaveTask = { title, priority ->
                        viewModel.addTask(title, priority)
                    },
                    // ★ 核心修复：添加新习惯时，强制使用【当前日历选定的日期 (selectedDate)】作为起点！
                    onSaveRoutine = { title, type, value, _ ->
                        viewModel.addRoutine(title, type, value, selectedDate)
                    },
                    onSaveMemo = { title, colorValue ->
                        viewModel.addMemo(title, colorValue)
                    }
                )
            }
        }

        if (showMemoDialog) {
            MemoManagerDialog(
                selectedDate = selectedDate,
                currentMemos = memos,
                onDismiss = { showMemoDialog = false },
                onAddMemo = { title -> viewModel.addMemo(title, 0xFF40C4FF) },
                onTogglePin = { memo -> viewModel.updateMemo(memo.copy(orderWeight = if (memo.orderWeight == 0L) System.nanoTime() else 0L)) },
                onToggleComplete = { memo -> viewModel.updateMemo(memo.copy(isCompleted = !memo.isCompleted)) },
                onDelete = { memo -> viewModel.deleteMemo(memo) })
        }

        if (showStatsDialog) {
            StatisticsDialog(weeklyData = realWeeklyStats, onDismiss = { showStatsDialog = false })
        }
    }
}

@Composable
fun StatisticsDialog(weeklyData: List<Float>, onDismiss: () -> Unit) {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
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
                        val displayValue = if (dataValue <= 0.05f) 0.05f else dataValue
                        val isToday = index == todayIndex
                        val isFuture = index > todayIndex
                        val is100Percent = dataValue >= 1f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (!isFuture) {
                                val textColor =
                                    if (is100Percent) Color(0xFF4CAF50) else if (isToday) Color.Black else Color.Gray
                                Text(
                                    text = "${(dataValue * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier
                                        .alpha(animationProgress)
                                        .padding(bottom = 6.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            val barColor = when {
                                isFuture -> Color(0xFFF5F5F5)
                                is100Percent -> Color(0xFF4CAF50)
                                isToday -> Color.Black
                                else -> Color(0xFFE0E0E0)
                            }
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(120.dp * displayValue * animationProgress)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(barColor)
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
                                    color = if (isToday) Color.White else if (isFuture) Color(
                                        0xFFDDDDDD
                                    ) else Color.Gray,
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
                Text(
                    "继续保持",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}