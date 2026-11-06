import unittest
from app.services.tts_service import SSMLStackParser

class TestSSMLStackParser(unittest.TestCase):
    def test_properly_nested_ssml(self):
        raw = '<prosody rate="+10%"><emphasis level="strong">Hello</emphasis></prosody>'
        sanitized = SSMLStackParser.sanitize_and_balance(raw)
        self.assertEqual(sanitized, '<prosody rate="+10%"><emphasis level="strong">Hello</emphasis></prosody>')

    def test_unclosed_tags_lifo_repair(self):
        raw = '<prosody rate="+15%"><emphasis level="strong">Stop speaking Malayalam!'
        sanitized = SSMLStackParser.sanitize_and_balance(raw)
        # Should close in reverse order: </emphasis></prosody>
        self.assertEqual(sanitized, '<prosody rate="+15%"><emphasis level="strong">Stop speaking Malayalam!</emphasis></prosody>')

    def test_crossed_tags_lifo_repair(self):
        # Crossed: open A, open B, close A
        raw = '<prosody rate="+5%"><emphasis>Important</prosody></emphasis>'
        sanitized = SSMLStackParser.sanitize_and_balance(raw)
        # LIFO must pop emphasis before prosody
        self.assertIn("</emphasis>", sanitized)
        self.assertIn("</prosody>", sanitized)

    def test_unsupported_tags_stripped(self):
        raw = '<prosody rate="0%"><script>alert(1)</script><randomtag>Speak English</randomtag></prosody>'
        sanitized = SSMLStackParser.sanitize_and_balance(raw)
        self.assertNotIn("<script>", sanitized)
        self.assertNotIn("<randomtag>", sanitized)
        self.assertIn('<prosody rate="0%">alert(1)Speak English</prosody>', sanitized)

    def test_self_closing_break(self):
        raw = '<prosody rate="+5%">Wait a moment.<break time="500ms"/> Now continue.</prosody>'
        sanitized = SSMLStackParser.sanitize_and_balance(raw)
        self.assertIn('<break time="500ms"/>', sanitized)
        self.assertTrue(sanitized.endswith('</prosody>'))

if __name__ == "__main__":
    unittest.main()
