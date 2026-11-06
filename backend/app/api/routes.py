import logging
from typing import Optional
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, status
from app.models import InteractResponse, EvaluationPayload, TutorResponse
from app.services.stt_service import stt_service
from app.services.llm_service import llm_service
from app.services.tts_service import tts_service
from app.database import (
    get_or_create_user,
    update_user_profile,
    record_errors,
    record_session,
    get_frequent_errors
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["Voice Interaction"])

@router.post("/interact", response_model=InteractResponse)
async def interact(
    audio: UploadFile = File(..., description="User voice recording (m4a, wav, mp3, ogg)"),
    user_id: str = Form("default_user", description="Identifier of the student")
):
    """
    Main push-to-talk voice interaction pipeline:
    1. Receives and buffers recorded voice audio.
    2. Transcribes voice via Groq Whisper (native Malayalam & English).
    3. Loads student profile from SQLite.
    4. Evaluates conversation and enforces the 3 behavioral rules with Groq Llama 3.3.
    5. Updates SQLite user profile, error ledger, and session history.
    6. Generates emotional TTS audio via edge-tts with LIFO-repaired SSML.
    7. Returns complete JSON payload and audio streaming URL.
    """
    # 1. Read audio bytes
    try:
        audio_bytes = await audio.read()
        if not audio_bytes:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Empty audio payload received."
            )
    except Exception as e:
        logger.error(f"Failed to read audio file: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Could not read audio file: {str(e)}"
        )

    # 2. Transcribe voice
    try:
        user_transcription = await stt_service.transcribe_audio(audio_bytes, filename=audio.filename or "recording.m4a")
        if not user_transcription:
            user_transcription = "[Inaudible or silent voice]"
    except Exception as e:
        logger.error(f"STT Error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Speech recognition failed: {str(e)}"
        )

    # 3. Fetch user profile from SQLite
    try:
        user_profile = await get_or_create_user(user_id)
        current_patience = user_profile.get("patience_score", 100)
        user_level = user_profile.get("current_level", "B1")
    except Exception as e:
        logger.error(f"Database error fetching user: {e}")
        current_patience = 100
        user_level = "B1"

    # 4. LLM Evaluation & Tutor SSML Generation
    try:
        llm_result = await llm_service.evaluate_and_respond(
            user_text=user_transcription,
            current_patience=current_patience,
            user_level=user_level
        )
        tutor_text = llm_result.get("tutor_text", "Let's continue practicing in English.")
        tutor_ssml = llm_result.get("tutor_ssml", tutor_text)
        evaluation_data = llm_result.get("evaluation", {})
    except Exception as e:
        logger.error(f"LLM Error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Language model evaluation failed: {str(e)}"
        )

    # 5. Extract evaluation metrics & update SQLite
    patience_delta = evaluation_data.get("patience_delta", 0)
    calculated_patience = max(0, min(100, current_patience + patience_delta))
    evaluation_data["new_patience_score"] = calculated_patience
    streak_inc = evaluation_data.get("streak_increment", False)

    try:
        updated_profile = await update_user_profile(
            user_id=user_id,
            new_patience=calculated_patience,
            streak_increment=streak_inc
        )
        # Record errors in error_ledger
        grammar_errors = evaluation_data.get("grammar_errors", [])
        if grammar_errors:
            await record_errors(grammar_errors)

        # Record session history
        fluency = evaluation_data.get("fluency_score", 80.0)
        grammar = evaluation_data.get("grammar_score", 85.0)
        penalty = abs(patience_delta) if patience_delta < 0 else 0
        await record_session(fluency, grammar, penalty)
    except Exception as e:
        logger.error(f"Database error updating state: {e}")
        updated_profile = {
            "patience_score": calculated_patience,
            "streak_count": user_profile.get("streak_count", 1),
            "current_level": user_level
        }

    # 6. Generate emotional TTS audio via edge-tts with LIFO SSML repair
    try:
        audio_url = await tts_service.generate_speech(raw_ssml=tutor_ssml, fallback_text=tutor_text)
    except Exception as e:
        logger.error(f"TTS synthesis error: {e}")
        audio_url = ""

    # 7. Construct and return response
    eval_model = EvaluationPayload(**evaluation_data)

    return InteractResponse(
        user_id=user_id,
        transcription=user_transcription,
        tutor_text=tutor_text,
        tutor_ssml=tutor_ssml,
        audio_url=audio_url,
        evaluation=eval_model,
        patience_score=updated_profile.get("patience_score", calculated_patience),
        streak_count=updated_profile.get("streak_count", 1),
        current_level=updated_profile.get("current_level", user_level)
    )

@router.get("/profile/{user_id}")
async def get_profile(user_id: str = "default_user"):
    """Fetches user status, patience score, streak, and frequent mistakes."""
    user = await get_or_create_user(user_id)
    frequent_errors = await get_frequent_errors(limit=5)
    return {
        "profile": user,
        "frequent_errors": frequent_errors
    }

@router.get("/health")
async def health_check():
    """Service health and readiness check."""
    return {"status": "ok", "app": "AI Teacher Backend", "version": "1.0.0"}
