package com.aiteacher.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aiteacher.app.audio.AudioPlayer
import com.aiteacher.app.audio.AudioRecorder
import com.aiteacher.app.data.api.RetrofitClient
import com.aiteacher.app.data.model.GrammarError
import com.aiteacher.app.data.model.InteractResponse
import com.aiteacher.app.data.repository.TeacherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class InteractionState {
    object Idle : InteractionState()
    object Recording : InteractionState()
    object UploadingAndEvaluating : InteractionState()
    object PlayingTutorAudio : InteractionState()
    data class Error(val message: String) : InteractionState()
}

data class TeacherUiState(
    val patienceScore: Int = 100,
    val streakCount: Int = 1,
    val currentLevel: String = "B1",
    val userTranscription: String = "",
    val tutorResponseText: String = "Hold the microphone button below and introduce yourself or ask a doubt in English.",
    val errors: List<GrammarError> = emptyList(),
    val interactionState: InteractionState = InteractionState.Idle,
    val lastAudioUrl: String? = null
)

class TeacherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TeacherRepository()
    private val audioRecorder = AudioRecorder(application.applicationContext)
    private val audioPlayer = AudioPlayer(application.applicationContext)

    private val _uiState = MutableStateFlow(TeacherUiState())
    val uiState: StateFlow<TeacherUiState> = _uiState.asStateFlow()

    private var activeRecordingFile: File? = null

    fun startRecording() {
        if (_uiState.value.interactionState == InteractionState.PlayingTutorAudio) {
            audioPlayer.stopAudio()
        }
        val file = audioRecorder.startRecording()
        if (file != null) {
            activeRecordingFile = file
            _uiState.value = _uiState.value.copy(
                interactionState = InteractionState.Recording
            )
        } else {
            _uiState.value = _uiState.value.copy(
                interactionState = InteractionState.Error("Could not initialize microphone.")
            )
        }
    }

    fun stopRecordingAndSubmit() {
        val file = audioRecorder.stopRecording()
        if (file == null || !file.exists() || file.length() == 0L) {
            _uiState.value = _uiState.value.copy(
                interactionState = InteractionState.Idle
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            interactionState = InteractionState.UploadingAndEvaluating
        )

        viewModelScope.launch {
            val result = repository.sendAudioInteraction(file)
            result.onSuccess { response ->
                handleInteractSuccess(response)
            }.onFailure { error ->
                Log.e("TeacherViewModel", "Interaction failed", error)
                _uiState.value = _uiState.value.copy(
                    interactionState = InteractionState.Error(error.message ?: "Failed to evaluate speech.")
                )
            }
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        activeRecordingFile = null
        _uiState.value = _uiState.value.copy(
            interactionState = InteractionState.Idle
        )
    }

    private fun handleInteractSuccess(response: InteractResponse) {
        _uiState.value = _uiState.value.copy(
            patienceScore = response.patienceScore,
            streakCount = response.streakCount,
            currentLevel = response.currentLevel,
            userTranscription = response.transcription,
            tutorResponseText = response.tutorText,
            errors = response.evaluation.grammarErrors,
            lastAudioUrl = response.audioUrl,
            interactionState = InteractionState.PlayingTutorAudio
        )

        if (response.audioUrl.isNotBlank()) {
            val resolvedUrl = RetrofitClient.resolveAudioUrl(response.audioUrl)
            audioPlayer.playAudio(
                url = resolvedUrl,
                onCompletion = {
                    _uiState.value = _uiState.value.copy(
                        interactionState = InteractionState.Idle
                    )
                },
                onError = { err ->
                    Log.w("TeacherViewModel", "Audio playback error: $err")
                    _uiState.value = _uiState.value.copy(
                        interactionState = InteractionState.Idle
                    )
                }
            )
        } else {
            _uiState.value = _uiState.value.copy(
                interactionState = InteractionState.Idle
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stopAudio()
        audioRecorder.cancelRecording()
    }
}
