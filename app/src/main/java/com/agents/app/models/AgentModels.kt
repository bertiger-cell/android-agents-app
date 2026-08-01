package com.agents.app.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import androidx.compose.runtime.Stable

// AI Provider Types
enum class AIProvider {
    OPENROUTER,
    OLLAMA,
    ZEN
}

// Base Agent Configuration
@Stable
@Entity(
    tableName = "agents",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class Agent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String,
    val description: String = "",
    val provider: AIProvider = AIProvider.OPENROUTER,
    val model: String = "gpt-4",
    val systemPrompt: String = "You are a helpful AI assistant.",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val lastRunAt: Long? = null
)

// Chat Message
@Stable
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val isInternalThought: Boolean = false,
    val tokenCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

// Chat Message Attachment (Phase 2)
@Stable
@Entity(
    tableName = "message_attachments",
    foreignKeys = [
        ForeignKey(
            entity = Message::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageId"]),
        Index(value = ["sessionId"])
    ]
)
data class MessageAttachment(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val sessionId: String,
    val displayName: String,
    val mimeType: String = "application/octet-stream",
    val localPath: String,
    val sizeBytes: Long = 0
)

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

// V3 Room Entities

@Stable
@Entity(
    tableName = "projects",
    indices = [Index(value = ["folderPath"], unique = true)]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderPath: String,
    val color: String = "#6200EE",
    val tags: String = "[]"  // JSON array als String
)

@Stable
@Entity(
    tableName = "chat_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Agent::class,
            parentColumns = ["id"],
            childColumns = ["agentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["agentId"])
    ]
)
data class ChatSessionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val agentId: String,
    val title: String = "Chat ${System.currentTimeMillis()}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

// Agent Execution Result
data class AgentResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val tokensUsed: Int = 0,
    val executionTimeMs: Long = 0
)

data class OllamaConnectionResult(
    val success: Boolean,
    val message: String,
    val version: String? = null
)

// API Request/Response Models
data class ApiMessage(
    val role: String,
    val content: String
)

// OpenAI-compatible Chat Response (used by OpenRouter and Zen)
data class OpenAIResponse(
    val id: String?,
    val choices: List<OpenAIChoice>?,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val message: OpenAIMessage?,
    val delta: OpenAIMessage?,
    val finish_reason: String?
)

data class OpenAIMessage(
    val role: String?,
    val content: String?
)

data class OpenAIUsage(
    val prompt_tokens: Int?,
    val completion_tokens: Int?,
    val total_tokens: Int?
)

// OpenAI-compatible Models List Response (used by OpenRouter and Zen)
data class OpenAIModelsResponse(
    val data: List<OpenAIModel>?
)

data class OpenAIModel(
    val id: String?,
    val name: String?,
    val description: String?
)

// Ollama Response
data class OllamaResponse(
    val model: String?,
    val message: OllamaMessage?,
    val done: Boolean?
)

data class OllamaVersionResponse(
    val version: String?
)

data class OllamaTagsResponse(
    val models: List<OllamaModel>?
)

data class OllamaModel(
    val name: String?,
    val model: String?,
    val size: Long?,
    val details: OllamaModelDetails?
)

data class OllamaModelDetails(
    val parameter_size: String?,
    val quantization_level: String?,
    val family: String?,
    val context_length: Int?
)

data class OllamaMessage(
    val role: String?,
    val content: String?
)

// Domain models (non-Room)
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val folderPath: String,
    val color: String = "#6200EE",
    val tags: List<String> = emptyList()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val agentId: String,
    val title: String = "Chat ${System.currentTimeMillis()}",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

// ===== Architect Agent Models (Task 8a) =====

data class ProjectDiscovery(
    val projectId: String,
    val domain: String = "",
    val technologies: List<String> = emptyList(),
    val experienceLevel: String = "",
    val projectSize: String = "",
    val concerns: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val keyInsights: String = ""
)

data class ProjectScaffold(
    val discoveryContext: ProjectDiscovery,
    val architecture: String,
    val phases: List<ScaffoldPhase>,
    val rules: List<String>,
    val suggestedAgents: List<AgentSpec>,
    val diaryEntry: String
)

data class ScaffoldPhase(
    val phaseNumber: Int,
    val name: String,
    val description: String,
    val duration: String,
    val focus: String
)

data class AgentSpec(
    val name: String,
    val description: String,
    val systemPrompt: String,
    val provider: String = "openrouter",
    val model: String = "gpt-4o",
    val temperature: Float = 0.7f
)

// ===== DEFAULT System Prompt for Architect Agent =====

const val DEFAULT_ARCHITECT_PROMPT = """
Du bist ein Projekt-Architekt und Discovery-Expert.

DEINE AUFGABE:
Interviewe den User ueber sein Projekt und generiere am Ende
ein komplettes Projekt-Scaffold: ARCHITECTURE.md, ROADMAP.md, RULES.md, etc.

PHASEN:

=== PHASE 1: WARM-UP (erste 5-10 Nachrichten) ===
- Stelle OFFENE Fragen, um die grosse Idee zu verstehen
- NICHT: "Welche Tech-Stack?" (zu eng)
- JA: "Erzaehl mir dein Traum-Projekt. Was ist die grosse Idee?"
- Stelle Follow-Up-Fragen basierend auf Antworten
- Biete LINKS an, wenn User erwaehnt:
  * Technologien -> Link zu Doku
  * Patterns -> Link zu Best-Practices
  * Concerns -> Link zu Learning Resources

WAEHREND Phase 1, MERKE DIR INTERN (speichern fuer spaeter):
- Domain (Web, Mobile, IoT, AI, Finance, etc.)
- Technologien erwahnt (Kotlin, Python, React, etc.)
- Experience Level (Anfaenger, Fortgeschritten, Expert)
- Groesse/Scope (klein Hobby, mittel Startup, gross Enterprise)
- Concerns/Pains (was macht User nervoes?)

=== PHASE 2: STRUKTURIERTES INTERVIEW (naechste 10-15 Nachrichten) ===
- Nutze das Wissen aus Phase 1
- Stelle GEZIELTE Fragen (nicht Standard-Fragen)
- Beispiel: Wenn "IoT + Kotlin" -> frag nicht "kennst du Microservices?"
           sondern: "Wie viele Geraete parallel? Das bestimmt Architektur"

Typische Fragen (je nach Domain tailored):
- Skalierung: "Wie viele Users/Devices/Requests?"
- Latenz: "Echtzeitanforderungen?"
- Persistenz: "Welche Datenbank passt?"
- Auth: "Braucht es Sicherheit?"
- Integration: "Externe APIs?"

=== PHASE 3: GENERIERUNG ===
Wenn User sagt "Okay, mach mir einen Plan!" oder nach ~15 Nachrichten:

Generiere DIESES JSON (am Ende deiner Antwort):

```json
{
  "discovery_context": {
    "domain": "IoT Smart-Home",
    "technologies": ["Kotlin", "Spring Boot", "MQTT"],
    "experience_level": "mid_kotlin_junior_iot",
    "project_size": "medium_startup",
    "concerns": ["Skalierung", "Neue Domain"]
  },
  "architecture": "Kotlin Spring Boot Backend mit MQTT IoT-Gateway. Event-driven mit Redis-Queue. TimeSeries-DB fuer Sensor-Daten.",
  "phases": [
    {
      "phase_number": 1,
      "name": "Learning & Setup",
      "description": "Weil User neu bei IoT: Fundament aufbauen",
      "duration": "1 week",
      "focus": "MQTT basics, DB-Setup, API-Foundation"
    },
    {
      "phase_number": 2,
      "name": "Core API",
      "description": "Kotlin/Spring Staerken nutzen",
      "duration": "2 weeks",
      "focus": "REST API, WebSocket, Real-time Updates"
    },
    {
      "phase_number": 3,
      "name": "Testing & Optimization",
      "description": "Produktion ready",
      "duration": "1 week",
      "focus": "Integration Tests, Performance, Monitoring"
    }
  ],
  "rules": [
    "Nutze Spring Boot best practices (dependency injection, etc.)",
    "Kotlin idioms (data classes, extension functions)",
    "Teste jeden Endpoint vor Production",
    "MQTT quality-of-service = 1 (mindestens einmal)"
  ],
  "suggested_agents": [
    {
      "name": "MQTT-Expert",
      "description": "Spezialist fuer IoT-Kommunikation",
      "system_prompt": "Du bist MQTT-Spezialist. User ist neu in IoT. Erklaere EINFACH mit Analogien.",
      "provider": "openrouter",
      "model": "gpt-4o",
      "temperature": 0.7
    },
    {
      "name": "Spring-Boot-Architect",
      "description": "Backend-Spezialist (nutzt Strengths vom User)",
      "system_prompt": "Du bist Spring Boot Experte. Nutze Kotlin-Idioms. User kann gut Kotlin.",
      "provider": "openrouter",
      "model": "gpt-4o",
      "temperature": 0.7
    }
  ],
  "diary_entry": "Projekt initialisiert. Domain: IoT Smart-Home. Tech: Kotlin+Spring+MQTT. 3 Phasen."
}
"""
