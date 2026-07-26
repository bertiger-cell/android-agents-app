package com.agents.app.ai

import com.agents.app.models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okio.BufferedSource
import java.util.concurrent.TimeUnit

class AIProviderService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(6, TimeUnit.MINUTES)
        .build()

    private val gson = Gson()

    // --- Ollama Test & Models ---

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

    suspend fun fetchOllamaModels(
        baseUrl: String,
        apiKey: String
    ): List<OllamaModel> = withContext(Dispatchers.IO) {
        val requestUrl = buildOllamaUrl(baseUrl, "/api/tags")
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
                return@withContext emptyList()
            }

            val tagsResponse = gson.fromJson(responseBody, OllamaTagsResponse::class.java)
            return@withContext tagsResponse.models ?: emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun fetchOpenAICompatibleModels(
        endpoint: String,
        apiKey: String
    ): List<OpenAIModel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()

            if (!response.isSuccessful) {
                return@withContext emptyList()
            }

            val modelsResponse = gson.fromJson(responseBody, OpenAIModelsResponse::class.java)
            return@withContext modelsResponse.data ?: emptyList()
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    // --- Streaming Message API ---

    fun streamMessage(
        provider: AIProvider,
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int = 4096,
        temperature: Float = 0.7f,
        keepAlive: String = "30m"
    ): Flow<AgentResult> = flow {
        val startTime = System.currentTimeMillis()
        val fullResponse = StringBuilder()
        var totalTokens = 0

        try {
            val tokenFlow = when (provider) {
                AIProvider.OPENROUTER -> streamOpenAiCompatible(
                    endpoint = "https://openrouter.ai/api/v1/chat/completions",
                    apiKey = apiKey,
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature,
                    extraHeaders = mapOf(
                        "HTTP-Referer" to "https://github.com/bertiger-cell/android-agents-app",
                        "X-Title" to "Android Agents App"
                    )
                )
                AIProvider.ZEN -> streamOpenAiCompatible(
                    endpoint = "https://opencode.ai/zen/v1/chat/completions",
                    apiKey = apiKey,
                    model = model,
                    messages = messages,
                    maxTokens = maxTokens,
                    temperature = temperature
                )
                AIProvider.OLLAMA -> streamOllama(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    keepAlive = keepAlive
                )
            }

            tokenFlow.collect { token ->
                fullResponse.append(token)
                totalTokens++
                emit(AgentResult(
                    success = true,
                    output = fullResponse.toString(),
                    tokensUsed = totalTokens,
                    executionTimeMs = System.currentTimeMillis() - startTime
                ))
            }
        } catch (e: Exception) {
            emit(AgentResult(
                success = false,
                output = fullResponse.toString(),
                error = e.message ?: "Unknown error",
                tokensUsed = totalTokens,
                executionTimeMs = System.currentTimeMillis() - startTime
            ))
        }
    }.flowOn(Dispatchers.IO)

    // --- Streaming Providers ---

    fun streamOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        maxTokens: Int,
        temperature: Float,
        extraHeaders: Map<String, String> = emptyMap()
    ): Flow<String> = flow {
        val requestBody = mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "max_tokens" to maxTokens,
            "temperature" to temperature,
            "stream" to true
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

        val request = requestBuilder.post(body).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Empty error"
            throw Exception("Streaming error (${response.code}): ${errorBody.take(500)}")
        }

        val source: BufferedSource = response.body?.source()
            ?: throw Exception("Empty streaming response")

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (!line.startsWith("data: ")) continue
                val data = line.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                val chunk = runCatching {
                    gson.fromJson(data, OpenAIResponse::class.java)
                }.getOrNull() ?: continue

                val choice = chunk.choices?.firstOrNull()
                val token = choice?.delta?.content ?: choice?.message?.content
                if (!token.isNullOrBlank()) {
                    emit(token)
                }
            }
        } finally {
            response.body?.close()
        }
    }.flowOn(Dispatchers.IO)

    fun streamOllama(
        apiKey: String,
        baseUrl: String,
        model: String,
        messages: List<ApiMessage>,
        temperature: Float,
        keepAlive: String
    ): Flow<String> = flow {
        val requestUrl = buildOllamaUrl(baseUrl, "/api/chat")

        val requestBody = mapOf(
            "model" to model,
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
            "options" to mapOf("temperature" to temperature, "num_ctx" to 4096, "num_thread" to 8, "num_batch" to 512),
            "stream" to true,
            "keep_alive" to keepAlive
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

        val request = requestBuilder.post(body).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Empty error"
            throw Exception("Ollama streaming error (${response.code}) bei $requestUrl: ${errorBody.take(500)}")
        }

        val source: BufferedSource = response.body?.source()
            ?: throw Exception("Empty Ollama streaming response")

        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.isBlank()) continue

                val chunk = runCatching {
                    gson.fromJson(line, OllamaResponse::class.java)
                }.getOrNull() ?: continue

                val token = chunk.message?.content
                if (!token.isNullOrBlank()) {
                    emit(token)
                }

                if (chunk.done == true) break
            }
        } finally {
            response.body?.close()
        }
    }.flowOn(Dispatchers.IO)

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
