package com.aiteacher.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiteacher.app.data.model.GrammarError
import com.aiteacher.app.ui.theme.CorrectionGreenBg
import com.aiteacher.app.ui.theme.CorrectionGreenBold
import com.aiteacher.app.ui.theme.ErrorRedBg
import com.aiteacher.app.ui.theme.ErrorRedStrikethrough
import com.aiteacher.app.ui.theme.Navy700
import com.aiteacher.app.ui.theme.Navy800
import com.aiteacher.app.ui.theme.Slate200
import com.aiteacher.app.ui.theme.Slate400

@Composable
fun ErrorDiffCard(
    errors: List<GrammarError>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = errors.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Navy800)
                .border(1.dp, Navy700, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Correction",
                    tint = ErrorRedStrikethrough
                )
                Text(
                    text = "GRAMMAR & PHRASING CORRECTIONS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            errors.forEachIndexed { index, error ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
                SingleErrorDiffItem(error = error)
            }
        }
    }
}

@Composable
private fun SingleErrorDiffItem(
    error: GrammarError
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1524))
            .border(1.dp, Navy700, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Tag chip for error category
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Navy700)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = error.errorType.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Slate200,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Incorrect phrase in strikethrough red
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ErrorRedBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "✗ ",
                color = ErrorRedStrikethrough,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error.incorrectPhrase,
                color = ErrorRedStrikethrough,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = TextDecoration.LineThrough,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Corrected phrase in bold green
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CorrectionGreenBg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "✓ ",
                color = CorrectionGreenBold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = error.correctedPhrase,
                color = CorrectionGreenBold,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (error.explanation.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error.explanation,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate400,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
