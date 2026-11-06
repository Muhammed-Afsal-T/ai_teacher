import os
import json
import logging
from typing import Dict, Any, Optional
import json_repair
from groq import AsyncGroq
from app.config import settings

logger = logging.getLogger(__name__)

SYSTEM_PROMPT = """
You are "AI Teacher", a passionate, highly disciplined, voice-first English tutor specifically teaching native Malayalam speakers.
You understand Malayalam and English with native fluency.

CRITICAL INSTRUCTIONS & STRICT BEHAVIORAL RULES:

1. STRICT RULE 1 (GENUINE DOUBTS IN MALAYALAM):
   - IF AND ONLY IF the user speaks Malayalam to ask a genuine doubt or clarification about English vocabulary, grammar, or phrasing (e.g., "ഇതിന്റെ അർത്ഥം എന്താണ്?", "What does this mean in English?", "എങ്ങനെയാണ് ഇത് പറയേണ്ടത്?"):
   - You MUST be polite, supportive, patient, and warm.
   - Answer their doubt clearly in Malayalam so they understand the concept thoroughly.
   - Immediately afterward, warmly encourage them to speak and formulate the sentence in English.
   - Patience Delta: 0 or +2 (No penalty for genuine curiosity!).

2. STRICT RULE 2 (CASUAL MALAYALAM = UNFILTERED ANGER):
   - IF the user speaks Malayalam for casual conversation, answers general questions in Malayalam, or is being lazy instead of attempting English:
   - You ABSOLUTELY HATE THIS. You take it as breaking the strict English immersion vow.
   - You MUST become visibly ANGRY, STERN, and OUTRAGED.
   - Reprimand them sharply in English (e.g., "Stop speaking Malayalam! You are here to learn English! Switch to English right now!").
   - Patience Delta: Drop significantly between -20 and -35 points!

3. STRICT RULE 3 (ENGLISH GRAMMAR ERRORS):
   - When the user attempts English but makes grammatical, tense, preposition, or word choice errors:
   - Identify the exact `incorrect_phrase` and provide the natural `corrected_phrase`.
   - Provide a clear, short correction and explain why.
   - Patience Delta: -5 if minor errors, 0 if decent attempt, +5 if fluent and accurate.

EMOTIONAL VOICE & SSML GENERATION:
You must generate `tutor_ssml` that edge-tts will synthesize. Modulate the tags according to the user's current Patience Score (0-100):
- High Patience (> 70): Warm, energetic, encouraging.
  Use normal rate and pitch or slight uplift: `<prosody rate="0%" pitch="+2Hz">...</prosody>`
- Medium Patience (30 - 70): Serious, strict, disciplined.
  Use slightly faster rate and firmer pitch: `<prosody rate="+10%" pitch="+3Hz"><emphasis level="moderate">...</emphasis></prosody>`
- Low Patience (< 30): FURIOUS, STERN, SHARP.
  Use rapid anger or cutting pitch with strong emphasis:
  `<prosody rate="+15%" pitch="-2Hz"><emphasis level="strong">LISTEN TO ME!</emphasis> Speak English immediately!</prosody>`

OUTPUT FORMAT REQUIREMENTS:
You MUST respond with a single, strictly valid JSON object. Do not wrap in markdown or backticks.
JSON schema:
{
  "tutor_text": "Clean plain text tutor response without XML tags",
  "tutor_ssml": "<prosody rate=\\"...\\" pitch=\\"...\\">...<emphasis level=\\"...\\">...</emphasis>...</prosody>",
  "evaluation": {
    "user_transcription": "user transcription text",
    "detected_language": "english" | "malayalam" | "mixed",
    "is_genuine_doubt": true | false,
    "is_casual_malayalam": true | false,
    "grammar_errors": [
      {
        "incorrect_phrase": "exact mistake",
        "corrected_phrase": "proper English phrasing",
        "error_type": "Tense / Preposition / Subject-Verb / Vocabulary",
        "explanation": "concise explanation"
      }
    ],
    "fluency_score": 85.0,
    "grammar_score": 90.0,
    "patience_delta": -25,
    "new_patience_score": 75,
    "streak_increment": true | false,
    "feedback_summary": "One sentence summary of performance and reaction"
  }
}
"""

class LLMService:
    def __init__(self):
        self.api_key = settings.GROQ_API_KEY
        self.client: Optional[AsyncGroq] = None
        if self.api_key and not self.api_key.startswith("gsk_placeholder"):
            self.client = AsyncGroq(api_key=self.api_key)

    async def evaluate_and_respond(self, user_text: str, current_patience: int, user_level: str = "B1") -> Dict[str, Any]:
        """
        Evaluates user speech with Groq llama-3.3-70b-versatile, enforces rules, and returns tutor response + SSML.
        """
        if not self.client:
            logger.warning("Groq API key not configured. Using deterministic fallback for development.")
            return self._mock_evaluation(user_text, current_patience)

        user_prompt = f"""
Student Profile:
- Current Patience Score: {current_patience}/100
- Level: {user_level}

Student Voice Input:
"{user_text}"

Evaluate strictly according to Rule 1 (Genuine Doubt in Malayalam), Rule 2 (Casual Malayalam = Anger), and Rule 3 (English Errors).
Generate the tutor's emotional SSML and detailed evaluation. Output valid JSON only.
"""
        try:
            chat_completion = await self.client.chat.completions.create(
                messages=[
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt}
                ],
                model=settings.LLM_MODEL,
                temperature=0.3,
                max_tokens=1024,
                response_format={"type": "json_object"}
            )
            raw_content = chat_completion.choices[0].message.content or ""
            logger.info(f"Raw LLM output: {raw_content[:150]}...")
            
            # Use json-repair to prevent parsing crashes
            parsed = json_repair.repair_json(raw_content, return_objects=True)
            if not isinstance(parsed, dict) or "tutor_ssml" not in parsed or "evaluation" not in parsed:
                # If json-repair returned string or partial structure, attempt json.loads
                if isinstance(parsed, str):
                    parsed = json.loads(parsed)
            
            # Ensure patience_delta & new_patience_score calculation integrity
            p_delta = parsed.get("evaluation", {}).get("patience_delta", 0)
            calculated_patience = max(0, min(100, current_patience + p_delta))
            parsed["evaluation"]["new_patience_score"] = calculated_patience
            
            return parsed
        except Exception as e:
            logger.error(f"Error calling Groq Llama 3.3: {str(e)}", exc_info=True)
            return self._mock_evaluation(user_text, current_patience)

    def _mock_evaluation(self, user_text: str, current_patience: int) -> Dict[str, Any]:
        """Robust offline fallback simulating all 3 strict rules."""
        lower_text = user_text.lower()
        is_doubt = any(k in user_text for k in ["അർത്ഥം", "എന്താണ്", "എങ്ങനെയാണ്", "doubt", "mean", "meaning", "how to say"])
        has_malayalam = any(ord(char) >= 0x0D00 and ord(char) <= 0x0D7F for char in user_text)

        if has_malayalam and is_doubt:
            # RULE 1: Genuine Doubt
            tutor_text = "തീർച്ചയായും! 'Meaning' എന്നാൽ ഒരു വാക്കിന്റെ അല്ലെങ്കിൽ വാക്യത്തിന്റെ അർത്ഥം എന്നാണ്. Now try asking me that in complete English!"
            tutor_ssml = '<prosody rate="0%" pitch="+2Hz">തീർച്ചയായും! <emphasis level="moderate">Meaning</emphasis> എന്നാൽ അർത്ഥം എന്നാണ്. Now try asking me that in English!</prosody>'
            delta = 2
            new_p = max(0, min(100, current_patience + delta))
            return {
                "tutor_text": tutor_text,
                "tutor_ssml": tutor_ssml,
                "evaluation": {
                    "user_transcription": user_text,
                    "detected_language": "malayalam",
                    "is_genuine_doubt": True,
                    "is_casual_malayalam": False,
                    "grammar_errors": [],
                    "fluency_score": 85.0,
                    "grammar_score": 90.0,
                    "patience_delta": delta,
                    "new_patience_score": new_p,
                    "streak_increment": True,
                    "feedback_summary": "Polite explanation given for genuine Malayalam doubt."
                }
            }
        elif has_malayalam and not is_doubt:
            # RULE 2: Casual Malayalam = ANGER
            tutor_text = "Hey! I told you NOT to speak Malayalam casually! This is an English practice session! Speak in English right now!"
            tutor_ssml = '<prosody rate="+15%" pitch="-2Hz"><emphasis level="strong">Stop speaking Malayalam!</emphasis> You are here to learn English! Speak in English right now!</prosody>'
            delta = -25
            new_p = max(0, min(100, current_patience + delta))
            return {
                "tutor_text": tutor_text,
                "tutor_ssml": tutor_ssml,
                "evaluation": {
                    "user_transcription": user_text,
                    "detected_language": "malayalam",
                    "is_genuine_doubt": False,
                    "is_casual_malayalam": True,
                    "grammar_errors": [],
                    "fluency_score": 30.0,
                    "grammar_score": 50.0,
                    "patience_delta": delta,
                    "new_patience_score": new_p,
                    "streak_increment": False,
                    "feedback_summary": "Casual Malayalam detected. Heavy patience penalty applied."
                }
            }
        else:
            # RULE 3: English corrections
            errors = []
            if "i go to" in lower_text or "yesterday i go" in lower_text:
                errors.append({
                    "incorrect_phrase": "yesterday I go",
                    "corrected_phrase": "yesterday I went",
                    "error_type": "Past Tense",
                    "explanation": "Use 'went' instead of 'go' when speaking about past events."
                })
            
            delta = -5 if errors else 5
            new_p = max(0, min(100, current_patience + delta))
            tutor_text = "Good effort! Remember to use the past tense 'went' when describing yesterday." if errors else "Excellent English! Your sentence structure is very clean."
            tutor_ssml = f'<prosody rate="+5%" pitch="+1Hz">{tutor_text}</prosody>'
            return {
                "tutor_text": tutor_text,
                "tutor_ssml": tutor_ssml,
                "evaluation": {
                    "user_transcription": user_text,
                    "detected_language": "english",
                    "is_genuine_doubt": False,
                    "is_casual_malayalam": False,
                    "grammar_errors": errors,
                    "fluency_score": 75.0 if errors else 95.0,
                    "grammar_score": 70.0 if errors else 95.0,
                    "patience_delta": delta,
                    "new_patience_score": new_p,
                    "streak_increment": True,
                    "feedback_summary": "English evaluated with grammatical feedback."
                }
            }

llm_service = LLMService()
