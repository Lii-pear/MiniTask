package com.example.minitask.ui.components

import android.os.SystemClock
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.core.calendar.LunarDateCache
import com.example.minitask.data.model.CalendarMemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.util.LinkedHashMap
import kotlin.math.abs

@Immutable
data class ColorsWrapper(val map: Map<String, Long>)

@Immutable
private data class CalendarDayUiModel(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val memos: List<CalendarMemo>,
    val badgeType: Int?,
    val lunarText: String
)

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

private val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
private const val EXPAND_RICH_CONTENT_DELAY_MS = 320L
private const val COLLAPSE_RICH_CONTENT_DELAY_MS = 220L

private class CalendarPageCellsCache(private val maxSize: Int = 18) {
    private val map = object : LinkedHashMap<YearMonth, List<CalendarDayUiModel>>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<YearMonth, List<CalendarDayUiModel>>?): Boolean {
            return size > maxSize
        }
    }

    fun getOrPut(yearMonth: YearMonth, builder: () -> List<CalendarDayUiModel>): List<CalendarDayUiModel> {
        return map[yearMonth] ?: builder().also { map[yearMonth] = it }
    }

    fun clear() {
        map.clear()
    }
}

private fun buildCalendarPageCells(
    yearMonth: YearMonth,
    memosByDate: Map<LocalDate, List<CalendarMemo>>,
    holidayBadgeMap: Map<LocalDate, Int>
): List<CalendarDayUiModel> {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = firstDayOfMonth.dayOfWeek.value - 1
    val cells = ArrayList<CalendarDayUiModel>(42)

    for (cellIndex in 0 until 42) {
        val dayOffset = cellIndex - startOffset + 1
        val isCurrentMonth = dayOffset in 1..daysInMonth
        val currentDate = when {
            isCurrentMonth -> yearMonth.atDay(dayOffset)
            dayOffset < 1 -> yearMonth.minusMonths(1).atEndOfMonth().plusDays(dayOffset.toLong())
            else -> yearMonth.plusMonths(1).atDay(dayOffset - daysInMonth)
        }

        cells.add(
            CalendarDayUiModel(
                date = currentDate,
                isCurrentMonth = isCurrentMonth,
                memos = memosByDate[currentDate].orEmpty(),
                badgeType = holidayBadgeMap[currentDate],
                lunarText = if (isCurrentMonth) LunarDateCache.peek(currentDate).orEmpty() else ""
            )
        )
    }
    return cells
}

private fun fillCalendarPageLunarText(cells: List<CalendarDayUiModel>): List<CalendarDayUiModel> {
    var hasUpdate = false
    val updated = cells.map { cell ->
        if (cell.isCurrentMonth && cell.lunarText.isEmpty()) {
            hasUpdate = true
            cell.copy(lunarText = LunarDateCache.getText(cell.date))
        } else {
            cell
        }
    }
    return if (hasUpdate) updated else cells
}

private fun smoothStep(value: Float): Float {
    return value * value * (3f - 2f * value)
}

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
    val gestureThreshold = 56f
    val switchCooldownMs = 220L

    val initialPage = 10_000
    val pageCount = 20_001
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })
    val coroutineScope = rememberCoroutineScope()
    val pageCellsCache = remember { CalendarPageCellsCache() }
    val wrappedColors = remember(dynamicMemoColors) { ColorsWrapper(dynamicMemoColors) }
    val today = remember { LocalDate.now() }

    val modeTransition = updateTransition(targetState = isExpanded, label = "calendarMode")
    val nonTargetRowsProgress by modeTransition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 260, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 180, easing = FastOutSlowInEasing)
            }
        },
        label = "nonTargetRowsProgress"
    ) { expanded -> if (expanded) 1f else 0f }
    val headerLiftProgress by modeTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 240, easing = FastOutSlowInEasing) },
        label = "headerLiftProgress"
    ) { expanded -> if (expanded) 1f else 0f }
    val headerTranslationPx = with(LocalDensity.current) { 4.dp.toPx() }
    val rowTranslationBasePx = with(LocalDensity.current) { 14.dp.toPx() }

    var anchorDate by remember { mutableStateOf(selectedDate) }
    var anchorPage by remember { mutableIntStateOf(initialPage) }
    var previousExpanded by remember { mutableStateOf(isExpanded) }
    var lastSwitchUptimeMs by remember { mutableLongStateOf(0L) }
    var pageDataVersion by remember { mutableIntStateOf(0) }
    var isModeSwitchLocked by remember { mutableStateOf(false) }
    var richContentMode by remember { mutableStateOf(isExpanded) }
    var shouldRenderRichContent by remember { mutableStateOf(true) }

    LaunchedEffect(memosByDate, holidayBadgeMap) {
        pageDataVersion++
        pageCellsCache.clear()
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded != richContentMode) {
            shouldRenderRichContent = false
            delay(if (isExpanded) EXPAND_RICH_CONTENT_DELAY_MS else COLLAPSE_RICH_CONTENT_DELAY_MS)
            richContentMode = isExpanded
            shouldRenderRichContent = true
        } else {
            shouldRenderRichContent = true
        }
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded != previousExpanded) {
            isModeSwitchLocked = true
            val settledPage = pagerState.settledPage
            val pageOffset = settledPage - anchorPage
            anchorDate = if (previousExpanded) {
                anchorDate.plusMonths(pageOffset.toLong())
            } else {
                anchorDate.plusWeeks(pageOffset.toLong())
            }
            anchorPage = settledPage
            previousExpanded = isExpanded
            delay(180)
            isModeSwitchLocked = false
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val offset = pagerState.settledPage - anchorPage
        if (offset == 0) return@LaunchedEffect

        val rawDate = if (isExpanded) {
            anchorDate.plusMonths(offset.toLong())
        } else {
            anchorDate.plusWeeks(offset.toLong())
        }

        val nextDate = if (isExpanded) {
            if (YearMonth.from(rawDate) == YearMonth.from(today)) today else rawDate
        } else {
            val rawMonday = rawDate.minusDays(rawDate.dayOfWeek.value.toLong() - 1)
            val todayMonday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            if (rawMonday == todayMonday) today else rawDate
        }

        anchorDate = nextDate
        anchorPage = pagerState.settledPage
        onDateSelected(nextDate)
    }

    val settledOffset = pagerState.settledPage - anchorPage
    val activeDate = if (isExpanded) {
        anchorDate.plusMonths(settledOffset.toLong())
    } else {
        anchorDate.plusWeeks(settledOffset.toLong())
    }
    val viewYearMonth = YearMonth.from(activeDate)
    val headerYearMonth = if (isExpanded) viewYearMonth else YearMonth.from(selectedDate)
    val canSwitchMode = !isModeSwitchLocked &&
        !pagerState.isScrollInProgress &&
        pagerState.currentPage == pagerState.settledPage

    LaunchedEffect(viewYearMonth, shouldRenderRichContent) {
        if (shouldRenderRichContent) {
            launch(Dispatchers.Default) { LunarDateCache.prewarmMonth(viewYearMonth.minusMonths(1)) }
            launch(Dispatchers.Default) { LunarDateCache.prewarmMonth(viewYearMonth) }
            launch(Dispatchers.Default) { LunarDateCache.prewarmMonth(viewYearMonth.plusMonths(1)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = 16.dp)
            .pointerInput(isExpanded) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount -> totalDrag += dragAmount },
                    onDragEnd = {
                        val isPagerBusy = pagerState.isScrollInProgress || pagerState.currentPage != pagerState.settledPage
                        if (isModeSwitchLocked || isPagerBusy) {
                            totalDrag = 0f
                            return@detectVerticalDragGestures
                        }

                        val now = SystemClock.uptimeMillis()
                        if (now - lastSwitchUptimeMs < switchCooldownMs) {
                            totalDrag = 0f
                            return@detectVerticalDragGestures
                        }

                        if (totalDrag > gestureThreshold && !isExpanded) {
                            onExpandedChange(true)
                            lastSwitchUptimeMs = now
                        } else if (totalDrag < -gestureThreshold && isExpanded) {
                            onExpandedChange(false)
                            lastSwitchUptimeMs = now
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                )
            }
    ) {
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
                ) {
                    if (canSwitchMode) {
                        onExpandedChange(!isExpanded)
                        lastSwitchUptimeMs = SystemClock.uptimeMillis()
                    }
                }
                .graphicsLayer {
                    val headerEase = smoothStep(headerLiftProgress)
                    val subtleScale = 0.985f + 0.015f * headerEase
                    scaleX = subtleScale
                    scaleY = subtleScale
                    translationY = (1f - headerEase) * headerTranslationPx
                }
            ) {
                Text(
                    text = headerYearMonth.monthValue.toString(),
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
                        onDateSelected(today)
                        anchorDate = today
                        anchorPage = initialPage
                        coroutineScope.launch { pagerState.scrollToPage(initialPage) }
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TODAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 0
            ) { page ->
                val pageOffset = page - anchorPage
                val pageBaseDate = if (isExpanded) {
                    anchorDate.plusMonths(pageOffset.toLong())
                } else {
                    anchorDate.plusWeeks(pageOffset.toLong())
                }

                val yearMonth = remember(pageBaseDate) { YearMonth.from(pageBaseDate) }
                val firstDayOffset = remember(yearMonth) { yearMonth.atDay(1).dayOfWeek.value - 1 }
                val targetRow = remember(pageBaseDate, firstDayOffset) {
                    ((pageBaseDate.dayOfMonth + firstDayOffset - 1) / 7).coerceIn(0, 5)
                }

                val basePageCells = remember(yearMonth, pageDataVersion) {
                    pageCellsCache.getOrPut(yearMonth) {
                        buildCalendarPageCells(
                            yearMonth = yearMonth,
                            memosByDate = memosByDate,
                            holidayBadgeMap = holidayBadgeMap
                        )
                    }
                }
                val pageCells = if (shouldRenderRichContent) {
                    val filledPageCells by produceState(
                        initialValue = basePageCells,
                        key1 = yearMonth,
                        key2 = basePageCells
                    ) {
                        value = withContext(Dispatchers.Default) {
                            fillCalendarPageLunarText(basePageCells)
                        }
                    }
                    filledPageCells
                } else {
                    basePageCells
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    for (row in 0..5) {
                        val isTargetRow = row == targetRow
                        val isNonTargetVisible = isExpanded || nonTargetRowsProgress > 0.01f
                        if (!isTargetRow && !isNonTargetVisible) continue

                        val rowModifier = if (isTargetRow) {
                            Modifier.fillMaxWidth()
                        } else {
                            val distance = abs(row - targetRow)
                            val staggerStart = (distance * 0.12f).coerceAtMost(0.45f)
                            val rawProgress = ((nonTargetRowsProgress - staggerStart) / (1f - staggerStart))
                                .coerceIn(0f, 1f)
                            val easedProgress = smoothStep(rawProgress)
                            val direction = if (row < targetRow) -1f else 1f

                            Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = easedProgress
                                    scaleY = 0.96f + 0.04f * easedProgress
                                    translationY = direction * rowTranslationBasePx * (1f - easedProgress)
                                }
                        }

                        Row(modifier = rowModifier) {
                            for (col in 0 until 7) {
                                val cell = pageCells[row * 7 + col]
                                val isSelected = cell.date == selectedDate
                                CalendarCell(
                                    date = cell.date,
                                    isCurrentMonth = cell.isCurrentMonth,
                                    isSelected = isSelected,
                                    memos = cell.memos,
                                    lunarText = cell.lunarText,
                                    wrappedColors = wrappedColors,
                                    badgeType = cell.badgeType,
                                    shouldRenderRichContent = shouldRenderRichContent,
                                    modifier = Modifier.weight(1f),
                                    onDateClick = { onDateSelected(cell.date) },
                                    onMemoClick = {
                                        if (isSelected) onMemoAreaSelected(cell.date) else onDateSelected(cell.date)
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
private fun CalendarCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    memos: List<CalendarMemo>,
    lunarText: String,
    wrappedColors: ColorsWrapper,
    badgeType: Int?,
    shouldRenderRichContent: Boolean,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit,
    onMemoClick: () -> Unit
) {
    val isToday = remember(date) { date == LocalDate.now() }
    val bgColor = if (isSelected) Color(0xFFF0F0F0) else Color.Transparent
    val dateTextColor = when {
        isToday && isCurrentMonth -> Color(0xFFFF5252)
        isCurrentMonth -> Color.Black
        else -> Color.LightGray.copy(alpha = 0.5f)
    }
    val lunarTextColor = if (shouldRenderRichContent && isCurrentMonth) Color.Gray else Color.Transparent
    val visibleMemos = if (shouldRenderRichContent) memos else emptyList()
    val visibleBadgeType = if (shouldRenderRichContent) badgeType else null

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
        if (isCurrentMonth && visibleBadgeType != null) {
            val isRest = visibleBadgeType == 1
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (isRest) Color(0xFFFF5252).copy(alpha = 0.8f) else Color(0xFF9E9E9E).copy(alpha = 0.8f)
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
                text = if (shouldRenderRichContent) lunarText else "",
                fontSize = 8.5.sp,
                color = lunarTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                visibleMemos.take(maxDisplay).forEachIndexed { index, memo ->
                    val overrideColor = wrappedColors.map[memo.id] ?: memo.colorValue
                    if (index == maxDisplay - 1 && visibleMemos.size > maxDisplay) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            MemoMiniBlock(memo = memo, overrideColor = overrideColor)
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 1.dp, end = 1.dp)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF757575))
                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                            ) {
                                Text(
                                    text = "+${visibleMemos.size - maxDisplay + 1}",
                                    fontSize = 7.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = staticBadgeTextStyle
                                )
                            }
                        }
                    } else {
                        MemoMiniBlock(memo = memo, overrideColor = overrideColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoMiniBlock(memo: CalendarMemo, overrideColor: Long) {
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
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(10.dp)
                    .background(indicatorColor)
            )
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
