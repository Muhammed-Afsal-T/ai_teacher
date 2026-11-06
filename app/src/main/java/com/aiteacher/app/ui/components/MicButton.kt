package com.aiteacher.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aiteacher.app.ui.theme.CyanAccent
import com.aiteacher.app.ui.theme.IndigoAccent
import com.aiteacher.app.ui.theme.Navy950
import com.aiteacher.app.ui.theme.PatienceRed
import com.aiteacher.app.ui.theme.PureWhite

@Composable
fun MicButton(
    isRecording: Boolean,
    enabled: Boolean = true,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnStart = rememberUpdatedState(onStartRecording)
    val currentOnStop = rememberUpdatedState(onStopRecording)
    val currentOnCancel = rememberUpdatedState(onCancelRecording)

    var isPointerDown by remember { mutableStateOf(false) }

    // Pulsing ripple animation while recording/holding down
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isPointerDown || isRecording) 1.15f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "btnScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(140.dp)
    ) {
        // Outer pulsing ripple ring
        if (isRecording || isPointerDown) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(rippleScale)
                    .clip(CircleShape)
                    .background(CyanAccent.copy(alpha = rippleAlpha))
            )
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(rippleScale * 0.85f)
                    .clip(CircleShape)
                    .background(IndigoAccent.copy(alpha = rippleAlpha * 0.6f))
            )
        }

        // Main Push-To-Talk Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(84.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(
                    if (isRecording || isPointerDown) {
                        Brush.linearGradient(listOf(PatienceRed, CyanAccent))
                    } else {
                        Brush.linearGradient(listOf(CyanAccent, IndigoAccent))
                    }
                )
                .border(2.dp, PureWhite.copy(alpha = 0.3f), CircleShape)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        // 1. Wait for ACTION_DOWN
                        awaitFirstDown(requireUnconsumed = false)
                        isPointerDown = true
                        currentOnStart.value()

                        // 2. Wait for ACTION_UP or cancellation via tryAwaitRelease()
                        val released = try {
                            val up = waitForUpOrCancellation()
                            up != null
                        } catch (_: Exception) {
                            false
                        }

                        isPointerDown = false
                        if (released) {
                            currentOnStop.value()
                        } else {
                            currentOnCancel.value()
                        }
                    }
                }
        ) {
            Icon(
                imageVector = if (isRecording || isPointerDown) Icons.Default.Mic else Icons.Default.MicNone,
                contentDescription = "Hold to Speak",
                tint = Navy950,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
