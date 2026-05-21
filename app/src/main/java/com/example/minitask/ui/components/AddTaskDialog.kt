package com.example.minitask.ui.components

import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.R
import com.example.minitask.data.model.TaskPriority
import com.example.minitask.domain.memo.MemoColorAllocator
import com.example.minitask.domain.routine.RoutineScheduleInput
import com.example.minitask.domain.routine.RoutineType

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTaskContent(
    memosCountToday: Int,
    onSaveTask: (String, TaskPriority) -> Unit,
    onSaveRoutine: (String, RoutineScheduleInput) -> Unit,
    onSaveMemo: (String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LEVEL_1) }
    var tabIndex by remember { mutableIntStateOf(0) }

    var routineType by remember { mutableStateOf(RoutineType.DAILY) }
    var intervalDays by remember { mutableIntStateOf(2) }
    val selectedWeekDays = remember { mutableStateListOf<Int>() }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current

    val tabLabels = listOf(
        stringResource(R.string.add_tab_task),
        stringResource(R.string.add_tab_memo),
        stringResource(R.string.add_tab_routine)
    )
    val errorTitleRequiredText = stringResource(R.string.error_title_required)
    val errorWeeklyDayRequiredText = stringResource(R.string.error_weekly_day_required)
    val addSuccessText = stringResource(R.string.toast_add_success)
    val weekDayLabels = listOf(
        stringResource(R.string.weekday_mon),
        stringResource(R.string.weekday_tue),
        stringResource(R.string.weekday_wed),
        stringResource(R.string.weekday_thu),
        stringResource(R.string.weekday_fri),
        stringResource(R.string.weekday_sat),
        stringResource(R.string.weekday_sun)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF2F2F2))
                    .padding(4.dp)
            ) {
                Row {
                    tabLabels.forEachIndexed { index, text ->
                        val isSelected = tabIndex == index
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (tabIndex != index) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        tabIndex = index
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                color = if (isSelected) Color.Black else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

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
                    color = Color.LightGray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color(0xFFD0D0D0),
                cursorColor = Color.Black,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8F9FA)
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    val springSpec = spring<IntOffset>(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = springSpec) { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally(animationSpec = springSpec) { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally(animationSpec = springSpec) { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally(animationSpec = springSpec) { width -> width } + fadeOut()
                        )
                    } using SizeTransform(clip = false)
                },
                label = "tab_content_animation"
            ) { currentTab ->
                when (currentTab) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = stringResource(R.string.add_priority_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TaskPriority.entries.forEach { priority ->
                                    val isSelected = selectedPriority == priority
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            selectedPriority = priority
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) Color.Black else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .padding(4.dp)
                                                .clip(CircleShape)
                                                .background(Color(priority.colorValue)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = priority.title,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.Black else Color.DarkGray,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.add_memo_hint),
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    else -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = stringResource(R.string.add_routine_cycle_title),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    listOf(
                                        RoutineType.DAILY to stringResource(R.string.routine_type_daily),
                                        RoutineType.INTERVAL to stringResource(R.string.routine_type_interval),
                                        RoutineType.WEEKLY to stringResource(R.string.routine_type_weekly)
                                    ).forEach { (type, label) ->
                                        val isTypeSelected = routineType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isTypeSelected) Color.Black else Color.Transparent)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    routineType = type
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isTypeSelected) Color.White else Color.Gray,
                                                fontSize = 13.sp,
                                                fontWeight = if (isTypeSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            when (routineType) {
                                RoutineType.INTERVAL -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.routine_interval_prefix),
                                            fontSize = 15.sp,
                                            color = Color.DarkGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        IconButton(
                                            onClick = { if (intervalDays > 1) intervalDays-- },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Remove,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = intervalDays.toString(),
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Black
                                        )
                                        IconButton(
                                            onClick = { if (intervalDays < 30) intervalDays++ },
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Text(
                                            text = stringResource(R.string.routine_interval_suffix),
                                            fontSize = 15.sp,
                                            color = Color.DarkGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                RoutineType.WEEKLY -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        weekDayLabels.forEachIndexed { index, day ->
                                            val dayNum = index + 1
                                            val isSelected = selectedWeekDays.contains(dayNum)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.Black else Color(0xFFF5F5F5))
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        if (isSelected) {
                                                            selectedWeekDays.remove(dayNum)
                                                        } else {
                                                            selectedWeekDays.add(dayNum)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = day,
                                                    color = if (isSelected) Color.White else Color.Gray,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.routine_daily_hint),
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                val normalizedTitle = title.trim()
                if (normalizedTitle.isEmpty()) {
                    Toast.makeText(context, errorTitleRequiredText, Toast.LENGTH_SHORT)
                        .show()
                    return@Button
                }

                if (tabIndex == 2 && routineType == RoutineType.WEEKLY && selectedWeekDays.isEmpty()) {
                    Toast.makeText(
                        context,
                        errorWeeklyDayRequiredText,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                when (tabIndex) {
                    0 -> onSaveTask(normalizedTitle, selectedPriority)
                    1 -> onSaveMemo(normalizedTitle, MemoColorAllocator.nextColorValue(memosCountToday))
                    else -> {
                        val scheduleInput = when (routineType) {
                            RoutineType.INTERVAL -> RoutineScheduleInput.interval(intervalDays)
                            RoutineType.WEEKLY -> RoutineScheduleInput.weekly(selectedWeekDays)
                            RoutineType.DAILY -> RoutineScheduleInput.daily()
                        }
                        onSaveRoutine(normalizedTitle, scheduleInput)
                    }
                }

                Toast.makeText(context, addSuccessText, Toast.LENGTH_SHORT)
                    .show()
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
            shape = RoundedCornerShape(16.dp),
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
