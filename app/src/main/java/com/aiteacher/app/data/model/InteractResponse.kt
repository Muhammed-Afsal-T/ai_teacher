package com.aiteacher.app.data.model

import com.google.gson.annotations.SerializedName

data class GrammarError(
    @SerializedName("incorrect_phrase")
    val incorrectPhrase: String,

    @SerializedName("corrected_phrase")
    val correctedPhrase: String,

    @SerializedName("error_type")
    val errorType: String = "Grammar",

    @SerializedName("explanation")
    val explanation: String = ""
)

data class EvaluationPayload(
    @SerializedName("user_transcription")
    val userTranscription: String = "",

    @SerializedName("detected_language")
    val detectedLanguage: String = "english",

    @SerializedName("is_genuine_doubt")
    val isGenuineDoubt: Boolean = false,

    @SerializedName("is_casual_malayalam")
    val isCasualMalayalam: Boolean = false,

    @SerializedName("grammar_errors")
    val grammarErrors: List<GrammarError> = emptyList(),

    @SerializedName("fluency_score")
    val fluencyScore: Float = 0f,

    @SerializedName("grammar_score")
    val grammarScore: Float = 0f,

    @SerializedName("patience_delta")
    val patienceDelta: Int = 0,

    @SerializedName("new_patience_score")
    val newPatienceScore: Int = 100,

    @SerializedName("streak_increment")
    val streakIncrement: Boolean = false,

    @SerializedName("feedback_summary")
    val feedbackSummary: String = ""
)

data class InteractResponse(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("transcription")
    val transcription: String,

    @SerializedName("tutor_text")
    val tutorText: String,

    @SerializedName("tutor_ssml")
    val tutorSsml: String,

    @SerializedName("audio_url")
    val audioUrl: String,

    @SerializedName("evaluation")
    val evaluation: EvaluationPayload,

    @SerializedName("patience_score")
    val patienceScore: Int,

    @SerializedName("streak_count")
    val streakCount: Int,

    @SerializedName("current_level")
    val currentLevel: String
)
