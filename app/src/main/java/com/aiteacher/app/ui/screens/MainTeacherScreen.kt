package com.aiteacher.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiteacher.app.ui.components.ErrorDiffCard
import com.aiteacher.app.ui.components.MicButton
import com.aiteacher.app.ui.components.PatienceBar
import com.aiteacher.app.ui.theme.CyanAccent
import com.aiteacher.app.ui.theme.IndigoAccent
import com.aiteacher.app.ui.theme.Navy700
import com.aiteacher.app.ui.theme.Navy800
import com.aiteacher.app.ui.theme.Navy900
import com.aiteacher.app.ui.theme.Navy950
import com.aiteacher.app.ui.theme.PatienceAmber
import com.aiteacher.app.ui.theme.PatienceGreen
import com.aiteacher.app.ui.theme.PatienceRed
import com.aiteacher.app.ui.theme.Slate200
import com.aiteacher.app.ui.theme.Slate400
import com.aiteacher.app.ui.viewmodel.InteractionState
import com.aiteacher.app.ui.viewmodel.TeacherViewModel

@Composable
fun MainTeacherScreen(
    viewModel: TeacherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Navy900
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header
            TopHeader(
                streakCount = uiState.streakCount,
                currentLevel = uiState.currentLevel
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Patience Bar
            PatienceBar(
                patienceScore = uiState.patienceScore
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content Area (Transcripts & Error Cards)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Voice Transcript Card (if user has spoken)
                if (uiState.userTranscription.isNotBlank()) {
                    UserTranscriptCard(transcription = uiState.userTranscription)
                }

                // AI Tutor Response Card
                TutorResponseCard(
                    tutorText = uiState.tutorResponseText,
                    patienceScore = uiState.patienceScore,
                    interactionState = uiState.interactionState
                )

                // Error Diff Card (Strikethrough red + bold green)
                ErrorDiffCard(errors = uiState.errors)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interaction Status Badge
            InteractionStatusBadge(state = uiState.interactionState)

            Spacer(modifier = Modifier.height(8.dp))

            // Push-To-Talk Mic Button
            MicButton(
                isRecording = uiState.interactionState == InteractionState.Recording,
                enabled = uiState.interactionState !is InteractionState.UploadingAndEvaluating,
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecordingAndSubmit() },
                onCancelRecording = { viewModel.cancelRecording() }
            )

            Text(
                text = "Hold to Speak • Release to Send",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun TopHeader(
    streakCount: Int,
    currentLevel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Navy800)
                    .border(1.dp, Navy700, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "AI Teacher",
                    tint = CyanAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "AI Teacher",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate200
                    )
                )
                Text(
                    text = "Level $currentLevel",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate400
                    )
                )
            }
        }

        // Streak Count Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Navy800)
                .border(1.dp, Navy700, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🔥 $streakCount Day Streak",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = PatienceAmber
                )
            )
        }
    }
}

@Composable
private fun UserTranscriptCard(transcription: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Navy700, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "YOU SPOKE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Slate400,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "\"$transcription\"",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Slate200,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun TutorResponseCard(
    tutorText: String,
    patienceScore: Int,
    interactionState: InteractionState
) {
    val borderColor = when {
        patienceScore > 70 -> PatienceGreen.copy(alpha = 0.5f)
        patienceScore >= 30 -> PatienceAmber.copy(alpha = 0.5f)
        else -> PatienceRed.copy(alpha = 0.6f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Navy800)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = if (patienceScore < 30) PatienceRed else CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI TUTOR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (patienceScore < 30) PatienceRed else CyanAccent
                    )
                )
            }

            if (interactionState == InteractionState.PlayingTutorAudio) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Playing Audio",
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Speaking...",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = tutorText,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Slate200,
                lineHeight = 22.sp
            )
        )
    }
}

@Composable
private fun InteractionStatusBadge(state: InteractionState) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Navy800)
            .border(1.dp, Navy700, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is InteractionState.Idle -> {
                Text(
                    text = "Ready to listen",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                )
            }
            is InteractionState.Recording -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PatienceRed)
                    )
                    Text(
                        text = "Listening... Release when finished",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = PatienceRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            is InteractionState.UploadingAndEvaluating -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = CyanAccent
                    )
                    Text(
                        text = "Evaluating speech & rules...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            is InteractionState.PlayingTutorAudio -> {
                Text(
                    text = "Tutor is responding...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = CyanAccent,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            is InteractionState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PatienceRed,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
