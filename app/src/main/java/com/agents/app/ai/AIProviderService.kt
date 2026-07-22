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
                AIProvider.OPENROUTER -> callOpenRouter(apiKey, model, messages, maxTokens, temperature)
                AIProvider.OLLAMA -> callOllama(apiKey, baseUrl, model, messages, temperature)
                AIProvider.ZEN -> callZen(apiKey, model, messages, maxTokens, temperature)
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

    private fun callOpenRouter(
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
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("HTTP-Referer", "https://github.com/bertiger-cell/android-agents-app")
            .addHeader("X-Title", "Android Agents App")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("OpenRouter error: $responseBody")
        }

        val openRouterResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
        val output = openRouterResponse.choices?.firstOrNull()?.message?.content ?: ""
        val tokens = openRouterResponse.usage?.total_tokens ?: 0

        return Pair(output, tokens)
    }

    private fun callOllama(
        apiKey: String,
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

        val requestBuilder = Request.Builder()
            .url("$baseUrl/api/chat")
            .addHeader("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val request = requestBuilder
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("Ollama API error: $responseBody")
        }

        val ollamaResponse = gson.fromJson(responseBody, OllamaResponse::class.java)
        val output = ollamaResponse.response ?: ""

        return Pair(output, 0)
    }

    private fun callZen(
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
            .url("https://opencode.ai/zen/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("OpenCode Zen error: $responseBody")
        }

        val zenResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
        val output = zenResponse.choices?.firstOrNull()?.message?.content ?: ""
        val tokens = zenResponse.usage?.total_tokens ?: 0

        return Pair(output, tokens)
    }
}
