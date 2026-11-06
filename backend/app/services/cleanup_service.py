import os
import time
import logging
import threading
from pathlib import Path
from app.config import settings

logger = logging.getLogger(__name__)

class AudioCleanupWorker:
    """
    Background worker thread that runs periodically to remove expired audio files
    from static/audio, preventing disk storage overflow.
    """
    def __init__(self, directory: str = settings.AUDIO_STORAGE_PATH, retention_seconds: int = settings.AUDIO_RETENTION_SECONDS, interval_seconds: int = settings.CLEANUP_INTERVAL_SECONDS):
        self.directory = Path(directory)
        self.retention_seconds = retention_seconds
        self.interval_seconds = interval_seconds
        self._stop_event = threading.Event()
        self._thread: Optional[threading.Thread] = None

    def start(self):
        if self._thread and self._thread.is_alive():
            return
        self._stop_event.clear()
        self._thread = threading.Thread(target=self._run_loop, daemon=True, name="AudioCleanupWorker")
        self._thread.start()
        logger.info(f"Audio cleanup worker started. Scanning {self.directory} every {self.interval_seconds}s for files older than {self.retention_seconds}s.")

    def stop(self):
        self._stop_event.set()
        if self._thread:
            self._thread.join(timeout=2.0)
        logger.info("Audio cleanup worker stopped.")

    def _run_loop(self):
        while not self._stop_event.is_set():
            try:
                self.clean_expired_files()
            except Exception as e:
                logger.error(f"Error during audio cleanup: {str(e)}", exc_info=True)
            # Sleep in small increments for responsive shutdown
            self._stop_event.wait(timeout=self.interval_seconds)

    def clean_expired_files(self) -> int:
        """Deletes files older than retention_seconds. Returns count of deleted files."""
        if not self.directory.exists():
            return 0

        now = time.time()
        deleted_count = 0

        for file_path in self.directory.glob("*.mp3"):
            try:
                mtime = file_path.stat().st_mtime
                if now - mtime > self.retention_seconds:
                    file_path.unlink(missing_ok=True)
                    deleted_count += 1
                    logger.debug(f"Deleted expired audio file: {file_path.name}")
            except Exception as e:
                logger.warning(f"Could not delete {file_path.name}: {e}")

        if deleted_count > 0:
            logger.info(f"Cleaned up {deleted_count} expired audio files from {self.directory}.")
        return deleted_count

cleanup_worker = AudioCleanupWorker()
