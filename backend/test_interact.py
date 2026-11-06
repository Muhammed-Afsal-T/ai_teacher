import asyncio
import io
import wave
import struct
import httpx

def generate_dummy_wav() -> bytes:
    """Generates a small 1-second 16kHz mono WAV file in memory."""
    buf = io.BytesIO()
    with wave.open(buf, 'wb') as wav_file:
        wav_file.setnchannels(1)        # mono
        wav_file.setsampwidth(2)       # 16-bit
        wav_file.setframerate(16000)   # 16kHz
        # Write 1 second of silence / soft tone
        samples = [int(1000 * 0.1) for _ in range(16000)]
        packed = struct.pack(f'<{len(samples)}h', *samples)
        wav_file.writeframes(packed)
    return buf.getvalue()

async def test_endpoint():
    url = "http://127.0.0.1:8000/api/v1/interact"
    wav_bytes = generate_dummy_wav()
    
    files = {
        "audio": ("test_voice.wav", wav_bytes, "audio/wav")
    }
    data = {
        "user_id": "test_user_cli"
    }
    
    print(f"Sending test audio payload to {url}...")
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(url, files=files, data=data)
            print(f"Status Code: {resp.status_code}")
            if resp.status_code == 200:
                print("Response payload:")
                import json
                print(json.dumps(resp.json(), indent=2))
            else:
                print(f"Error: {resp.text}")
    except Exception as e:
        print(f"Connection failed (is the backend running?): {e}")

if __name__ == "__main__":
    asyncio.run(test_endpoint())
