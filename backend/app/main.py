import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.config import settings
from app.database import init_db
from app.services.cleanup_service import cleanup_worker
from app.api.routes import router as api_router

# Configure logging
logging.basicConfig(
    level=logging.INFO if not settings.DEBUG else logging.DEBUG,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("ai_teacher")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup sequence
    logger.info("Initializing AI Teacher database...")
    await init_db()
    
    logger.info("Starting audio background cleanup worker...")
    cleanup_worker.start()
    
    yield
    
    # Shutdown sequence
    logger.info("Stopping audio background cleanup worker...")
    cleanup_worker.stop()
    logger.info("AI Teacher Backend shutdown complete.")

app = FastAPI(
    title="AI Teacher Voice Engine API",
    description="Backend engine for voice-first English tutor app with Malayalam native understanding",
    version="1.0.0",
    lifespan=lifespan
)

# Enable CORS for Android client & development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount static directory for streaming generated TTS audio files
app.mount("/static", StaticFiles(directory=str(settings.BASE_DIR / "static")), name="static")

# Register API routes
app.include_router(api_router)

@app.get("/")
async def root():
    return {
        "message": "AI Teacher Backend is running!",
        "docs_url": "/docs",
        "health_url": "/api/v1/health"
    }
