from typing import List, Optional
from pydantic import BaseModel, Field

class GrammarError(BaseModel):
    incorrect_phrase: str = Field(description="The exact incorrect phrase spoken by the user")
    corrected_phrase: str = Field(description="The natural, grammatically correct English alternative")
    error_type: str = Field(default="Grammar", description="Category of error, e.g. Tense, Preposition, Subject-Verb Agreement, Word Choice")
    explanation: str = Field(default="", description="Brief explanation in simple English")

class EvaluationPayload(BaseModel):
    user_transcription: str = Field(description="Transcribed user speech")
    detected_language: str = Field(description="'english', 'malayalam', or 'mixed'")
    is_genuine_doubt: bool = Field(description="True ONLY if user spoke Malayalam to ask a genuine doubt/question about meaning or grammar")
    is_casual_malayalam: bool = Field(description="True if user casually spoke Malayalam for general conversation")
    grammar_errors: List[GrammarError] = Field(default_factory=list, description="List of English grammar/vocabulary errors")
    fluency_score: float = Field(default=80.0, ge=0.0, le=100.0, description="Fluency assessment score 0-100")
    grammar_score: float = Field(default=85.0, ge=0.0, le=100.0, description="Grammar accuracy score 0-100")
    patience_delta: int = Field(default=0, description="Change in patience score (-35 to +5)")
    new_patience_score: int = Field(default=100, ge=0, le=100, description="Calculated updated patience score")
    streak_increment: bool = Field(default=False, description="Whether to increment user daily streak")
    feedback_summary: str = Field(default="", description="Quick one-sentence evaluation overview")

class TutorResponse(BaseModel):
    tutor_text: str = Field(description="Clean text response of the tutor")
    tutor_ssml: str = Field(description="SSML response modulating prosody and emphasis according to emotional state")
    evaluation: EvaluationPayload

class InteractResponse(BaseModel):
    user_id: str
    transcription: str
    tutor_text: str
    tutor_ssml: str
    audio_url: str
    evaluation: EvaluationPayload
    patience_score: int
    streak_count: int
    current_level: str
