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
