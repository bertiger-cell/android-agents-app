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
                AIProvider.OPENROUTER -> callOpenAiCompatible(
                    endpoint = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = apiKey,
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature,
                    providerName = "OpenRouter",
                    extraHeaders = mapOf(
                        "HTTP-Referer" to "https://github.com/bertiger-cell/android-agents-app",
                        "X-Title" to "Android Agents App"
                    )
                )
                AIProvider.OLLAMA -> callOllama(apiKey, baseUrl, model, messages, temperature)
                AIProvider.ZEN -> callOpenAiCompatible(
                    endpoint = "https://opencode.ai/zen/v1/chat/completions",
                    apiKey = apiKey,
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature,
                    providerName = "OpenCode Zen"
                )
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

    suspend fun testOllamaConnection(
        baseUrl: String,
        apiKey: String
    ): OllamaConnectionResult = withContext(Dispatchers.IO) {
        val requestUrl = buildOllamaUrl(baseUrl, "/api/version")
        try {
            val requestBuilder = Request.Builder()
                .url(requestUrl)
                .get()
                .addHeader("Content-Type", "application/json")

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext OllamaConnectionResult(
                    success = false,
                    message = "Ollama nicht erreichbar (${response.code}) bei $requestUrl: ${responseBody.take(500)}",
                    version = null
                )
            }

            val version = runCatching {
                gson.fromJson(responseBody, OllamaVersionResponse::class.java).version
            }.getOrNull()

            return@withContext OllamaConnectionResult(
                success = true,
                message = if (version.isNullOrBlank()) {
                    "Ollama ist erreichbar."
                } else {
                    "Ollama ist erreichbar. Version: $version"
                },
                version = version
            )
        } catch (e: Exception) {
            return@withContext OllamaConnectionResult(
                success = false,
                message = "Ollama-Verbindung fehlgeschlagen bei $requestUrl: ${e.message ?: "Unbekannter Fehler"}",
                version = null
            )
        }
    }

    private fun callOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int,
        temperature: Float,
        providerName: String,
        extraHeaders: Map<String, String> = emptyMap()
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

        val requestBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        for ((key, value) in extraHeaders) {
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response")

        if (!response.isSuccessful) {
            throw Exception("$providerName error: $responseBody")
        }

        val openAiResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
        val output = openAiResponse.choices?.firstOrNull()?.message?.content ?: ""
        val tokens = openAiResponse.usage?.total_tokens ?: 0

        return Pair(output, tokens)
    }

    private fun callOllama(
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        temperature: Float
    ): Pair<String, Int> {
        val requestUrl = buildOllamaUrl(baseUrl, "/api/chat")
        try {
            val requestBody = mapOf(
                "model" to model,
                "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
                "options" to mapOf("temperature" to temperature),
                "stream" to false
            )

            val json = gson.toJson(requestBody)
            val mediaType = "application/json".toMediaType()
            val body = json.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(requestUrl)
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
                throw Exception("Ollama API error (${response.code}) bei $requestUrl: ${responseBody.take(500)}")
            }

            val ollamaResponse = gson.fromJson(responseBody, OllamaResponse::class.java)
            val output = ollamaResponse.message?.content ?: ""

            if (output.isBlank()) {
                throw Exception("Ollama lieferte keine Antwort bei $requestUrl")
            }

            return Pair(output, 0)
        } catch (e: Exception) {
            throw Exception("Ollama-Aufruf fehlgeschlagen bei $requestUrl: ${e.message ?: "Unbekannter Fehler"}")
        }
    }

    private fun buildOllamaUrl(baseUrl: String, path: String): String {
        val normalizedBaseUrl = baseUrl.trim().trimEnd('/').removeSuffix("/api")
        if (normalizedBaseUrl.isBlank()) {
            throw IllegalArgumentException("Ollama base URL is empty")
        }

        val safeBaseUrl = if (normalizedBaseUrl.startsWith("http://") || normalizedBaseUrl.startsWith("https://")) {
            normalizedBaseUrl
        } else {
            "http://$normalizedBaseUrl"
        }

        return "$safeBaseUrl$path"
    }
}
