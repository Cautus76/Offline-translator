# Roadmap

Development is intentionally incremental. A stage should be tested before the next stage is added.

## 1. German dictation — working

**Goal:** German speech → German text.

Current state:

- Android `SpeechRecognizer`
- language: `de-DE`
- partial results
- clean final result
- no duplicated partial hypotheses
- one START press = one recognition session

Status: **working**

## 2. German text → Czech text

Add translation only after speech recognition remains stable.

Requirements:

- target language is always Czech
- no paid per-request API
- no API key if avoidable
- keep speech-recognition code unchanged
- first test translation on the final recognized sentence

## 3. Czech text-to-speech

Add Czech TTS after translation works.

Requirements:

- Czech output only
- simple system TTS first
- no audio history

## 4. Continuous listening

Move from one phrase per START press to repeated/continuous recognition.

Things to test:

- recognizer restart behavior
- pause handling
- duplicated final results
- system start/end beeps
- battery use
- long-running stability

## 5. Bluetooth audio

Add:

- Bluetooth microphone input
- Czech TTS output to the connected headset

This stage should not change the already-working speech/translation logic.

## 6. Automatic source-language detection

Add automatic recognition of the incoming language.

Initial target languages:

- German
- Polish
- Ukrainian
- Arabic
- Turkish
- Persian / Farsi

Czech should be treated as the target language, not translated away from Czech.

## 7. Offline migration

Once the full online/system prototype works well, replace components with offline alternatives where practical.

Long-term target:

```text
offline speech recognition
        ↓
offline translation
        ↓
offline Czech TTS
```

The offline version should be evaluated for:

- model size
- CPU usage
- battery drain
- latency
- recognition quality
- language coverage

## Development rule

Change **one major behavior at a time**.

If a new stage breaks something that already worked, fix the regression before adding another feature.
