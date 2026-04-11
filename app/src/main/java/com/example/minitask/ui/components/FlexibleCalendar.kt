package com.example.minitask.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.data.model.CalendarMemo
import com.nlf.calendar.Solar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

@Immutable
data class MemosWrapper(val list: List<CalendarMemo>)

@Immutable
data class ColorsWrapper(val map: Map<String, Long>)

object LunarDateCache {
    private val cache = ConcurrentHashMap<LocalDate, String>()

    fun getText(date: LocalDate): String {
        return cache.getOrPut(date) {
            val solar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.lunar
            val solarFestivals = solar.festivals
            if (solarFestivals.isNotEmpty()) return@getOrPut solarFestivals[0]
            val lunarFestivals = lunar.festivals
            if (lunarFestivals.isNotEmpty()) return@getOrPut lunarFestivals[0]
            val jieQi = lunar.jieQi
            if (jieQi.isNotEmpty()) return@getOrPut jieQi
            if (lunar.day == 1) return@getOrPut lunar.monthInChinese + "月"
            lunar.dayInChinese
        }
    }
}

private val staticMemoTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

private val staticBadgeTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FlexibleCalendarGrid(
    selectedDate: LocalDate,
    memosByDate: Map<LocalDate, List<CalendarMemo>>,
    dynamicMemoColors: Map<String, Long>,
    holidayBadgeMap: Map<LocalDate, Int>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMemoAreaSelected: (LocalDate) -> Unit
) {
    val initialPage = 50000
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 100000 })
    val coroutineScope = rememberCoroutineScope()
    val wrappedColors = remember(dynamicMemoColors) { ColorsWrapper(dynamicMemoColors) }

    var anchorDate by remember { mutableStateOf(selectedDate) }
    var anchorPage by remember { mutableIntStateOf(initialPage) }
    var previousIsExpanded by remember { mutableStateOf(isExpanded) }

    // ★ 稳定回退：保留了最稳定的状态同步逻辑
    LaunchedEffect(isExpanded) {
        if (isExpanded != previousIsExpanded) {
            val currentOffset = pagerState.currentPage - anchorPage
            val activeDate = if (previousIsExpanded) {
                anchorDate.plusMonths(currentOffset.toLong())
            } else {
                anchorDate.plusWeeks(currentOffset.toLong())
            }
            anchorDate = activeDate
            anchorPage = pagerState.currentPage
            previousIsExpanded = isExpanded
        }
    }

    // ★ 智能寻路：保留了“滑入本周自动选中今天”的好用功能
    LaunchedEffect(pagerState.settledPage) {
        val offset = pagerState.settledPage - anchorPage
        if (offset != 0) {
            val rawDate = if (isExpanded) {
                anchorDate.plusMonths(offset.toLong())
            } else {
                anchorDate.plusWeeks(offset.toLong())
            }

            val today = LocalDate.now()
            val finalDate = if (isExpanded) {
                if (YearMonth.from(rawDate) == YearMonth.from(today)) today else rawDate
            } else {
                val rawMonday = rawDate.minusDays(rawDate.dayOfWeek.value.toLong() - 1)
                val todayMonday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                if (rawMonday == todayMonday) today else rawDate
            }

            anchorDate = finalDate
            anchorPage = pagerState.settledPage
            onDateSelected(finalDate)
        }
    }

    val currentOffset = pagerState.currentPage - anchorPage
    val activeDate =
        if (isExpanded) anchorDate.plusMonths(currentOffset.toLong()) else anchorDate.plusWeeks(
            currentOffset.toLong()
        )
    val viewYearMonth = YearMonth.from(activeDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .animateContentSize(animationSpec = tween(350, easing = FastOutSlowInEasing))
            .padding(top = 16.dp)
            // ★ 稳定回退：使用系统原生自带的手势探测，放弃底层强拦截，保证绝对稳定
            .pointerInput(isExpanded) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag > 50 && !isExpanded) {
                            onExpandedChange(true)
                            totalDrag = 0f
                        } else if (totalDrag < -50 && isExpanded) {
                            onExpandedChange(false)
                            totalDrag = 0f
                        }
                    }
                )
            }
    ) {
        // --- 顶部操作栏 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandedChange(!isExpanded) }) {
                Text(
                    text = viewYearMonth.monthValue.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(32.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(20.dp))
                    .clickable {
                        val today = LocalDate.now()
                        onDateSelected(today)
                        anchorDate = today
                        anchorPage = initialPage
                        coroutineScope.launch { pagerState.scrollToPage(initialPage) }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp), contentAlignment = Alignment.Center
            ) {
                Text(
                    "TODAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }

        // --- 星期头 ---
        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // --- 核心日历网格 ---
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val pageOffset = page - anchorPage
            val pageBaseDate = if (isExpanded) {
                anchorDate.plusMonths(pageOffset.toLong())
            } else {
                anchorDate.plusWeeks(pageOffset.toLong())
            }

            val yearMonth = YearMonth.from(pageBaseDate)
            val firstDayOfMonth = yearMonth.atDay(1)
            val daysInMonth = yearMonth.lengthOfMonth()
            val startOffset = firstDayOfMonth.dayOfWeek.value - 1

            Column(modifier = Modifier.fillMaxWidth()) {
                val targetRow = run {
                    val dayOffset = pageBaseDate.dayOfMonth + startOffset - 1
                    (dayOffset / 7).coerceIn(0, 5)
                }

                for (row in 0..5) {
                    // ★ 核心回退：使用最稳健的 AnimatedVisibility，无论怎么切，绝不会让周历隐形！
                    AnimatedVisibility(
                        visible = isExpanded || row == targetRow,
                        enter = fadeIn(tween(200)) + expandVertically(
                            tween(
                                350,
                                easing = FastOutSlowInEasing
                            )
                        ),
                        exit = fadeOut(tween(150)) + shrinkVertically(
                            tween(
                                300,
                                easing = FastOutSlowInEasing
                            )
                        )
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayOffset = cellIndex - startOffset + 1
                                val isCurrentMonth = dayOffset in 1..daysInMonth
                                val currentDate =
                                    if (isCurrentMonth) yearMonth.atDay(dayOffset) else if (dayOffset < 1) yearMonth.minusMonths(
                                        1
                                    ).atEndOfMonth()
                                        .plusDays(dayOffset.toLong()) else yearMonth.plusMonths(1)
                                        .atDay(dayOffset - daysInMonth)

                                val isSelected = currentDate == selectedDate
                                val rawMemos = remember(
                                    memosByDate,
                                    currentDate
                                ) {
                                    memosByDate[currentDate]?.sortedBy { it.orderWeight }
                                        ?: emptyList()
                                }
                                val wrappedMemos = remember(rawMemos) { MemosWrapper(rawMemos) }
                                val badgeType = remember(
                                    holidayBadgeMap,
                                    currentDate
                                ) { holidayBadgeMap[currentDate] }

                                CalendarCell(
                                    date = currentDate,
                                    isCurrentMonth = isCurrentMonth,
                                    isSelected = isSelected,
                                    wrappedMemos = wrappedMemos,
                                    wrappedColors = wrappedColors,
                                    badgeType = badgeType,
                                    modifier = Modifier.weight(1f),
                                    onDateClick = { onDateSelected(currentDate) },
                                    onMemoClick = {
                                        if (isSelected) onMemoAreaSelected(currentDate) else onDateSelected(
                                            currentDate
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    wrappedMemos: MemosWrapper,
    wrappedColors: ColorsWrapper,
    badgeType: Int?,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit,
    onMemoClick: () -> Unit
) {
    val isToday = remember(date) { date == LocalDate.now() }
    val smartDateText = LunarDateCache.getText(date)

    val bgColor = if (isSelected) Color(0xFFF0F0F0) else Color.Transparent
    val dateTextColor =
        if (isToday && isCurrentMonth) Color(0xFFFF5252) else if (isCurrentMonth) Color.Black else Color.LightGray.copy(
            alpha = 0.5f
        )
    val lunarTextColor = if (!isCurrentMonth) Color.Transparent else Color.Gray

    Box(
        modifier = modifier
            .height(110.dp)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDateClick() }
            .padding(1.dp)
    ) {
        if (isCurrentMonth && badgeType != null) {
            val isRest = badgeType == 1
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (isRest) Color(0xFFFF5252).copy(alpha = 0.8f) else Color(
                            0xFF9E9E9E
                        ).copy(alpha = 0.8f)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isRest) "休" else "班",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = staticBadgeTextStyle
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = dateTextColor
            )

            Text(
                text = smartDateText,
                fontSize = 8.5.sp,
                color = lunarTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // ★ 保留：使用 Ellipsis 防卡顿
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onMemoClick() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val maxDisplay = 4
                wrappedMemos.list.take(maxDisplay).forEachIndexed { index, memo ->
                    val overrideColor = wrappedColors.map[memo.id] ?: memo.colorValue

                    if (index == maxDisplay - 1 && wrappedMemos.list.size > maxDisplay) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            MemoMiniBlock(memo, overrideColor = overrideColor)
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 1.dp, end = 1.dp)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF757575))
                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "+${wrappedMemos.list.size - maxDisplay + 1}",
                                    fontSize = 7.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = staticBadgeTextStyle
                                )
                            }
                        }
                    } else {
                        MemoMiniBlock(memo, overrideColor = overrideColor)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoMiniBlock(memo: CalendarMemo, overrideColor: Long) {
    val softBgColor = Color(overrideColor).copy(alpha = 0.25f)
    val indicatorColor = Color(overrideColor).copy(alpha = 0.9f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp, start = 2.dp, end = 2.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(softBgColor)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .width(2.5.dp)
                .height(10.dp)
                .background(indicatorColor))
            Text(
                text = memo.title,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                fontSize = 8.sp,
                color = Color(0xFF444444),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                style = staticMemoTextStyle
            )
        }
    }
}