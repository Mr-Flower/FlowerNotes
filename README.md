# 🌸 FlowerNotes

Android app that turns your voice into calendar events: dictate a commitment ("dentist on Thursday at 9") and FlowerNotes interprets it with Gemini, lets you confirm the details and saves it to your phone's calendar (synced with Google Calendar).

*FlowerNotes è disponibile anche in italiano: cambia lingua dalle impostazioni.*

## Download

Grab the latest APK from the [Releases](../../releases/latest) page and install it on your phone (you need to allow installs from unknown sources). The app is **not** on the Play Store.

## How it works

1. **Dictate** the event by tapping the microphone (or type it with manual entry) — a Quick Settings tile can open the app already listening
2. **Google Gemini** extracts title, date, time, location and reminder
3. **Confirm or edit** the pre-filled fields (Material 3 date/time pickers)
4. The event is created on the **device calendar** (your Google account syncs it with Google Calendar)
5. Created events stay browsable and deletable in the built-in list

## Configuration (BYOK — Bring Your Own Key)

The app ships with no API key: in **Settings**, paste your Gemini key — it never leaves the device. Create one for free at [aistudio.google.com/apikey](https://aistudio.google.com/apikey). You can also pick which Gemini model to use (default: `gemini-2.5-flash`) or type any custom model id.

## Features

- 🎙️ Native Android speech recognition (English and Italian)
- 🤖 Event extraction via Gemini (BYOK, model selectable)
- 🎨 Material You: dynamic colors from your wallpaper or six preset accents, light/dark/system theme
- 🌍 English and Italian UI, switchable in settings
- ⚡ Quick Settings tile that opens the app already listening
- 🔒 No tracking, no app servers, MIT-licensed

## Permissions

- **Microphone** — speech recognition (native Android SpeechRecognizer)
- **Calendar** — event creation through the system Calendar Provider
- **Internet** — only for the call to your configured LLM provider

## Building from source

Requirements: JDK 17 and the Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

## Stack

Kotlin · Jetpack Compose · Material 3 · SpeechRecognizer · CalendarContract · DataStore

## License

FlowerNotes is free software released under the [MIT license](LICENSE).
