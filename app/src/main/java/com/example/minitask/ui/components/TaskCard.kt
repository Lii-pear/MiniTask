package com.example.minitask.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minitask.data.model.DailyRoutine
import com.example.minitask.data.model.DailyTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ParticleStatus(
    val velocityX: Float,
    val velocityY: Float,
    val initialSize: Float,
    val color: Color
)

@Composable
fun rememberParticles(count: Int = 12): List<ParticleStatus> {
    return remember(count) {
        val random = Random()
        val softColors = listOf(
            Color(0xFF81D4FA),
            Color(0xFFA5D6A7),
            Color(0xFFFFCC80),
            Color(0xFFF48FB1),
            Color(0xFFFFF59D),
            Color(0xFFE0E0E0)
        )
        List(count) {
            val angle = (random.nextFloat() * 2 * PI).toFloat()
            val speed = random.nextFloat() * 24f + 12f
            ParticleStatus(
                velocityX = speed * cos(angle),
                velocityY = speed * sin(angle) - 10f,
                initialSize = random.nextFloat() * 4f + 3f,
                color = softColors[random.nextInt(softColors.size)]
            )
        }
    }
}

@Composable
fun ExplodingParticle(status: ParticleStatus, progress: Float) {
    val translationX = status.velocityX * progress * 2.8f
    val translationY = (status.velocityY * progress * 2.8f) + (progress * progress * 120f)
    val scale = (1f - progress * 0.95f).coerceAtLeast(0f)
    val alpha = (1f - progress * 1.2f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .size(status.initialSize.dp)
            .graphicsLayer {
                this.translationX = translationX
                this.translationY = translationY
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .background(status.color, CircleShape)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineChip(
    routine: DailyRoutine,
    isDone: Boolean,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    var localIsDone by remember(routine.id, isDone) { mutableStateOf(isDone) }
    var isProcessing by remember { mutableStateOf(false) }
    var isExploding by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val particles = rememberParticles()
    val interactionSource = remember { MutableInteractionSource() }

    val displayIsDone = localIsDone
    val bgColor by animateColorAsState(if (displayIsDone) Color(0xFFFAFAFA) else Color.White, label = "")
    val borderColor by animateColorAsState(
        if (displayIsDone) Color.Transparent else Color(0xFFE8E8E8),
        label = ""
    )
    val contentColor by animateColorAsState(
        if (displayIsDone) Color.LightGray else Color(0xFF424242),
        label = ""
    )
    val explosionProgress by animateFloatAsState(
        targetValue = if (isExploding) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "chipExplosion"
    )

    LaunchedEffect(isExploding) {
        if (isExploding) {
            delay(300)
            onLongClick()
        }
    }

    Box(
        modifier = modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (!isProcessing && !isExploding) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    localIsDone = !localIsDone
                    isProcessing = true
                    coroutineScope.launch {
                        delay(120)
                        onClick()
                        isProcessing = false
                    }
                }
            },
            onLongClick = {
                if (!isProcessing && !isExploding) {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    isProcessing = true
                    isExploding = true
                }
            }
        )
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isExploding) Color.Transparent else bgColor)
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
        )

        Text(
            text = routine.title,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (displayIsDone) TextDecoration.LineThrough else null,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .alpha(if (isExploding) 0f else 1f)
        )

        if (explosionProgress > 0f) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                particles.forEach { particle ->
                    ExplodingParticle(status = particle, progress = explosionProgress)
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: DailyTask,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var localIsDone by remember(task.id, task.isCompleted) { mutableStateOf(task.isCompleted) }
    var isProcessing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val isDone = localIsDone
    val titleAlpha by animateFloatAsState(if (isDone) 0.4f else 1f, label = "")
    val bgSurfaceColor by animateColorAsState(
        if (task.isPinned) Color(0xFFFFFDE7) else Color.White,
        label = ""
    )
    val borderColor by animateColorAsState(
        if (task.isPinned) Color(0xFFFFD54F).copy(alpha = 0.3f) else Color(0xFFF0F0F0),
        label = ""
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = bgSurfaceColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDone) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = !isProcessing) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    localIsDone = !localIsDone
                    isProcessing = true
                    coroutineScope.launch {
                        delay(120)
                        onClick()
                        isProcessing = false
                    }
                }
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    text = "全天",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDone) Color.LightGray else Color(0xFF333333)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            Color(task.priority.colorValue).copy(alpha = if (isDone) 0.05f else 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.priority.shortName,
                        fontSize = 9.sp,
                        color = if (isDone) Color.LightGray else Color(task.priority.colorValue),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(width = 3.dp, height = 28.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isDone) Color(0xFFEEEEEE) else Color(task.priority.colorValue).copy(alpha = 0.8f)
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = titleAlpha),
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onPinClick()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (task.isPinned) Color(0xFFFFAB40) else Color(0xFFDDDDDD),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeleteClick()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFDDDDDD),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .border(
                            if (isDone) 0.dp else 1.5.dp,
                            if (isDone) Color.Transparent else Color(0xFFE0E0E0),
                            CircleShape
                        )
                        .background(
                            if (isDone) Color(0xFF4CAF50) else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
