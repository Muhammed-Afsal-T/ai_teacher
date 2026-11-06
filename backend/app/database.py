import aiosqlite
import logging
from datetime import datetime, date, timezone
from typing import Optional, Dict, Any, List
from app.config import settings

logger = logging.getLogger(__name__)

def get_db():
    """Returns an asynchronous connection context manager to the SQLite database."""
    return aiosqlite.connect(settings.DATABASE_PATH)

async def init_db():
    """Initializes the database schema with exactly the 4 required tables and initial seed data."""
    async with get_db() as db:
        db.row_factory = aiosqlite.Row
        await db.execute("PRAGMA journal_mode=WAL;")
        await db.execute("PRAGMA foreign_keys=ON;")

        # Table 1: user_profile
        await db.execute("""
            CREATE TABLE IF NOT EXISTS user_profile (
                user_id TEXT PRIMARY KEY,
                current_level TEXT DEFAULT 'B1',
                patience_score INTEGER DEFAULT 100,
                streak_count INTEGER DEFAULT 0,
                last_active_date TEXT
            );
        """)

        # Table 2: curriculum_vault
        await db.execute("""
            CREATE TABLE IF NOT EXISTS curriculum_vault (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT NOT NULL,
                category TEXT NOT NULL,
                target_pattern TEXT NOT NULL,
                prompt_scenario TEXT NOT NULL
            );
        """)

        # Table 3: error_ledger
        await db.execute("""
            CREATE TABLE IF NOT EXISTS error_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                incorrect_phrase TEXT NOT NULL UNIQUE,
                corrected_phrase TEXT NOT NULL,
                error_type TEXT NOT NULL,
                recurrence_count INTEGER DEFAULT 1
            );
        """)

        # Table 4: session_history
        await db.execute("""
            CREATE TABLE IF NOT EXISTS session_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                fluency_score REAL NOT NULL,
                grammar_score REAL NOT NULL,
                penalty_applied INTEGER DEFAULT 0
            );
        """)

        # Seed initial curriculum patterns if empty
        cursor = await db.execute("SELECT COUNT(*) FROM curriculum_vault;")
        row = await cursor.fetchone()
        if row and row[0] == 0:
            seed_curriculum = [
                ("A1", "Greetings & Small Talk", "Present Simple (I am / You are)", "Ask the user to introduce themselves and describe what they do for work or study."),
                ("A2", "Daily Routines", "Past Simple vs Present Continuous", "Ask the user what they did yesterday evening and what they are working on today."),
                ("B1", "Opinions & Explanations", "Conditionals (If I were... / I would)", "Ask the user what they would change if they became the Prime Minister or CEO for a day."),
                ("B2", "Professional Discourse", "Passive Voice & Modal Verbs", "Ask the user to explain a challenging problem they resolved recently at work or in college.")
            ]
            await db.executemany(
                "INSERT INTO curriculum_vault (level, category, target_pattern, prompt_scenario) VALUES (?, ?, ?, ?);",
                seed_curriculum
            )

        # Seed default user if not exists
        await db.execute("""
            INSERT OR IGNORE INTO user_profile (user_id, current_level, patience_score, streak_count, last_active_date)
            VALUES ('default_user', 'B1', 100, 1, ?);
        """, (date.today().isoformat(),))

        await db.commit()
        logger.info("Database schema initialized successfully.")

async def get_or_create_user(user_id: str) -> Dict[str, Any]:
    """Retrieves user profile or initializes a fresh one."""
    today_str = date.today().isoformat()
    async with get_db() as db:
        db.row_factory = aiosqlite.Row
        cursor = await db.execute("SELECT * FROM user_profile WHERE user_id = ?;", (user_id,))
        row = await cursor.fetchone()
        if row:
            return dict(row)
        
        # Create new profile
        await db.execute("""
            INSERT INTO user_profile (user_id, current_level, patience_score, streak_count, last_active_date)
            VALUES (?, 'B1', 100, 1, ?);
        """, (user_id, today_str))
        await db.commit()

        cursor = await db.execute("SELECT * FROM user_profile WHERE user_id = ?;", (user_id,))
        row = await cursor.fetchone()
        return dict(row)

async def update_user_profile(user_id: str, new_patience: int, streak_increment: bool = False) -> Dict[str, Any]:
    """Updates user patience score (clamped 0-100) and updates streak if applicable."""
    clamped_patience = max(0, min(100, new_patience))
    today_str = date.today().isoformat()
    
    async with get_db() as db:
        db.row_factory = aiosqlite.Row
        cursor = await db.execute("SELECT streak_count, last_active_date FROM user_profile WHERE user_id = ?;", (user_id,))
        row = await cursor.fetchone()
        streak = 1
        if row:
            current_streak = row["streak_count"]
            last_date = row["last_active_date"]
            if last_date != today_str and streak_increment:
                streak = current_streak + 1
            else:
                streak = max(1, current_streak)
        
        await db.execute("""
            UPDATE user_profile
            SET patience_score = ?, streak_count = ?, last_active_date = ?
            WHERE user_id = ?;
        """, (clamped_patience, streak, today_str, user_id))
        await db.commit()

        cursor = await db.execute("SELECT * FROM user_profile WHERE user_id = ?;", (user_id,))
        updated_row = await cursor.fetchone()
        return dict(updated_row)

async def record_errors(errors: List[Dict[str, str]]):
    """Records grammar errors into error_ledger, incrementing recurrence_count for repeated phrases."""
    if not errors:
        return
    async with get_db() as db:
        for err in errors:
            incorrect = err.get("incorrect_phrase", "").strip()
            corrected = err.get("corrected_phrase", "").strip()
            error_type = err.get("error_type", "Grammar").strip()
            if not incorrect:
                continue

            await db.execute("""
                INSERT INTO error_ledger (incorrect_phrase, corrected_phrase, error_type, recurrence_count)
                VALUES (?, ?, ?, 1)
                ON CONFLICT(incorrect_phrase) DO UPDATE SET
                    recurrence_count = recurrence_count + 1,
                    corrected_phrase = excluded.corrected_phrase,
                    error_type = excluded.error_type;
            """, (incorrect, corrected, error_type))
        await db.commit()

async def record_session(fluency_score: float, grammar_score: float, penalty_applied: int):
    """Records interaction scores in session_history."""
    now_str = datetime.now(timezone.utc).isoformat()
    async with get_db() as db:
        await db.execute("""
            INSERT INTO session_history (date, fluency_score, grammar_score, penalty_applied)
            VALUES (?, ?, ?, ?);
        """, (now_str, fluency_score, grammar_score, penalty_applied))
        await db.commit()

async def get_frequent_errors(limit: int = 10) -> List[Dict[str, Any]]:
    """Returns top recurring errors."""
    async with get_db() as db:
        db.row_factory = aiosqlite.Row
        cursor = await db.execute("""
            SELECT incorrect_phrase, corrected_phrase, error_type, recurrence_count
            FROM error_ledger
            ORDER BY recurrence_count DESC
            LIMIT ?;
        """, (limit,))
        rows = await cursor.fetchall()
        return [dict(row) for row in rows]
