package com.example.minitask.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.domain.calendar.CalendarDayCell
import com.example.minitask.domain.calendar.CalendarDisplayMode
import java.time.LocalDate
import java.time.YearMonth

private val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

@Composable
fun LightweightCalendar(
    selectedDate: LocalDate,
    visibleMonth: YearMonth,
    mode: CalendarDisplayMode,
    cells: List<CalendarDayCell>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggleMode: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMemoAreaSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = remember(visibleMonth) {
        "${visibleMonth.year}年${visibleMonth.monthValue}月"
    }
    val rows = remember(cells) {
        if (cells.isEmpty()) emptyList() else cells.chunked(7)
    }
    val cellHeight = if (mode == CalendarDisplayMode.MONTH) 58.dp else 68.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    ) {
        CalendarHeader(
            title = title,
            isMonthMode = mode == CalendarDisplayMode.MONTH,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday,
            onToggleMode = onToggleMode
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            weekDayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A8A),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    CalendarCell(
                        cell = cell,
                        isSelected = cell.date == selectedDate,
                        height = cellHeight,
                        modifier = Modifier.weight(1f),
                        onDateSelected = onDateSelected,
                        onMemoAreaSelected = onMemoAreaSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    title: String,
    isMonthMode: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onToggleMode: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一段",
                tint = Color.Black
            )
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )

        TextButton(onClick = onToday) {
            Text(
                text = "今天",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        IconButton(onClick = onToggleMode, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (isMonthMode) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = "切换周月视图",
                tint = Color.Black
            )
        }

        IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一段",
                tint = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CalendarCell(
    cell: CalendarDayCell,
    isSelected: Boolean,
    height: Dp,
    onDateSelected: (LocalDate) -> Unit,
    onMemoAreaSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = when {
        isSelected -> Color.White
        cell.isToday -> Color(0xFFE53935)
        cell.isCurrentMonth -> Color.Black
        else -> Color(0xFFBDBDBD)
    }
    val background = when {
        isSelected -> Color.Black
        cell.isToday -> Color(0xFFFFEBEE)
        else -> Color.Transparent
    }
    val borderColor = if (cell.marker.hasContent && !isSelected) {
        Color(0xFFE8E8E8)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .height(height)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    if (isSelected) {
                        onMemoAreaSelected(cell.date)
                    } else {
                        onDateSelected(cell.date)
                    }
                },
                onLongClick = { onMemoAreaSelected(cell.date) }
            )
            .padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(5.dp))

            MarkerRow(
                marker = cell.marker,
                selected = isSelected
            )
        }
    }
}

@Composable
private fun MarkerRow(
    marker: com.example.minitask.domain.calendar.CalendarDayMarker,
    selected: Boolean
) {
    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (marker.taskCount > 0) {
            Surface(
                shape = CircleShape,
                color = if (selected) Color.White else Color.Black,
                modifier = Modifier.size(6.dp)
            ) {}
        }

        marker.memoColors.forEach { colorValue ->
            Spacer(modifier = Modifier.size(3.dp))
            Surface(
                shape = CircleShape,
                color = if (selected) Color.White.copy(alpha = 0.85f) else Color(colorValue),
                modifier = Modifier.size(6.dp)
            ) {}
        }

        if (marker.memoCount > marker.memoColors.size) {
            Spacer(modifier = Modifier.size(3.dp))
            Text(
                text = "+",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else Color(0xFF757575)
            )
        }
    }
}
