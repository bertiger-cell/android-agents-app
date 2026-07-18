package com.agents.app.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// AI Provider Types
enum class AIProvider {
    OPENAI,
    ANTHROPIC,
    OLLAMA
}

// Agent Status
enum class AgentStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    FAILED,
    STOPPED
}

// Agent Types for Specialized Agents
enum class AgentType {
    GENERAL,
    RESEARCHER,
    CODER,
    WRITER,
    AUTOMATOR,
    CUSTOM
}

// Base Agent Configuration
@Entity(tableName = "agents")
data class Agent(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val type: AgentType = AgentType.GENERAL,
    val provider: AIProvider = AIProvider.OPENAI,
    val systemPrompt: String = "You are a helpful AI assistant.",
    val model: String = "gpt-4",
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRunAt: Long? = null
)

// Chat Message
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0
)

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

// Agent Execution Result
data class AgentResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val tokensUsed: Int = 0,
    val executionTimeMs: Long = 0
)

// API Request/Response Models
data class ChatRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
)

data class ApiMessage(
    val role: String,
    val content: String
)

// OpenAI Response
data class OpenAIResponse(
    val id: String?,
    val choices: List<OpenAIChoice>?,
    val usage: OpenAIUsage?
)

data class OpenAIChoice(
    val message: OpenAIMessage?,
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

// Anthropic Response
data class AnthropicResponse(
    val id: String?,
    val content: List<AnthropicContent>?,
    val usage: AnthropicUsage?
)

data class AnthropicContent(
    val type: String?,
    val text: String?
)

data class AnthropicUsage(
    val input_tokens: Int?,
    val output_tokens: Int?
)

// Ollama Response
data class OllamaResponse(
    val model: String?,
    val response: String?,
    val done: Boolean?
)

// Automation Task
data class AutomationTask(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val name: String,
    val description: String,
    val schedule: String? = null, // cron expression
    val isEnabled: Boolean = true,
    val lastExecuted: Long? = null,
    val nextExecution: Long? = null
)
