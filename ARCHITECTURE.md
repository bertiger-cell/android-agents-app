# Architektur – Agent Studio (android-agents-app)

## Zweck (v2)

Android-App zur Verwaltung von KI-Projekten und -Agenten. Jedes Projekt startet
mit einem **Architect-Interview**, das einen Projekt-Plan als JSON generiert und
daraus Markdown-Dokumente sowie spezialisierte Agenten erstellt. Anschließend kann
mit den Agenten über **OpenRouter**, **OpenCode Zen** oder **Ollama** (Cloud/lokal)
gestreamt gechattet werden.

## Feature-Übersicht

- Dashboard „Agent Studio“ mit Willkommens-/Übersichts-Ansicht
- Projekt-CRUD (erstellen, editieren, löschen mit Bestätigung)
- Architect-Discovery: 3-Phasen-Interview, JSON-Extraktion, Summary mit Prompt-Editing
- Automatische Datei-Generierung im Projekt-Ordner
- Agenten aus `suggested_agents` automatisch anlegen
- Streaming-Chat (Token für Token via Flow)
- Multi-Provider: OpenRouter, OpenCode Zen, Ollama (Cloud + lokal)
- Model-Picker pro Provider (API-gestützt)
- Settings mit persistenten API-Keys und Architect-Konfiguration
- Projekt-Dateien in der App anzeigen (`.md`, `.json`)
- Discovery-Interview jederzeit neu starten
- 5 Unit-Test-Suiten

## Optional Features

### Architect Agent
- **Status:** optional, standardmäßig deaktiviert
- **Toggle Location:** Settings
- **Behavior:**
  - **Enabled (`true`):** Neues Projekt startet automatisch das Architect-Interview
    in einer Chat-Session, erzeugt den Projektplan und schreibt die Scaffold-Dateien.
  - **Disabled (`false`):** Neues Projekt wird ohne Architect-Interview erstellt und
    direkt im Projekt-Detail angezeigt.
- **Implementation:** DataStore-Key `architect_enabled` über
  `ArchitectConfigRepository.isArchitectEnabled`

## Pattern

Zentraler Service statt Interface pro Provider:

- `AIProviderService` (in `ai/`) – eine Klasse mit:
  - `streamMessage()` – Streaming, gibt `Flow<AgentResult>` zurück
  - `streamOpenAiCompatible()` – SSE-Parsing für OpenRouter/Zen
  - `streamOllama()` – NDJSON-Parsing für Ollama
  - `fetchOpenAICompatibleModels()` – Model-Liste laden
  - `fetchOllamaModels()` – Ollama `/api/tags`
  - `testOllamaConnection()` – Verbindungstest
- `AIProvider` – Enum: `OPENROUTER`, `OLLAMA`, `ZEN`

## Package-Struktur

```
com.agents.app/
├── ai/
│   └── AIProviderService.kt
├── data/
│   ├── ArchitectConfigRepository.kt
│   ├── ProjectFileWriter.kt
│   └── ProviderCredentialsRepository.kt
├── db/
│   ├── AgentDao.kt
│   ├── AgentDatabase.kt
│   ├── ChatSessionDao.kt
│   ├── MessageDao.kt
│   └── ProjectDao.kt
├── models/
│   └── AgentModels.kt
├── ui/
│   ├── AgentViewModel.kt
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── screens/
│   │   ├── ArchitectSummaryScreen.kt
│   │   ├── ChatScreen.kt
│   │   ├── CreateAgentScreen.kt
│   │   ├── ProjectDetailWithTabsScreen.kt
│   │   ├── ProjectListScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── AgentRepository.kt
├── AgentsApplication.kt
└── MainActivity.kt
```

## Navigation

```
Projects/Dashboard (start)
  → Project Detail (Agenten / Sessions / Dateien)
    → Create Agent
  → Settings

Modals (überlagern Navigation):
  → Chat (wenn Session selektiert)
  → Architect Summary (wenn Projektplan erstellt)
```

Routen in `AppNavigation.kt`:
- `projects` – Dashboard/Projektliste
- `project/{projectId}` – Detail mit 3 Tabs
- `create-agent/{projectId}` – Agent anlegen
- `settings` – Einstellungen

## Datenbank (Room, Version 2)

Tabellen:
- `projects` – Projektmetadaten + `folderPath`
- `agents` – Agent-Konfiguration, Fremdschlüssel auf Projekt (CASCADE)
- `chat_sessions` – Session pro Agent (CASCADE)
- `messages` – Chat-Nachrichten, Fremdschlüssel auf Session (CASCADE)

Alle Löschungen kaskadieren von Projekt → Agenten → Sessions → Nachrichten.

## Dateisystem

Projekt-Ordner unter `files/projects/<Name>_<ID>/`:

| Datei | Zweck |
|---|---|
| `diary.md` | Projekt-Tagebuch, Shared Memory |
| `discovery.json` | Roh-JSON des Architect-Plans |
| `ARCHITECTURE.md` | Architektur-Dokumentation |
| `ROADMAP.md` | Roadmap/Phasen |
| `RULES.md` | Projektregeln |
| `SKILLS.md` | Skills für das Projekt |
| `AGENTS.md` | Agenten-Rollen |

Generiert durch `ProjectFileWriter`.

## Architect-Flow

```
Projekt erstellen
  → createArchitectDiscoverySession()
    → createOrGetArchitectAgent() (Provider/Modell aus Settings)
    → Chat mit Architect (3 Phasen)
  → sendMessage() erkennt Architect-Antwort
    → extractAndParseArchitectJSON()
      → saveProjectScaffold() → ScaffoldParseResult
        → discovery.json speichern
  → showArchitectSummary = true
    → ArchitectSummaryScreen (Prompts editierbar)
  → createAgentsFromScaffold()
    → ProjectFileWriter (Markdown-Dateien)
    → Agenten aus suggestedAgents anlegen
    → Toast + Loading-Overlay
```

Fehler beim JSON-Parsing erscheinen als Assistant-Message im Chat.

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
  → AgentRepository.chat()
    → buildApiMessages() (System-Prompt + letzte 50 Nachrichten)
    → AIProviderService.streamMessage() → Flow<AgentResult>
      → streamOpenAiCompatible() / streamOllama() → Flow<String>
      → OkHttp SSE / NDJSON Parsing
      → flowOn(Dispatchers.IO)
    → Token für Token emit(AgentResult(output=laufenderText))
  → collect { } updated _messages inkrementell
  → ChatScreen LaunchedEffect scrollt automatisch
```

Die History wird für API-Calls auf die letzten 50 Nachrichten begrenzt
(`MAX_HISTORY_MESSAGES`), um Token-Verbrauch und Latenz bei großen Sessions
zu begrenzen.

## Settings / DataStore

Persistiert via Jetpack DataStore (Preferences):

- `provider_credentials`:
  - `openrouter_api_key`
  - `opencode_zen_api_key`
  - `ollama_base_url` (Default: `https://ollama.com`)
  - `ollama_api_key`
- `architect_config`:
  - `architect_system_prompt`
  - `architect_provider` (Default: `openrouter`)
  - `architect_model` (Default: `gpt-4o`)

## Theme

Material 3 mit Dynamic Color (Android 12+) und Fallback-Palette:

- Dark: Background `#0D0D0D`, Primary `#BB86FC`, Secondary `#03DAC6`
- Light: Background `#FFFBFE`, Primary `#6200EE`, Secondary `#03DAC6`
- Shapes: 8-32dp abgerundet

## Tests

5 Unit-Test-Suiten unter `app/src/test/`:
- JSON-Extraktion aus Architect-Antworten
- JSON-Struktur/Modelle (Gson)
- Agent-/Projekt-/Scaffold-Datenmodelle
- ArchitectSummary-Konvertierung
- Message-History-Begrenzung

## Non-Goals (aktuell geplant, nicht umgesetzt)

- LangChain4j / formale ToolRegistry
- On-Device-Inferenz (LiteRT/MediaPipe)
- Vision / Bildgenerierung
- Speech-to-Text / Text-to-Speech
- Multi-Agent-Orchestrierung / Debating
- RAG / Vector Store
- Geplante Automatisierung (WorkManager)

## Versionierung

Diese Datei wird zuerst geändert, dann der Code – nicht umgekehrt.
