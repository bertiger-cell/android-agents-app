# Architektur – android-agents-app

## Zweck (v1)

Android-App zur Verwaltung von KI-Agenten (DB-gestützt) mit Chat über
drei Provider-Optionen: OpenRouter, OpenCode Zen, Ollama (lokal oder
cloud).

## Pattern

Zentraler Service statt Interface pro Provider – bewusst so für v1:

- `AIProviderService` (in `ai/`) – eine Klasse mit `sendMessage()`,
  verzweigt über `when(provider)` via gemeinsamer
  `callOpenAiCompatible()` für OpenRouter/Zen und `callOllama()` für
  Ollama.
- `AIProvider` – Enum: `OPENROUTER`, `OLLAMA`, `ZEN`

## Provider-Details

### OpenRouter
- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response: OpenAI-kompatibel (`OpenAIResponse`)

### OpenCode Zen
- Endpoint: `https://opencode.ai/zen/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response: OpenAI-kompatibel (`OpenAIResponse`)

### Ollama (lokal + cloud)
- Endpoint: `$baseUrl/api/chat`
- Lokal: `http://127.0.0.1:11434`, kein API-Key nötig
- Cloud: `https://ollama.com`, zusätzlich `Authorization: Bearer <apiKey>`
- Response: `OllamaResponse` mit `message.content`

## Package-Struktur
```
com.agents.app/
├── ai/
│   └── AIProviderService.kt
├── automation/
│   ├── AgentForegroundService.kt   (Skeleton für v2)
│   └── BootReceiver.kt             (Skeleton für v2)
├── data/
│   └── ProviderCredentialsRepository.kt
├── db/
│   └── AgentDatabase.kt (+ DAOs)
├── models/
│   └── AgentModels.kt (Agent, Message, ApiMessage, AgentResult,
│       OpenAIResponse, OllamaResponse, OllamaModel, ...)
├── ui/
│   ├── AgentViewModel.kt
│   ├── navigation/
│   │   └── AppNavigation.kt
│   ├── screens/
│   │   ├── AgentListScreen.kt
│   │   ├── ChatScreen.kt
│   │   ├── CreateAgentScreen.kt
│   │   └── SettingsScreen.kt
│   └── theme/
├── AgentRepository.kt
├── AgentsApplication.kt
└── MainActivity.kt
```

## Provider-Zugangsdaten

Persistiert via Jetpack DataStore (Preferences) in
`ProviderCredentialsRepository`:

- `openrouter_api_key`
- `opencode_zen_api_key`
- `ollama_base_url` (Default: `https://ollama.com`)
- `ollama_api_key` (optional, nur für Ollama-Cloud)

`AgentViewModel` stellt `credentials: StateFlow<ProviderCredentials>`
bereit. `sendMessage()` liest pro `agent.provider` den passenden Wert
aus `credentials.value`.

## Conversation History

`AgentRepository.chat()` lädt `getMessagesByAgent()` und baut die
volle Nachrichtenliste (system + history + user) auf, bevor der
Provider aufgerufen wird.

## Non-Goals für v1
- Tool-Calling / Function-Calling
- Mehrere Agenten gleichzeitig / Orchestrierung
- On-Device-Inferenz
- RAG
- Streaming-Antworten

## Versionierung
Diese Datei wird zuerst geändert, dann der Code – nicht umgekehrt.
