package com.agents.app.ai

import com.agents.app.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AIProviderService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun sendMessage(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int = 4096,
        temperature: Float = 0.7f
    ): AgentResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        try {
            val response = when (provider) {
                AIProvider.OPENAI -> callOpenAI(apiKey, model, messages, maxTokens, temperature)
                AIProvider.ANTHROPIC -> callAnthropic(apiKey, model, messages, maxTokens, temperature)
                AIProvider.OLLAMA -> callOllama(baseUrl, model, messages, temperature)
                AIProvider.OPENCODE -> callOpenCode(baseUrl, model, messages, temperature)
            }

            val executionTime = System.currentTimeMillis() - startTime

            return@withContext AgentResult(
                success = true,
                output = response.first,
                tokensUsed = response.second,
                executionTimeMs = executionTime
            )
        } catch (e: Exception) {
            return@withContext AgentResult(
                success = false,
                output = "",
                error = e.message ?: "Unknown error",
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun callOpenAI(
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int,
        temperature: Float
    ): Pair<String, Int> {
        val requestBody = mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "max_tokens" to maxTokens,
            "temperature" to temperature
        )

        val json = gson.toJson(requestBody)
        val mediaType = "application/json".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("OpenAI API error: $responseBody")
        }

        val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
        val output = openAIResponse.choices?.firstOrNull()?.message?.content ?: ""
        val tokens = openAIResponse.usage?.total_tokens ?: 0

        return Pair(output, tokens)
    }

    private fun callAnthropic(
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int,
        temperature: Float
    ): Pair<String, Int> {
        val systemMessage = messages.find { it.role == "system" }?.content ?: ""
        val userMessages = messages.filter { it.role != "system" }

        val requestBody = mapOf(
            "model" to model,
            "max_tokens" to maxTokens,
            "temperature" to temperature,
            "system" to systemMessage,
            "messages" to userMessages.map { mapOf("role" to it.role, "content" to it.content) }
        )

        val json = gson.toJson(requestBody)
        val mediaType = "application/json".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("Anthropic API error: $responseBody")
        }

        val anthropicResponse = gson.fromJson(responseBody, AnthropicResponse::class.java)
        val output = anthropicResponse.content?.firstOrNull { it.type == "text" }?.text ?: ""
        val tokens = (anthropicResponse.usage?.input_tokens ?: 0) +
                (anthropicResponse.usage?.output_tokens ?: 0)

        return Pair(output, tokens)
    }

    private fun callOllama(
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        temperature: Float
    ): Pair<String, Int> {
        val requestBody = mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "options" to mapOf("temperature" to temperature)
        )

        val json = gson.toJson(requestBody)
        val mediaType = "application/json".toMediaType()
        val body = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("Ollama API error: $responseBody")
        }

        val ollamaResponse = gson.fromJson(responseBody, OllamaResponse::class.java)
        val output = ollamaResponse.response ?: ""

        return Pair(output, 0) // Ollama doesn't return token count
    }

    private fun callOpenCode(
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        temperature: Float
    ): Pair<String, Int> {
        val serverUrl = baseUrl.ifBlank { "http://localhost:4096" }

        // Build the prompt from messages
        val systemMessage = messages.find { it.role == "system" }?.content ?: ""
        val userMessages = messages.filter { it.role == "system" }
        val fullPrompt = buildString {
            if (systemMessage.isNotBlank()) {
                appendLine("System: $systemMessage")
                appendLine()
            }
            userMessages.forEach { msg ->
                appendLine("${msg.role}: ${msg.content}")
            }
        }.trim()

        // Create a session
        val createSessionBody = mapOf(
            "title" to "Android Agents Chat"
        )
        val createSessionJson = gson.toJson(createSessionBody)
        val mediaType = "application/json".toMediaType()
        val createSessionReqBody = createSessionJson.toRequestBody(mediaType)

        val createSessionRequest = Request.Builder()
            .url("$serverUrl/session/create")
            .addHeader("Content-Type", "application/json")
            .post(createSessionReqBody)
            .build()

        val createSessionResponse = client.newCall(createSessionRequest).execute()
        val createSessionResponseBody = createSessionResponse.body?.string()
            ?: throw Exception("Empty response from OpenCode session create")

        if (!createSessionResponse.isSuccessful) {
            throw Exception("OpenCode session create error: $createSessionResponseBody")
        }

        // Parse session ID from response
        val sessionObj = gson.fromJson(createSessionResponseBody, com.google.gson.JsonObject::class.java)
        val sessionId = sessionObj.get("id")?.asString
            ?: throw Exception("No session ID in OpenCode response")

        // Send prompt
        val promptBody = mapOf(
            "parts" to listOf(mapOf("type" to "text", "text" to fullPrompt))
        )
        val promptJson = gson.toJson(promptBody)
        val promptReqBody = promptJson.toRequestBody(mediaType)

        val promptRequest = Request.Builder()
            .url("$serverUrl/session/$sessionId/prompt")
            .addHeader("Content-Type", "application/json")
            .post(promptReqBody)
            .build()

        val promptResponse = client.newCall(promptRequest).execute()
        val promptResponseBody = promptResponse.body?.string()
            ?: throw Exception("Empty response from OpenCode prompt")

        if (!promptResponse.isSuccessful) {
            throw Exception("OpenCode prompt error: $promptResponseBody")
        }

        // Parse response
        val promptResult = gson.fromJson(promptResponseBody, com.google.gson.JsonObject::class.java)

        // Try to get the assistant message content
        val info = promptResult.getAsJsonObject("info")
        val output = info?.get("content")?.asString
            ?: promptResult.get("text")?.asString
            ?: ""

        return Pair(output, 0)
    }
}
