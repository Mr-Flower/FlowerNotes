# CLAUDE.md — FlowerNotes

> Il nome dell'app si scrive **FlowerNotes** (tutto attaccato), decisione dell'utente.

Questo file fornisce contesto persistente a Claude Code per questo progetto. Aggiornalo man mano che le decisioni si consolidano.

## Cos'è il progetto

**Flower Notes**: app Android nativa che:
1. Ascolta un comando vocale (es. "devo andare dal barbiere domani alle 15")
2. Estrae titolo, data, ora e reminder tramite un LLM esterno (provider scelto dall'utente)
3. Mostra una schermata di conferma/modifica con i campi pre-compilati
4. Crea l'evento sul calendario del dispositivo (sincronizzato con Google Calendar)
5. Mantiene una lista degli eventi creati così, consultabile e modificabile
6. Permette anche inserimento manuale (testuale) come alternativa alla voce

Repo GitHub: `Mr-Flower/FlowerNotes` — distribuzione APK via GitHub Releases.

## Stack tecnico

- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose + Material 3 (colori dinamici su Android 12+)
- **Riconoscimento vocale**: `SpeechRecognizer` di Android (nativo), lingua it-IT
- **Parsing linguaggio naturale**: chiamata API a un LLM esterno (vedi sotto), non offline in questa fase
- **Calendar**: `CalendarContract` (Calendar Provider di sistema) — scrive sul calendario
  dell'account Google che sincronizza da solo, senza OAuth. La Google Calendar API v3 +
  OAuth2 resta un'evoluzione futura (richiede progetto Google Cloud Console dell'utente).
  Dopo insert/delete, `CalendarWriter` chiama `ContentResolver.requestSync`
  (MANUAL+EXPEDITED) sull'account del calendario per ridurre la latenza di
  sincronizzazione col cloud (2026-07-08).
- **Persistenza locale**: DataStore Preferences (lista eventi come JSON, impostazioni, API key).
  Niente Room per ora (evita KSP).
- **HTTP/JSON**: HttpURLConnection + org.json (built-in Android) — nessuna libreria di rete esterna
- **Build**: Gradle wrapper (`./gradlew`), AGP 8.7, Kotlin 2.0, compileSdk 35, minSdk 26
- **Target**: distribuzione solo tramite GitHub Releases (APK sideload), NON Play Store

## Provider LLM (architettura BYOK)

**Decisione (2026-07-07)**: provider principale Google Gemini — l'utente ha chiesto di rimuovere Claude/OpenAI. L'utente fornisce la propria API key nelle impostazioni e può scegliere il modello (lista in `llm/GeminiModels.kt`, default `gemini-2.5-flash`). Nessuna key hardcoded; le key sono salvate in DataStore, solo sul dispositivo.

**Aggiunta (2026-07-08)**: secondo provider **Ollama** self-hosted (`llm/OllamaProvider.kt`) — l'utente inserisce nelle impostazioni l'indirizzo del proprio server (es. container sulla LAN, `http://ip:11434`) e il nome del modello. Usa l'endpoint nativo `/api/chat` con `stream:false` e `format:"json"`. Il manifest ha `usesCleartextTraffic="true"` per l'HTTP in chiaro sulla LAN. La scelta del provider è in `Settings.llmProvider` (enum `LlmProviderType`).

Interfaccia comune (`llm/LlmProvider.kt`):
```kotlin
interface LlmProvider {
    suspend fun estraiEvento(testo: String): EventoData
}
```

Il prompt condiviso è in `llm/ExtractionPrompt.kt` e richiede output JSON puro; il parsing tollera eventuali code fence.

**Gestione errori (2026-07-08)**: `HttpClient` ritenta fino a 3 volte con backoff su HTTP 429/5xx (tipico il 503 "overloaded" di Gemini) e sugli errori di rete; il 503 ha un messaggio dedicato. Gli errori sulla Home tornano a Idle da soli dopo 8 secondi (`HomeViewModel.showError`). Nel riconoscimento vocale, ERROR_CLIENT (5) è mappato su "non ho sentito nulla" e gli errori successivi a un `cancel()` volontario vengono ignorati.

**Nota UI (bug fix v0.2.0)**: i campi di testo che persistono su DataStore NON devono essere legati direttamente al Flow (binding bidirezionale asincrono = cursore che salta, testo che "torna indietro"). Usare stato locale nel ViewModel + salvataggio esplicito, come in `SettingsViewModel`.

## Struttura del codice

- `data/` — EventoData, SavedEvent, EventRepository (lista eventi), SettingsRepository
- `llm/` — interfaccia provider, prompt condiviso, tre implementazioni, HttpClient minimale
- `calendar/CalendarWriter.kt` — inserimento/cancellazione eventi via CalendarContract
- `speech/SpeechRecognizerManager.kt` — wrapper del SpeechRecognizer
- `ui/` — una cartella per schermata (home, confirm, list, manual, settings), un ViewModel per schermata
- La card `EventoCard` (in `ui/confirm/`) è il componente riusato da flusso vocale e manuale: entrambi convergono su ConfirmScreen

## Convenzioni di codice

- Nomi di file/classi in inglese, commenti in italiano dove aiutano la comprensione
- Package base: `com.flowernotes`
- Un `ViewModel` per schermata; ViewModel come `AndroidViewModel` (niente DI framework per ora)
- **i18n fatto in casa** (niente appcompat/risorse per lingua): tutte le stringhe UI
  vivono in `i18n/Strings.kt` (ItalianStrings/EnglishStrings), fornite ai composable
  via `LocalStrings`; il codice non-Compose (speech, LLM, calendario) usa il
  singleton `I18n`, che MainActivity tiene allineato alle impostazioni.
  **Default: inglese.** La lingua guida anche SpeechRecognizer e prompt Gemini.
- Logica di estrazione condivisa in `llm/ExtractEventUseCase` (usata da Home e Manuale)

## CI/CD e release

- `.github/workflows/build.yml` — build debug su ogni push/PR
- `.github/workflows/release.yml` — su tag `v*`: build release firmata + GitHub Release con APK
- Firma: keystore in `~/.keystores/flowernotes.jks` (MAI in repo), caricato nella CI via
  secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- Per rilasciare: aggiornare `versionCode`/`versionName` in `app/build.gradle.kts`, poi
  `git tag vX.Y.Z && git push --tags`
- Minify (R8) disattivato nella v0.x: riattivarlo più avanti con test su dispositivo
- **Processo attuale (dal 2026-07-07, su richiesta dell'utente)**: si itera
  ripubblicando sempre la release **v0.5.0** (eliminare release+tag con
  `gh release delete v0.5.0 -y --cleanup-tag`, poi ritaggare e pushare) finché
  l'utente non dichiara chiusa questa fase. **Chiedere SEMPRE conferma
  all'utente prima di pushare/ripubblicare una release.**
- Licenza: MIT (file LICENSE, mostrata anche nella pagina Info dell'app)

## Ambiente di sviluppo

- Sistema: Arch/CachyOS, ThinkPad L14 AMD
- JDK 17 in `~/.jdks/` (il default di sistema è JDK 26: NON usarlo per Gradle) —
  esportare `JAVA_HOME=$(ls -d ~/.jdks/jdk-17* | head -1)` prima di buildare
- Android SDK in `~/Android/Sdk` (cmdline-tools, platform-tools, platform 35, build-tools 35)
- Test su **telefono fisico via ADB** (`~/Android/Sdk/platform-tools/adb`), non emulatore
- Prima di ogni sessione di test: verificare `adb devices` mostri il telefono connesso

## Cosa NON fare senza chiedere

- Non introdurre dipendenze/librerie nuove senza spiegare perché
- Non modificare l'architettura BYOK per usare una key centralizzata
- Non aggiungere tracking/analytics di terze parti
- Non assumere target Play Store: niente vincoli di review Google da rispettare

## Stato attuale del progetto

- [x] Setup iniziale progetto Android + Gradle
- [x] Schermata Home con pulsante microfono
- [x] Integrazione SpeechRecognizer
- [x] Schermata conferma/modifica evento
- [x] Scrittura eventi sul calendario (CalendarContract; API Google + OAuth rimandata)
- [x] Provider LLM astratto + implementazioni Gemini/Claude/OpenAI
- [x] Schermata lista attività
- [x] Inserimento manuale
- [x] CI GitHub Actions + release APK firmato
- [x] Quick Settings Tile (apre l'app in modalità ascolto via EXTRA_START_LISTENING)
- [x] Date/time picker Material 3 nella schermata di conferma
- [x] Toggle colori dinamici Material You (default on; fallback palette rosa)
- [x] Icona: logo SVG fornito dall'utente (art/logo.svg, NON modificarlo),
      rasterizzato nei mipmap perché contiene una maschera raster incorporata.
      Per rigenerare: rsvg-convert a 260px, centrare su canvas nero 432px
      (foreground) e su trasparente con alpha=luminanza (monochrome), poi
      scalare a 324/216/162/108 nei mipmap-*dpi
- [x] SpeechRecognizer: istanza riusata + retry automatico su
      ERROR_SERVER_DISCONNECTED (codice 11) — NON distruggere/ricreare a ogni avvio
- [ ] Test end-to-end su telefono fisico (voce → LLM → calendario)
- [ ] Google Calendar API v3 + OAuth (opzionale, per calendari non sul dispositivo)
