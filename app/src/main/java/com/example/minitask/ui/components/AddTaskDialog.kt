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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.data.model.TaskPriority
import java.time.LocalDate

val memoColorValues = listOf(
    0xFF81D4FA, 0xFFA5D6A7, 0xFFFFCC80, 0xFFF48FB1,
    0xFFFFF59D, 0xFFB0BEC5, 0xFFB39DDB, 0xFFFFAB91,
    0xFF80CBC4, 0xFF9FA8DA, 0xFFE6EE9C, 0xFFBCAAA4
)

fun getNextMemoColorValue(currentCount: Int): Long {
    return memoColorValues[currentCount % memoColorValues.size]
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTaskContent(
    memosCountToday: Int,
    onSaveTask: (String, TaskPriority) -> Unit,
    onSaveRoutine: (String, String, String, LocalDate) -> Unit,
    onSaveMemo: (String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(TaskPriority.LEVEL_1) }
    var tabIndex by remember { mutableStateOf(0) }

    // --- 每日必做专属状态 ---
    var routineType by remember { mutableStateOf("DAILY") } // DAILY, INTERVAL, WEEKLY
    var intervalDays by remember { mutableIntStateOf(2) }
    val selectedWeekDays = remember { mutableStateListOf<Int>() } // 1..7 代表周一..周日

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 16.dp)
    ) {
        // --- 1. 胶囊状 Tab 切换 ---
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
                    listOf("待办任务", "月历备忘", "每日必做").forEachIndexed { index, text ->
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

        // --- 2. 输入框 ---
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = {
                Text(
                    text = when (tabIndex) {
                        0 -> "准备做什么？"
                        1 -> "记点什么备忘？"
                        else -> "养成什么好习惯？"
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

        // --- 3. 核心内容区 ---
        // ★ 优化 1：高度从 140dp 放宽到 160dp，让内容有更多呼吸空间
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)) {
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
                    0 -> { // 待办任务：优先级选择
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "优先级设置",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TaskPriority.values().forEach { priority ->
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

                    1 -> Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📌 备忘内容将同步到今日任务列表中\n并显示在日历打点",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }

                    2 -> { // 每日必做：频率选择
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "重复周期",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // ★ 优化 2：二级切换器改成【填满全宽、均匀分布】的现代卡片
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5))
                                    .padding(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    mapOf(
                                        "DAILY" to "每天",
                                        "INTERVAL" to "隔几天",
                                        "WEEKLY" to "每周"
                                    ).forEach { (type, label) ->
                                        val isTypeSelected = routineType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f) // 平分宽度，告别拥挤
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isTypeSelected) Color.Black else Color.Transparent)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    view.performHapticFeedback(
                                                        HapticFeedbackConstants.KEYBOARD_TAP
                                                    )
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

                            // 根据类型显示具体设置
                            when (routineType) {
                                "INTERVAL" -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "每 ",
                                            fontSize = 15.sp,
                                            color = Color.DarkGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                        IconButton(
                                            onClick = { if (intervalDays > 2) intervalDays-- },
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
                                            " 天一次",
                                            fontSize = 15.sp,
                                            color = Color.DarkGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                "WEEKLY" -> {
                                    // ★ 优化 3：星期选择器填满全宽，间距均匀拉开，圆圈稍稍放大
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        listOf(
                                            "一",
                                            "二",
                                            "三",
                                            "四",
                                            "五",
                                            "六",
                                            "日"
                                        ).forEachIndexed { i, day ->
                                            val dayNum = i + 1
                                            val isDaySelected = selectedWeekDays.contains(dayNum)
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp) // 尺寸从32放大到36，触控更精准
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isDaySelected) Color.Black else Color(
                                                            0xFFF5F5F5
                                                        )
                                                    )
                                                    .clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        if (isDaySelected) selectedWeekDays.remove(
                                                            dayNum
                                                        ) else selectedWeekDays.add(dayNum)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = day,
                                                    color = if (isDaySelected) Color.White else Color.Gray,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isDaySelected) FontWeight.Bold else FontWeight.Medium
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
                                            "习惯每天都会出现在你的任务清单中",
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
                if (title.isNotBlank()) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    when (tabIndex) {
                        0 -> onSaveTask(title, selectedPriority)
                        1 -> onSaveMemo(title, getNextMemoColorValue(memosCountToday))
                        2 -> {
                            val repeatValue = when (routineType) {
                                "INTERVAL" -> intervalDays.toString()
                                "WEEKLY" -> selectedWeekDays.sorted().joinToString(",")
                                else -> ""
                            }
                            onSaveRoutine(title, routineType, repeatValue, LocalDate.now())
                        }
                    }
                    Toast.makeText(context, "添加成功！", Toast.LENGTH_SHORT).show()
                    title = ""
                    selectedPriority = TaskPriority.LEVEL_1
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            enabled = title.isNotBlank()
        ) {
            Text("确定添加", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}