import os
import re
import uuid
import logging
import asyncio
from pathlib import Path
from typing import Tuple, List
import edge_tts
from app.config import settings

logger = logging.getLogger(__name__)

SUPPORTED_TAGS = {"prosody", "emphasis", "break", "say-as", "sub"}

class SSMLStackParser:
    """
    Safely sanitizes and balances SSML using a Last-In First-Out (LIFO) tag stack.
    Guarantees that unclosed tags, crossed tags, or malformed tags produced by LLMs
    are repaired into valid XML for edge-tts.
    """
    
    TAG_REGEX = re.compile(r'(<(/?[a-zA-Z0-9_\-]+)([^>]*)>)')

    @classmethod
    def sanitize_and_balance(cls, raw_ssml: str) -> str:
        """
        Parses raw SSML string, strips invalid tags, enforces LIFO matching,
        and auto-closes any dangling tags.
        """
        if not raw_ssml or not raw_ssml.strip():
            return "No response generated."

        # Remove outer speak or voice tags if LLM mistakenly added them
        cleaned = re.sub(r'</?(speak|voice)[^>]*>', '', raw_ssml, flags=re.IGNORECASE).strip()
        
        stack: List[str] = []
        result_parts: List[str] = []
        last_idx = 0

        for match in cls.TAG_REGEX.finditer(cleaned):
            start, end = match.span()
            full_match = match.group(1)
            raw_tag_name = match.group(2)
            attributes = match.group(3)

            # Append text between tags
            text_segment = cleaned[last_idx:start]
            if text_segment:
                result_parts.append(text_segment)
            last_idx = end

            is_closing = raw_tag_name.startswith("/")
            tag_name = raw_tag_name[1:].lower() if is_closing else raw_tag_name.lower()
            is_self_closing = attributes.strip().endswith("/") or tag_name in {"break"}

            # Ignore unsupported tags
            if tag_name not in SUPPORTED_TAGS:
                continue

            if is_self_closing:
                # Normalize self-closing tag
                clean_attrs = attributes.rstrip("/").strip()
                result_parts.append(f"<{tag_name} {clean_attrs}/>" if clean_attrs else f"<{tag_name}/>")
            elif is_closing:
                # Closing tag: LIFO unwind
                if tag_name in stack:
                    # Pop until we match the closing tag
                    while stack:
                        top_tag = stack.pop()
                        result_parts.append(f"</{top_tag}>")
                        if top_tag == tag_name:
                            break
                else:
                    # Orphan closing tag, discard
                    continue
            else:
                # Opening tag: push to LIFO stack
                clean_attrs = attributes.strip()
                result_parts.append(f"<{tag_name} {clean_attrs}>" if clean_attrs else f"<{tag_name}>")
                stack.append(tag_name)

        # Append remaining text
        remaining = cleaned[last_idx:]
        if remaining:
            result_parts.append(remaining)

        # LIFO Close any dangling tags left on the stack
        while stack:
            dangling_tag = stack.pop()
            result_parts.append(f"</{dangling_tag}>")

        return "".join(result_parts)

class TTSService:
    def __init__(self):
        self.output_dir = Path(settings.AUDIO_STORAGE_PATH)
        self.output_dir.mkdir(parents=True, exist_ok=True)

    def select_voice(self, text: str) -> str:
        """Chooses Malayalam voice if the response is predominantly Malayalam, otherwise English."""
        malayalam_chars = sum(1 for c in text if 0x0D00 <= ord(c) <= 0x0D7F)
        if malayalam_chars > 10:
            return settings.TTS_MALAYALAM_VOICE
        return settings.TTS_DEFAULT_VOICE

    async def generate_speech(self, raw_ssml: str, fallback_text: str = "") -> str:
        """
        Processes SSML with LIFO sanitizer, runs edge-tts synthesis,
        and saves MP3 file to disk. Returns the relative web URL.
        """
        sanitized_content = SSMLStackParser.sanitize_and_balance(raw_ssml)
        plain_text = re.sub(r'<[^>]+>', '', sanitized_content).strip() or fallback_text or "Please try again."
        voice = self.select_voice(plain_text)
        
        file_id = f"tutor_{uuid.uuid4().hex[:12]}.mp3"
        file_path = self.output_dir / file_id
        
        # Build SSML document wrapper
        ssml_doc = (
            f"<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
            f"<voice name='{voice}'>{sanitized_content}</voice>"
            f"</speak>"
        )

        try:
            # First attempt synthesis with structured SSML
            communicate = edge_tts.Communicate(text=ssml_doc, voice=voice)
            await communicate.save(str(file_path))
        except Exception as ssml_err:
            logger.warning(f"Edge-TTS SSML synthesis failed ({ssml_err}), falling back to plain text synthesis.")
            try:
                communicate_plain = edge_tts.Communicate(text=plain_text, voice=voice)
                await communicate_plain.save(str(file_path))
            except Exception as e:
                logger.error(f"TTS plain text fallback failed: {str(e)}", exc_info=True)
                raise RuntimeError(f"Audio synthesis failed: {str(e)}")

        audio_url = f"/static/audio/{file_id}"
        logger.info(f"Synthesized TTS audio saved: {file_path} -> {audio_url}")
        return audio_url

tts_service = TTSService()
