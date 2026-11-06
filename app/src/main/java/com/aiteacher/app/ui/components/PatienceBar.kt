package com.aiteacher.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiteacher.app.ui.theme.Navy700
import com.aiteacher.app.ui.theme.Navy800
import com.aiteacher.app.ui.theme.PatienceAmber
import com.aiteacher.app.ui.theme.PatienceGreen
import com.aiteacher.app.ui.theme.PatienceRed
import com.aiteacher.app.ui.theme.Slate400

@Composable
fun PatienceBar(
    patienceScore: Int,
    modifier: Modifier = Modifier
) {
    // Dynamic color thresholds: Green > 70, Amber 30-70, Red < 30
    val targetColor = when {
        patienceScore > 70 -> PatienceGreen
        patienceScore >= 30 -> PatienceAmber
        else -> PatienceRed
    }

    val statusText = when {
        patienceScore > 70 -> "Patient & Encouraging 😊"
        patienceScore >= 30 -> "Firm & Disciplined 😐"
        else -> "FURIOUS! English Only! 😡"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (patienceScore.coerceIn(0, 100)) / 100f,
        animationSpec = tween(durationMillis = 600),
        label = "patienceProgress"
    )

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 500),
        label = "patienceColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Navy800)
            .border(1.dp, Navy700, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TEACHER'S PATIENCE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = animatedColor
                    )
                )
            }
            Text(
                text = "$patienceScore / 100",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = animatedColor
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(Navy700)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                animatedColor.copy(alpha = 0.7f),
                                animatedColor
                            )
                        )
                    )
            )
        }
    }
}
