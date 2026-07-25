# Android Agents

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android" alt="Android">
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin" alt="Kotlin">
<img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4" alt="Compose">
<img src="https://img.shields.io/badge/License-MIT-blue" alt="License">

> Erstelle KI-Agenten mit eigener Identitaet. Chate mit ihnen ueber verschiedene Provider.

## Was kann die App?

### Agent-System
- **Agenten erstellen** mit Name, Beschreibung, System-Prompt, Modell und Temperatur
- **Chat-Verlauf** pro Agent in Room-Datenbank gespeichert
- **Agent-Typen**: General, Researcher, Coder, Writer, Automator, Custom

### Provider-Unterstützung
| Provider | Modus | Auth |
|----------|-------|------|
| **OpenRouter** | Cloud | API Key |
| **OpenCode Zen** | Cloud | API Key |
| **Ollama** | Cloud | API Key + Base URL |
| **Ollama** | Lokal | Base URL (z.B. `http://127.0.0.1:11434`) |

### Ollama Modell-Auswahl
- Modelle werden automatisch via `/api/tags` geladen
- Dropdown-Auswahl bei der Agent-Erstellung
- Modellname, Parameter-Size und Quantization werden angezeigt

### Einstellungen
- Separate API-Key-Felder pro Provider
- Persistente Speicherung ueber Jetpack DataStore
- Ollama-Verbindungstest mit einem Klick

## Build & Install

```bash
# Debug-Build
./gradlew assembleDebug

# APK finden unter:
# app/build/outputs/apk/debug/app-debug.apk
```

## Stack

| Komponente | Technologie |
|------------|-------------|
| UI | Jetpack Compose + Material 3 |
| Datenbank | Room (SQLite) |
| Settings | DataStore Preferences |
| Networking | OkHttp + Gson |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow |

## Projektstruktur

```
com.agents.app/
├── ai/                 # Provider-Logik (OpenRouter, Zen, Ollama)
├── automation/         # Foreground Service (Skeleton fuer v2)
├── data/               # ProviderCredentialsRepository (DataStore)
├── db/                 # Room Database + DAOs
├── models/             # Data Klassen (Agent, Message, API Models)
├── ui/
│   ├── screens/        # AgentList, Chat, CreateAgent, Settings
│   ├── navigation/     # AppNavigation (NavHost)
│   └── theme/          # Farben, Typografie, Theme
├── AgentRepository.kt  # DB + AI Calls
├── AgentsApplication.kt
└── MainActivity.kt
```

## License

MIT
