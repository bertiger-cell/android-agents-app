# Architektur – android-agents-app

## Zweck (v1)

Android-App zur Verwaltung von KI-Agenten (DB-gestützt) mit Chat über
drei Provider-Optionen: OpenRouter, OpenCode Zen, Ollama (lokal oder
cloud).

## Tatsächliches Pattern

Der bestehende Code nutzt einen zentralen Service statt einem Interface
pro Provider – das ist bewusst so beibehalten, kein Interface-Rewrite:

- `AIProviderService` (in `ai/`) – eine Klasse mit `sendMessage()`, die
  über `when(provider)` auf die passende private Methode verzweigt:
  `callOpenRouter()`, `callOllama()`, `callZen()`.
- `AIProvider` – Enum mit den Werten `OPENROUTER`, `OLLAMA`, `ZEN`
  (`ZEN` = OpenCode Zen).

Das ist ein einfaches, valides Strategy-Pattern für drei Provider.

## Provider-Details

### OpenRouter
- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response-Format: OpenAI-kompatibel (`OpenAIResponse`)

### OpenCode Zen
- Endpoint: `https://opencode.ai/zen/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response-Format: OpenAI-kompatibel (`OpenAIResponse`) – identisch zu
  OpenRouter (siehe "Bekannte offene Punkte")

### Ollama – lokal UND cloud
- Endpoint (beide Modi): `$baseUrl/api/chat`
- **Lokal:** `baseUrl` zeigt auf `http://localhost:11434` oder eine
  LAN-IP, kein API-Key nötig.
- **Cloud:** `baseUrl` = `https://ollama.com`, benötigt zusätzlich
  `Authorization: Bearer <apiKey>` – **dieser Header fehlt aktuell im
  Code** (siehe "Bekannte offene Punkte", Punkt 3).
- Response-Format: aktuell `OllamaResponse` mit Feld `response` – das
  passt zum `/api/generate`-Endpoint. `/api/chat` liefert normalerweise
  ein verschachteltes `message: {role, content}`-Objekt (siehe Punkt 2).

## Package-Struktur (Ist-Zustand)
```
com.agents.app/
├── ai/
│   └── AIProviderService.kt
├── automation/
├── db/
│   └── AgentDatabase.kt (+ DAOs)
├── models/
│   └── Agent, Message, ApiMessage, AgentResult, OpenAIResponse, OllamaResponse, ...
├── ui/
├── AgentRepository.kt
├── AgentsApplication.kt
└── MainActivity.kt
```

## Bekannte offene Punkte

1. **Conversation History wird nicht mitgeschickt.**
   `AgentRepository.chat()` baut nur `system` + neue `user`-Message,
   ruft `getMessagesByAgent()` nie ab. Kein Chat-Gedächtnis innerhalb
   einer Session.
2. **Ollama-Response-Parsing vermutlich falsches Feld.** `response`
   statt `message.content` für den `/api/chat`-Endpoint – noch nicht
   anhand der `OllamaResponse`-Datenklasse verifiziert.
3. **Ollama-Cloud-Auth fehlt.** Kein Authorization-Header in
   `callOllama()`, wenn `baseUrl` auf `https://ollama.com` zeigt. Ohne
   diesen Header funktioniert der Cloud-Modus nicht.
4. **Code-Duplikation** zwischen `callOpenRouter()` und `callZen()` –
   beide OpenAI-kompatibel, könnten eine gemeinsame private Methode mit
   Endpoint als Parameter nutzen.

## Non-Goals für v1
- Tool-Calling / Function-Calling
- Mehrere Agenten gleichzeitig im Gespräch / Orchestrierung
- On-Device-Inferenz (das macht bereits AI Workbench / Private Agent)
- RAG
- Streaming-Antworten

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
