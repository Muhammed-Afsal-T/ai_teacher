import asyncio
import os
import unittest
from pathlib import Path
from app.config import settings
from app.database import (
    get_db,
    init_db,
    get_or_create_user,
    update_user_profile,
    record_errors,
    record_session,
    get_frequent_errors
)

class TestDatabase(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        # Initialize and clean test database
        await init_db()
        async with get_db() as db:
            await db.execute("DELETE FROM user_profile WHERE user_id LIKE 'test_%';")
            await db.execute("DELETE FROM error_ledger WHERE incorrect_phrase LIKE 'test_%' OR incorrect_phrase = 'I doesn''t know';")
            await db.execute("DELETE FROM session_history;")
            await db.commit()

    async def test_user_profile_crud(self):
        user = await get_or_create_user("test_student_1")
        self.assertEqual(user["user_id"], "test_student_1")
        self.assertEqual(user["patience_score"], 100)
        self.assertEqual(user["current_level"], "B1")

        # Update patience with penalty
        updated = await update_user_profile("test_student_1", new_patience=75, streak_increment=True)
        self.assertEqual(updated["patience_score"], 75)

    async def test_error_ledger_recurrence(self):
        errors = [
            {"incorrect_phrase": "I doesn't know", "corrected_phrase": "I don't know", "error_type": "Subject-Verb Agreement"}
        ]
        await record_errors(errors)
        # Record same error again to test recurrence count increment
        await record_errors(errors)

        freq = await get_frequent_errors(limit=5)
        matched = [e for e in freq if e["incorrect_phrase"] == "I doesn't know"]
        self.assertTrue(len(matched) > 0)
        self.assertEqual(matched[0]["recurrence_count"], 2)

    async def test_session_history_logging(self):
        await record_session(fluency_score=85.0, grammar_score=90.0, penalty_applied=10)

if __name__ == "__main__":
    unittest.main()
