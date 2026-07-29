# Architektur – android-agents-app

## Zweck (v1.5)

Android-App zur Verwaltung von KI-Agenten (DB-gestützt) mit
Streaming-Chat über drei Provider: OpenRouter, OpenCode Zen,
Ollama (lokal oder cloud). Animierter Intro-Screen,
Agent-Vorlagen, Dark Theme mit intensiven Farben.

## Feature-Übersicht

- Streaming-Antworten (Token für Token via Flow)
- Model-Picker pro Provider (API-gestützt)
- Agent-Vorlagen (Code Assistant, Creative Writer, etc.)
- Animierter Intro-Screen mit wechselnden Sprüchen
- HomeScreen mit Uhrzeit und letzter Aktivität
- Dark Theme mit Shapes und intensiven Farben
- Screen-Transitions (fade + slide)

## Pattern

Zentraler Service statt Interface pro Provider:

- `AIProviderService` (in `ai/`) – eine Klasse mit:
  - `sendMessage()` – synchron (Legacy, für AgentRepository)
  - `streamMessage()` – streaming, gibt `Flow<AgentResult>` zurück
  - `streamOpenAiCompatible()` – SSE-Parsing für OpenRouter/Zen
  - `streamOllama()` – NDJSON-Parsing für Ollama
  - `fetchOpenAICompatibleModels()` – Model-Liste laden
  - `testOllamaConnection()` – Verbindungstest
- `AIProvider` – Enum: `OPENROUTER`, `OLLAMA`, `ZEN`

## Provider-Details

### OpenRouter
- Chat: `https://openrouter.ai/api/v1/chat/completions` (SSE)
- Models: `https://openrouter.ai/api/v1/models`
- Auth: `Authorization: Bearer <apiKey>`

### OpenCode Zen
- Chat: `https://opencode.ai/zen/v1/chat/completions` (SSE)
- Models: `https://opencode.ai/zen/v1/models`
- Auth: `Authorization: Bearer <apiKey>`

### Ollama (lokal + cloud)
- Chat: `$baseUrl/api/chat` (NDJSON Streaming)
- Models: `$baseUrl/api/tags`
- Lokal: `http://127.0.0.1:11434`, kein API-Key nötig
- Cloud: `https://ollama.com`, `Authorization: Bearer <apiKey>`

## Streaming-Architektur

```
AgentViewModel.sendMessage()
  → aiService.streamMessage() → Flow<AgentResult>
    → streamOpenAiCompatible() / streamOllama() → Flow<String>
      → OkHttp SSE / NDJSON Parsing
      → flowOn(Dispatchers.IO)
    → Token für Token emit(AgentResult(output=laufenderText))
  → collect { } updated _messages inkrementell
  → ChatScreen LaunchedEffect scrollt automatisch
```

## Package-Struktur
```
com.agents.app/
├── ai/
│   └── AIProviderService.kt
├── data/
│   └── ProviderCredentialsRepository.kt
├── db/
│   ├── AgentDao.kt
│   ├── MessageDao.kt
│   └── AgentDatabase.kt
├── models/
│   └── AgentModels.kt
├── ui/
│   ├── AgentViewModel.kt
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── screens/
│   │   ├── IntroScreen.kt      (animierter Splash)
│   │   ├── HomeScreen.kt       (Uhr, letzte Aktivität)
│   │   ├── AgentListScreen.kt
│   │   ├── ChatScreen.kt       (Streaming-Auto-Scroll)
│   │   ├── CreateAgentScreen.kt (Templates + Model-Picker)
│   │   └── SettingsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt            (Dark Theme, Shapes)
│       └── Type.kt
├── AgentRepository.kt
├── AgentsApplication.kt
└── MainActivity.kt
```

## Navigation
```
IntroScreen (4.2s) → HomeScreen → AgentListScreen → ChatScreen
                                    ↕                   ↕
                              CreateAgentScreen    SettingsScreen
```

## Provider-Zugangsdaten

Persistiert via Jetpack DataStore (Preferences):

- `openrouter_api_key`
- `opencode_zen_api_key`
- `ollama_base_url` (Default: `https://ollama.com`)
- `ollama_api_key` (optional, nur für Ollama-Cloud)

## Theme

Dark Mode erzwungen (`darkTheme = true, dynamicColor = false`):
- Primary: `#BB86FC` (Violett)
- Secondary: `#03DAC6` (Tuerkis)
- Background: `#0D0D0D`
- Surface: `#1A1A1A`
- Shapes: 8-32dp abgerundet

## Project Diary (Projekt-Tagebuch)

Ein einfaches Markdown-File `diary.md` im Projekt-Ordner (`folderPath`).
Dient als Shared Memory für alle Entwicklung-Agenten (Coder, Architekt).

### Zweck
- Entscheidungen festhalten (warum wurde X so implementiert?)
- Code-Review-Ergebnisse dokumentieren
- Offene Fragen / Blockaden notieren
- Kontext für nachfolgende Agenten-Sessions bereitstellen

### Format
```
# Project Diary – <Projektname>

## 2024-07-29 04:30 – Coder
Kurze Beschreibung der Änderung/Entscheidung.

## 2024-07-29 04:35 – Architekt
Review-Ergebnis, Genehmigung oder Einwand.
```

### Implementierung
- `AgentRepository.kt`: `readDiary(projectId)` und `appendToDiary(projectId, role, content)`
- Datei-Pfad: `{folderPath}/diary.md`
- Anlegen bei Projekt-Erstellung (leeres File)
- Anhängen nach jeder abgeschlossenen Änderung (via Repository-Methoden)
- Lesen am Anfang jeder Agenten-Session (via SESSION_START.md)

### Kein RAG
Das Diary ist ein plain-text Markdown-File, kein Vector Store.
Agenten lesen das komplette File und entscheiden selbst, was relevant ist.

## Non-Goals für v1
- Tool-Calling / Function-Calling
- Mehrere Agenten gleichzeitig / Orchestrierung
- On-Device-Inferenz
- RAG

## Versionierung
Diese Datei wird zuerst geändert, dann der Code – nicht umgekehrt.

## Provider-Zugangsdaten – Ist-Zustand & Ziel

### Ist-Zustand (Bug, Stand: Prüfung des echten Codes)
- Ein einziges globales Feld `_apiKey: MutableStateFlow<String>` in
  `AgentViewModel.kt` für ALLE Cloud-Provider zusammen (OpenRouter UND
  OpenCode Zen teilen sich denselben Wert).
- Kein Ollama-API-Key-Feld (Cloud-Modus nicht bedienbar, obwohl
  `AIProviderService.callOllama()` den Header bereits unterstützt).
- **Keine Persistenz.** Weder SharedPreferences noch DataStore (obwohl
  als Dependency vorhanden) noch Room werden genutzt. Bei App-Neustart
  bzw. Prozess-Kill durch Android ist jeder eingegebene Key weg.

### Ziel-Architektur
- Jetpack DataStore (Preferences) als Speicher, EIN Key pro Provider:
  - `openrouter_api_key`
  - `opencode_zen_api_key`
  - `ollama_base_url` (Default: `http://127.0.0.1:11434`)
  - `ollama_api_key` (optional, nur für Ollama-Cloud-Modus)
- Neue Klasse `ProviderCredentialsRepository` (in `data/`), kapselt
  DataStore-Zugriff, stellt `Flow<ProviderCredentials>` bereit sowie
  `suspend fun update...()`-Funktionen pro Feld.
- `AgentViewModel` bezieht Zugangsdaten aus diesem Repository statt
  eigener `MutableStateFlow<String>`-Felder.
- `AgentRepository.chat()` / `AIProviderService.sendMessage()` wählen
  anhand von `agent.provider` das passende Credential aus dem
  Repository aus, statt einen einzigen global übergebenen `apiKey`-
  Parameter zu erwarten.
