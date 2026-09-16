# Offline Translator

Experimental Android speech translation project.

> **Current status:** the project is at its first working milestone: **German speech → German text**.  
> Translation, Czech TTS, Bluetooth audio routing, automatic language detection, and fully offline operation are **not implemented yet**.

The repository name reflects the long-term goal. Development currently starts with the simplest working online/system speech-recognition path and will add features one by one.

## Current milestone

The Android app currently:

- uses Android `SpeechRecognizer`
- listens in **German (`de-DE`)**
- shows partial recognition results while the user is speaking
- replaces partial hypotheses instead of appending them
- shows the final recognized text when the phrase is complete
- uses one recognition session per START press
- requests microphone permission at runtime
- does not use a paid speech API or API key

Test phrase used during development:

```text
Hallo, wie geht es dir? Ich möchte eine Katze haben.
```

A successful recognition currently produces text similar to:

```text
hallo wie geht es dir ich möchte eine Katze haben
```

Punctuation is intentionally not post-processed at this stage.

## Why the project is being built in small steps

The goal is to keep each stage independently testable.

The project previously became unnecessarily complicated by combining speech recognition, translation, TTS, Bluetooth routing, and offline processing at once. The current approach starts from a minimal speech-recognition app and adds only one capability after the previous one works reliably.

## Planned direction

The intended pipeline is:

```text
speech
  ↓
speech recognition
  ↓
source-language text
  ↓
Czech translation
  ↓
Czech text-to-speech
  ↓
Bluetooth headset
```

See [ROADMAP.md](ROADMAP.md) for the planned development stages.

## Android app

The native Android implementation is in:

```text
android/
```

Main speech-recognition logic:

```text
android/app/src/main/java/com/example/dictationde/MainActivity.kt
```

The recognizer is currently configured for:

```text
de-DE
```

and uses `RecognizerIntent.EXTRA_PARTIAL_RESULTS`.

### Partial-result handling

Android speech recognition may return a sequence such as:

```text
hallo
hallo wie
hallo wie geht
hallo wie geht es
```

These are successive hypotheses, not separate pieces of text.

The app therefore replaces the current partial result:

```kotlin
currentPartial = text
```

instead of appending every hypothesis. This avoids output such as:

```text
hallo hallo wie hallo wie geht ...
```

## Building the Android project

Requirements:

- Android Studio with a recent Android SDK
- JDK 17
- Android device or emulator with a speech-recognition service
- microphone permission
- network access may be used by the system speech-recognition service

Open the `android` directory as the Android project and build/run the `app` module.

The project currently targets Android SDK 34 and has a minimum SDK of 26.

## Web prototype

The repository also contains a small React/Vite speech-recognition prototype in `src/`.

It uses the browser Web Speech API where available. The native Android implementation is the primary test target for the project.

## Cost and privacy

The current implementation does **not** call Gemini API, Google Cloud Speech-to-Text API, OpenAI API, or another usage-billed API.

Android's system speech-recognition service may process audio online depending on the device and installed recognition service. Fully offline recognition remains a later project goal.

No audio recording history is intentionally stored by the app.

## Known limitations

- German is currently hard-coded as the recognition language.
- One START press runs one recognition session.
- Punctuation depends on the underlying recognition service and is currently not added by the app.
- Translation is not implemented.
- Czech TTS is not implemented.
- Bluetooth microphone/output routing is not implemented.
- Fully offline recognition is not implemented.

## Acknowledgement

Thanks to **Eddy Verbruggen** and the open-source
[nativescript-speech-recognition](https://github.com/EddyVerbruggen/nativescript-speech-recognition)
project. Its handling of partial versus final speech-recognition results helped clarify the correct approach for avoiding duplicated partial transcripts.

## License

No open-source license has been selected yet. Until a license is added, normal copyright rules apply.
