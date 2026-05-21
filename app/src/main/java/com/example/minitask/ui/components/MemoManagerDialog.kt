package com.example.minitask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.R
import com.example.minitask.data.model.CalendarMemo
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoManagerDialog(
    selectedDate: LocalDate,
    currentMemos: List<CalendarMemo>,
    onDismiss: () -> Unit,
    onAddMemo: (String) -> Unit,
    onTogglePin: (CalendarMemo) -> Unit,
    onToggleComplete: (CalendarMemo) -> Unit,
    onDelete: (CalendarMemo) -> Unit
) {
    var newMemoTitle by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val sortedMemos = remember(currentMemos) { currentMemos.sortedBy { it.orderWeight } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
        contentAlignment = Alignment.Center
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = {
                Text(
                    text = stringResource(
                        R.string.memo_dialog_title,
                        selectedDate.monthValue,
                        selectedDate.dayOfMonth
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        itemsIndexed(sortedMemos, key = { _, item -> item.id }) { _, memo ->
                            val isPinned = memo.orderWeight == 0L
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isPinned) Color(0xFFFFF9C4) else Color.Transparent)
                                    .padding(start = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onToggleComplete(memo) }) {
                                    Icon(
                                        imageVector = if (memo.isCompleted) {
                                            Icons.Default.CheckCircle
                                        } else {
                                            Icons.Default.RadioButtonUnchecked
                                        },
                                        contentDescription = stringResource(R.string.cd_complete),
                                        tint = if (memo.isCompleted) Color(0xFF4CAF50) else Color.LightGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = memo.title,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp),
                                    fontSize = 15.sp,
                                    color = if (memo.isCompleted) Color.Gray else Color.Black,
                                    textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else null
                                )

                                IconButton(
                                    onClick = { onTogglePin(memo) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = stringResource(R.string.cd_pin),
                                        tint = if (isPinned) Color(0xFFFFAB40) else Color.LightGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDelete(memo) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete),
                                        tint = Color(0xFFFF5252).copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (currentMemos.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFFEEEEEE)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newMemoTitle,
                            onValueChange = { newMemoTitle = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.memo_placeholder_new),
                                    fontSize = 14.sp,
                                    color = Color.LightGray
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Black,
                                unfocusedBorderColor = Color(0xFFEEEEEE)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val normalized = newMemoTitle.trim()
                                if (normalized.isNotEmpty()) {
                                    onAddMemo(normalized)
                                    newMemoTitle = ""
                                    focusManager.clearFocus()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            enabled = newMemoTitle.isNotBlank()
                        ) {
                            Text(
                                text = stringResource(R.string.button_add),
                                color = Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.button_close),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
