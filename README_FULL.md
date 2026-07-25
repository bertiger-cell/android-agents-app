# Android Agents

Multi-Provider KI-Agent-App fuer Android. Verwalte spezialisierte KI-Agenten und chate mit ihnen ueber OpenRouter, OpenCode Zen oder Ollama.

## Demo

1. App installieren und oeffnen
2. Unter **Settings** API-Keys fuer mindestens einen Provider eintragen
3. **+** druecken und einen Agenten erstellen (Name, Provider, Modell waehlen)
4. Auf den Agenten tippen und loschatten

## Features

### Agent-Management
- Erstelle Agenten mit eigener Identitaet: Name, Beschreibung, System-Prompt
- Waehle den Provider und das Modell pro Agent
- Temperatur-Einstellung ueber Slider (0.0 - 1.0)
- Chat-Verlauf wird automatisch in Room-Datenbank gespeichert
- Nachrichten zeigen Token-Verbrauch an

### Drei Provider

**OpenRouter** — Zugriff auf hunderte Modelle (GPT-4, Claude, Llama, Mistral, ...). Benötigt einen API Key von [openrouter.ai](https://openrouter.ai).

**OpenCode Zen** — OpenCode's eigener API-Endpoint. Benötigt einen API Key.

**Ollama** — Lokal oder in der Cloud.
- *Lokal:* Ollama auf demselben Gerät oder im LAN starten, Base URL eintragen (z.B. `http://127.0.0.1:11434`). Kein API Key noetig.
- *Cloud:* `https://ollama.com` als Base URL plus API Key von [ollama.com](https://ollama.com).
- Modell-Auswahl ueber Dropdown (wird automatisch via `/api/tags` geladen).

### Einstellungen
- Jeder Provider hat eigene Credentials-Felder
- API-Keys werden persistent via Jetpack DataStore gespeichert
- Ollama-Verbindungstest prueft ob der Server erreichbar ist

## Architektur

```
com.agents.app/
├── ai/                         # AIProviderService - alle Provider-Logik
│   └── AIProviderService.kt    # sendMessage(), testOllamaConnection()
├── data/                       # Credentials-Verwaltung
│   └── ProviderCredentialsRepository.kt
├── db/                         # Room Database
│   ├── AgentDao.kt
│   ├── MessageDao.kt
│   └── AgentDatabase.kt
├── models/                     # Data Klassen
│   └── AgentModels.kt          # Agent, Message, API Models, Enums
├── ui/
│   ├── AgentViewModel.kt       # Zentraler ViewModel
│   ├── screens/
│   │   ├── AgentListScreen.kt  # Uebersicht aller Agenten
│   │   ├── ChatScreen.kt       # Chat-Interface
│   │   ├── CreateAgentScreen.kt# Agent erstellen
│   │   └── SettingsScreen.kt   # Provider-Einstellungen
│   ├── navigation/
│   │   └── AppNavigation.kt    # NavHost + State-Wiring
│   └── theme/                  # Material 3 Theme
├── automation/                 # Foreground Service (v2)
├── AgentRepository.kt          # DB-Operationen + AI-Aufrufe
├── AgentsApplication.kt        # App-Initialisierung
└── MainActivity.kt             # Entry Point
```

### Datenfluss

```
User tippt Nachricht
  → AgentViewModel.sendMessage()
    → credentials.value[provider] laden
    → AgentRepository.chat()
      → Messages aus DB laden (History)
      → User-Message in DB speichern
      → AIProviderService.sendMessage() aufrufen
        → callOpenAiCompatible() (OpenRouter/Zen)
        → callOllama() (Ollama lokal/cloud)
      → Assistant-Response in DB speichern
  → UI aktualisiert sich ueber StateFlow
```

## Build

```bash
# Voraussetzungen
# - Android SDK (API 34)
# - JDK 8+

# Debug-Build
./gradlew assembleDebug

# Release-Build
./gradlew assembleRelease

# Tests
./gradlew test

# Lint
./gradlew lint
```

APK liegt nach dem Build unter `app/build/outputs/apk/`.

## Technologien

| Komponente | Library |
|------------|---------|
| UI | Jetpack Compose + Material 3 |
| Datenbank | Room (SQLite) |
| Persistenz | DataStore Preferences |
| HTTP | OkHttp 4.12 |
| JSON | Gson |
| Navigation | Navigation Compose 2.7 |
| State | ViewModel + Kotlin StateFlow |
| Async | Kotlin Coroutines |

## Minimum

- Android 8.0 (API 26)
- Keine Internet-Berechtigung noetig fuer lokalen Ollama-Modus

## Roadmap (v2)

- Tool-Calling / Function-Calling
- Streaming-Antworten
- Multi-Agenten-Orchestrierung
- RAG (Retrieval-Augmented Generation)
- On-Device-Inferenz via AI Workbench

## License

MIT
