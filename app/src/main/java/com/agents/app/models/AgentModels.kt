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
