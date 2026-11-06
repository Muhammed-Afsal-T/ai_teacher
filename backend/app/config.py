import os
from pathlib import Path
from pydantic_settings import BaseSettings

BASE_DIR = Path(__file__).resolve().parent.parent

class Settings(BaseSettings):
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = True
    
    # Database
    DATABASE_PATH: str = str(BASE_DIR / "ai_teacher.db")
    
    # Audio Storage & Cleanup
    AUDIO_STORAGE_PATH: str = str(BASE_DIR / "static" / "audio")
    AUDIO_RETENTION_SECONDS: int = 3600  # 1 hour
    CLEANUP_INTERVAL_SECONDS: int = 600   # 10 minutes
    
    # Voices
    TTS_DEFAULT_VOICE: str = "en-US-ChristopherNeural"
    TTS_MALAYALAM_VOICE: str = "ml-IN-MidhunNeural"
    
    # Models
    STT_MODEL: str = "whisper-large-v3-turbo"
    LLM_MODEL: str = "llama-3.3-70b-versatile"
    
    class Config:
        env_file = str(BASE_DIR / ".env")
        env_file_encoding = "utf-8"
        extra = "ignore"

settings = Settings()

# Ensure directories exist
Path(settings.AUDIO_STORAGE_PATH).mkdir(parents=True, exist_ok=True)
