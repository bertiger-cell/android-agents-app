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
  `callOpenAiCompatible()` (für OpenRouter und Zen) und `callOllama()`.
- `AIProvider` – Enum mit den Werten `OPENROUTER`, `OLLAMA`, `ZEN`
  (`ZEN` = OpenCode Zen).

Das ist ein einfaches, valides Strategy-Pattern für drei Provider.
OpenRouter und Zen teilen sich `callOpenAiCompatible()` mit jeweils
anderem Endpoint und optionalen Extra-Headern.

## Provider-Details

### OpenRouter
- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response-Format: OpenAI-kompatibel (`OpenAIResponse`)
- Extra-Header: `HTTP-Referer`, `X-Title`

### OpenCode Zen
- Endpoint: `https://opencode.ai/zen/v1/chat/completions`
- Auth: `Authorization: Bearer <apiKey>`
- Response-Format: OpenAI-kompatibel (`OpenAIResponse`)
- Identisches Format wie OpenRouter – gemeinsame Methode

### Ollama – lokal UND cloud
- Endpoint (beide Modi): `$baseUrl/api/chat`
- **Lokal:** `baseUrl` zeigt auf `http://localhost:11434` oder eine
  LAN-IP, kein API-Key nötig.
- **Cloud:** `baseUrl` = `https://ollama.com`, benötigt zusätzlich
  `Authorization: Bearer <apiKey>` – wird nur gesetzt wenn
  `apiKey.isNotBlank()` ist.
- Response-Format: `OllamaResponse` mit `message.content` (korrekt
  für `/api/chat`-Endpoint).

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

1. **[Behoben] Conversation History wird jetzt mitgeschickt.**
   `AgentRepository.chat()` ruft `getMessagesByAgent()` ab und baut
   die vollständige Nachrichtenhistorie (system + history + user) auf.

2. **[Behoben] Ollama-Response-Parsing korrigiert.** `message.content`
   wird jetzt korrekt für den `/api/chat`-Endpoint gelesen.

3. **[Behoben] Ollama-Cloud-Auth implementiert.** Authorization-Header
   wird gesetzt wenn `apiKey.isNotBlank()` ist.

4. **[Behoben] Code-Duplikation entfernt.** `callOpenRouter()` und
   `callZen()` wurden zu `callOpenAiCompatible()` zusammengelegt.

## Non-Goals für v1
- Tool-Calling / Function-Calling
- Mehrere Agenten gleichzeitig im Gespräch / Orchestrierung
- On-Device-Inferenz (das macht bereits AI Workbench / Private Agent)
- RAG
- Streaming-Antworten

## Versionierung
Diese Datei wird zuerst geändert, dann der Code – nicht umgekehrt.
