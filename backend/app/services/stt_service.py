import os
import io
import logging
from typing import Tuple
from groq import AsyncGroq
from app.config import settings

logger = logging.getLogger(__name__)

class STTService:
    def __init__(self):
        self.api_key = settings.GROQ_API_KEY
        self.client: Optional[AsyncGroq] = None
        if self.api_key and not self.api_key.startswith("gsk_placeholder"):
            self.client = AsyncGroq(api_key=self.api_key)

    async def transcribe_audio(self, audio_bytes: bytes, filename: str = "audio.m4a") -> str:
        """
        Transcribes user voice input using Groq whisper-large-v3-turbo.
        Supports Malayalam and English multilingual audio seamlessly.
        """
        if not self.client:
            logger.warning("Groq API key not configured or is placeholder. Using mock transcription for development.")
            return "ഇതിന്റെ അർത്ഥം എന്താണ് teacher? What does this word mean?"

        try:
            # Create in-memory file tuple for Groq SDK
            file_tuple = (filename, audio_bytes)
            
            # Transcription with hint prompt for Malayalam-English code-switching
            transcription = await self.client.audio.transcriptions.create(
                file=file_tuple,
                model=settings.STT_MODEL,
                prompt="Malayalam and English conversational audio. സംഭാഷണം, ഇംഗ്ലീഷ് പഠനം, doubts and questions.",
                response_format="text",
                temperature=0.0
            )
            
            result_text = transcription if isinstance(transcription, str) else getattr(transcription, "text", str(transcription))
            logger.info(f"Transcription successful: '{result_text[:80]}...'")
            return result_text.strip()
        except Exception as e:
            logger.error(f"Error during Groq Whisper transcription: {str(e)}", exc_info=True)
            raise RuntimeError(f"STT transcription failed: {str(e)}")

stt_service = STTService()
