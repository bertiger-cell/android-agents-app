# Architektur – android-agents-app

## Zweck (v1)

Eine Android-App, die eine Chat-Nachricht an einen von drei Cloud-Providern
(OpenAI, Anthropic, Ollama) schickt und die Antwort anzeigt.

**Das ist der gesamte Umfang von v1.** Kein Tool-Calling, keine Multi-Agenten-
Orchestrierung, kein RAG, keine On-Device-Inferenz. Diese Dinge kommen erst,
wenn v1 stabil läuft und getestet ist – siehe "Non-Goals" unten.

## Tech-Stack

- **Sprache/UI:** Kotlin, Jetpack Compose
- **Async:** Kotlin Coroutines + Flow
- **Netzwerk:** Retrofit + OkHttp (oder Ktor Client – EINE Entscheidung treffen,
  nicht beides parallel einbauen)
- **API-Key-Speicherung:** Jetpack DataStore (verschlüsselt), niemals im Code
  oder in Shared Preferences im Klartext
- **DI:** Hilt (falls das Projekt wächst) – für v1 reicht auch manuelles
  Constructor-Injection, keine Pflicht

## Package-Struktur

```
app/src/main/java/.../
├── data/
│   ├── provider/
│   │   ├── AiProvider.kt          // Interface (siehe unten)
│   │   ├── OpenAiProvider.kt
│   │   ├── AnthropicProvider.kt
│   │   └── OllamaProvider.kt
│   ├── network/
│   │   └── <Retrofit-Interfaces, DTOs pro Provider>
│   └── repository/
│       └── ChatRepository.kt       // wählt Provider, delegiert
├── domain/
│   └── model/
│       ├── ChatMessage.kt
│       ├── ChatRequest.kt
│       └── ChatResponse.kt
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   └── settings/
│       ├── SettingsScreen.kt        // Provider wählen, API-Key eingeben
│       └── SettingsViewModel.kt
└── di/                              // falls Hilt genutzt wird
```

## Provider-Abstraktion (Kernstück)

Jeder Provider implementiert dasselbe Interface. Die UI/ViewModel-Schicht kennt
nie OpenAI/Anthropic/Ollama-spezifische Details – nur das Interface.

```kotlin
interface AiProvider {
    val id: ProviderType
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        apiKey: String
    ): Result<ChatResponse>
}

enum class ProviderType { OPENROUTER, Opencode zen, OLLAMA }
```

- `Result<ChatResponse>` statt Exceptions durchreichen – Fehler (falscher Key,
  Rate Limit, kein Netz, Ollama nicht erreichbar) werden im Repository/
  ViewModel behandelt, nie in der UI-Schicht direkt gefangen.
- Jeder Provider ist verantwortlich für sein eigenes Request/Response-Mapping
  auf die gemeinsamen `domain`-Modelle. Provider-spezifische DTOs bleiben in
  `data/network`, nie in `domain`.

## Datenfluss

```
ChatScreen → ChatViewModel → ChatRepository → AiProvider (gewählte Implementierung)
                                                    ↓
                                              Retrofit/Ktor Call
                                                    ↓
                                            Result<ChatResponse>
                                                    ↑
ChatScreen ← ChatViewModel ← ChatRepository ←──────┘
```

## Fehlerbehandlung – Grundsatz

- Netzwerkfehler, HTTP-Fehler, leere/kaputte Antworten → eigene Sealed-Class
  `ChatError` (z.B. `NetworkError`, `AuthError`, `ProviderUnavailable`,
  `UnknownError`), nie rohe Exceptions bis in die UI durchreichen.
- Ollama-Sonderfall: kein API-Key nötig, aber Erreichbarkeit (lokales Netz/
  IP) muss geprüft und dem Nutzer klar kommuniziert werden, wenn der Host
  nicht erreichbar ist.

## Non-Goals für v1 (bewusst NICHT bauen, bis v1 steht)

- Tool-Calling / Function-Calling
- Mehrere Agenten gleichzeitig / Orchestrierung
- On-Device-Inferenz (das macht bereits AI Workbench / Private Agent)
- RAG oder Konversationshistorie über die aktuelle Session hinaus
- Streaming-Antworten (kann später nachgerüstet werden, v1 = einfacher
  Request/Response-Zyklus)

## Versionierung dieses Dokuments

Wird dieses Dokument geändert (z.B. Provider-Interface erweitert), muss die
Änderung hier zuerst passieren – dann erst der Code. Nicht umgekehrt.
