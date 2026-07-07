# 🌸 FlowerNotes

App Android che trasforma la voce in eventi di calendario: detta un impegno in italiano ("devo andare dal barbiere domani alle 15") e FlowerNotes lo interpreta con un LLM, ti fa confermare i dettagli e lo salva sul calendario del telefono (sincronizzato con Google Calendar).

## Download

Scarica l'ultimo APK dalla pagina [Releases](../../releases/latest) e installalo sul telefono (serve abilitare l'installazione da origini sconosciute). L'app **non** è sul Play Store.

## Come funziona

1. **Detta** l'evento toccando il microfono (o scrivilo con l'inserimento manuale)
2. **Google Gemini** estrae titolo, data, ora, luogo e promemoria
3. **Confermi o modifichi** i campi pre-compilati (con date/time picker Material 3)
4. L'evento viene creato sul **calendario del dispositivo** (l'account Google lo sincronizza con Google Calendar)
5. Gli eventi creati restano consultabili ed eliminabili nella lista interna

## Configurazione (BYOK — Bring Your Own Key)

L'app non include nessuna API key: nelle **Impostazioni** inserisci la tua chiave Gemini, che resta solo sul dispositivo. La crei gratis su [aistudio.google.com/apikey](https://aistudio.google.com/apikey). Puoi anche scegliere quale modello Gemini usare (default: `gemini-2.5-flash`).

## Permessi richiesti

- **Microfono** — riconoscimento vocale (SpeechRecognizer nativo di Android)
- **Calendario** — creazione degli eventi tramite il Calendar Provider di sistema
- **Internet** — solo per la chiamata al provider LLM scelto

## Build da sorgente

Requisiti: JDK 17 e Android SDK (compileSdk 35).

```bash
./gradlew assembleDebug
# APK in app/build/outputs/apk/debug/app-debug.apk
```

## Stack

Kotlin · Jetpack Compose · Material 3 · SpeechRecognizer · CalendarContract · DataStore

## Licenza

FlowerNotes è software libero distribuito con licenza [MIT](LICENSE).
