package com.example.minitask.ui.components

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.R
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.domain.routine.RoutineScheduleInput
import com.example.minitask.domain.routine.RoutineType

@Composable
fun AddTaskContent(
    onSaveTask: (String, TaskPriority) -> Unit,
    onSaveRoutine: (String, RoutineScheduleInput) -> Unit,
    onSaveMemo: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LEVEL_1) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var routineType by remember { mutableStateOf(RoutineType.DAILY) }
    var intervalDays by remember { mutableIntStateOf(2) }
    val selectedWeekDays = remember { mutableStateListOf<Int>() }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    val tabLabels = listOf(
        stringResource(R.string.add_tab_task),
        stringResource(R.string.add_tab_memo),
        stringResource(R.string.add_tab_routine)
    )
    val weekDayLabels = listOf(
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat),
        stringResource(R.string.weekday_sun)
    )
    val titleRequired = stringResource(R.string.error_title_required)
    val weeklyRequired = stringResource(R.string.error_weekly_day_required)
    val addSuccess = stringResource(R.string.toast_add_success)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
    ) {
        SegmentedTabs(
            labels = tabLabels,
            selectedIndex = tabIndex,
            onSelected = { index ->
                if (tabIndex != index) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    tabIndex = index
                }
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = {
                Text(
                    text = when (tabIndex) {
                        0 -> stringResource(R.string.add_placeholder_task)
                        1 -> stringResource(R.string.add_placeholder_memo)
                        else -> stringResource(R.string.add_placeholder_routine)
                    },
                    color = Color(0xFFBDBDBD)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color(0xFFD8D8D8),
                cursorColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8F8F8)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        when (tabIndex) {
            0 -> PriorityPicker(
                selectedPriority = selectedPriority,
                onPrioritySelected = { priority ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    selectedPriority = priority
                }
            )

            1 -> MemoHint()

            else -> RoutinePicker(
                routineType = routineType,
                intervalDays = intervalDays,
                selectedWeekDays = selectedWeekDays,
                weekDayLabels = weekDayLabels,
                onTypeSelected = { type ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    routineType = type
                },
                onIntervalChanged = { intervalDays = it }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                val normalizedTitle = title.trim()
                if (normalizedTitle.isEmpty()) {
                    Toast.makeText(context, titleRequired, Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (tabIndex == 2 && routineType == RoutineType.WEEKLY && selectedWeekDays.isEmpty()) {
                    Toast.makeText(context, weeklyRequired, Toast.LENGTH_SHORT).show()
                    return@Button
                }

                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (tabIndex) {
                    0 -> onSaveTask(normalizedTitle, selectedPriority)
                    1 -> onSaveMemo(normalizedTitle)
                    else -> {
                        val scheduleInput = when (routineType) {
                            RoutineType.DAILY -> RoutineScheduleInput.daily()
                            RoutineType.INTERVAL -> RoutineScheduleInput.interval(intervalDays)
                            RoutineType.WEEKLY -> RoutineScheduleInput.weekly(selectedWeekDays)
                        }
                        onSaveRoutine(normalizedTitle, scheduleInput)
                    }
                }

                Toast.makeText(context, addSuccess, Toast.LENGTH_SHORT).show()
                title = ""
                selectedPriority = TaskPriority.LEVEL_1
                routineType = RoutineType.DAILY
                intervalDays = 2
                selectedWeekDays.clear()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(14.dp),
            enabled = title.isNotBlank()
        ) {
            Text(
                text = stringResource(R.string.button_confirm_add),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF1F2F4))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.Black else Color(0xFF777777),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PriorityPicker(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_priority_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF777777)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TaskPriority.entries.forEach { priority ->
                val selected = selectedPriority == priority
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onPrioritySelected(priority) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = 2.dp,
                                color = if (selected) Color.Black else Color.Transparent,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(priority.colorValue)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = priority.shortName,
                        fontSize = 11.sp,
                        color = if (selected) Color.Black else Color(0xFF666666),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF6FAFF))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.add_memo_hint),
            color = Color(0xFF4D6B82),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RoutinePicker(
    routineType: RoutineType,
    intervalDays: Int,
    selectedWeekDays: MutableList<Int>,
    weekDayLabels: List<String>,
    onTypeSelected: (RoutineType) -> Unit,
    onIntervalChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.add_routine_cycle_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF777777)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F2F4))
                .padding(4.dp)
        ) {
            listOf(
                RoutineType.DAILY to stringResource(R.string.routine_type_daily),
                RoutineType.INTERVAL to stringResource(R.string.routine_type_interval),
                RoutineType.WEEKLY to stringResource(R.string.routine_type_weekly)
            ).forEach { (type, label) ->
                val selected = routineType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color.Black else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeSelected(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else Color(0xFF777777),
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        when (routineType) {
            RoutineType.DAILY -> Text(
                text = stringResource(R.string.routine_daily_hint),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = Color(0xFF777777)
            )

            RoutineType.INTERVAL -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.routine_interval_prefix), fontSize = 15.sp)
                IconButton(onClick = { onIntervalChanged((intervalDays - 1).coerceAtLeast(1)) }) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = intervalDays.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = { onIntervalChanged((intervalDays + 1).coerceAtMost(30)) }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Text(stringResource(R.string.routine_interval_suffix), fontSize = 15.sp)
            }

            RoutineType.WEEKLY -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDayLabels.forEachIndexed { index, label ->
                    val day = index + 1
                    val selected = selectedWeekDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.Black else Color(0xFFF1F2F4))
                            .clickable {
                                if (selected) {
                                    selectedWeekDays.remove(day)
                                } else {
                                    selectedWeekDays.add(day)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else Color(0xFF666666),
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
