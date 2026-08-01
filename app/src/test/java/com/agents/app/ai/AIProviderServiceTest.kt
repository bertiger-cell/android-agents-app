package com.agents.app.ai

import com.agents.app.models.ApiMessage
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.toList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

class AIProviderServiceTest {

    private val service = AIProviderService()

    @Test
    fun streamOpenAiCompatible_emitsTokensInOrder() = withHttpServer("/openai") { exchange ->
        val requestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        assertTrue(requestBody.contains("\"model\":\"gpt-4o\""))
        assertTrue(requestBody.contains("\"stream\":true"))
        assertTrue(requestBody.contains("\"content\":\"Hello\""))

        writeSseResponse(
            exchange,
            listOf(
                """data: {"choices":[{"delta":{"content":"Hel"}}]}""",
                """data: {"choices":[{"delta":{"content":"lo"}}]}""",
                "data: [DONE]"
            )
        )
    } { port ->
        val tokens = runBlocking {
            service.streamOpenAiCompatible(
                endpoint = "http://127.0.0.1:$port/openai",
                apiKey = "test-key",
                model = "gpt-4o",
                messages = listOf(ApiMessage(role = "user", content = "Hello")),
                maxTokens = 64,
                temperature = 0.2f
            ).toList()
        }

        assertEquals(listOf("Hel", "lo"), tokens)
    }

    @Test
    fun streamOpenAiCompatible_ignoresMalformedChunks() = withHttpServer("/openai-malformed") { exchange ->
        writeSseResponse(
            exchange,
            listOf(
                """data: {"choices":[{"delta":{"content":"Hel"}}]}""",
                "data: not-json",
                """data: {"choices":[{"delta":{"content":"lo"}}]}""",
                "data: [DONE]"
            )
        )
    } { port ->
        val tokens = runBlocking {
            service.streamOpenAiCompatible(
                endpoint = "http://127.0.0.1:$port/openai-malformed",
                apiKey = "test-key",
                model = "gpt-4o",
                messages = listOf(ApiMessage(role = "user", content = "Hello")),
                maxTokens = 64,
                temperature = 0.2f
            ).toList()
        }

        assertEquals(listOf("Hel", "lo"), tokens)
    }

    @Test
    fun streamOpenAiCompatible_throwsOnProviderErrorResponse() = withHttpServer("/openai-error") { exchange ->
        val body = """{"error":{"message":"Model not found"}}"""
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(400, body.toByteArray().size.toLong())
        exchange.responseBody.use { output ->
            output.write(body.toByteArray())
        }
    } { port ->
        try {
            runBlocking {
                service.streamOpenAiCompatible(
                    endpoint = "http://127.0.0.1:$port/openai-error",
                    apiKey = "test-key",
                    model = "gpt-4o",
                    messages = listOf(ApiMessage(role = "user", content = "Hello")),
                    maxTokens = 64,
                    temperature = 0.2f
                ).toList()
            }
            fail("Expected provider error to throw")
        } catch (e: Exception) {
            assertTrue(e.message.orEmpty().contains("Fehler vom Server (400)"))
            assertTrue(e.message.orEmpty().contains("Model not found"))
        }
    }

    @Test
    fun streamOpenAiCompatible_returnsEmptyListForDoneOnlyStream() = withHttpServer("/openai-empty") { exchange ->
        writeSseResponse(exchange, listOf("data: [DONE]"))
    } { port ->
        val tokens = runBlocking {
            service.streamOpenAiCompatible(
                endpoint = "http://127.0.0.1:$port/openai-empty",
                apiKey = "test-key",
                model = "gpt-4o",
                messages = listOf(ApiMessage(role = "user", content = "Hello")),
                maxTokens = 64,
                temperature = 0.2f
            ).toList()
        }

        assertTrue(tokens.isEmpty())
    }

    @Test
    fun streamOllama_emitsTokensInOrder() = withHttpServer("/api/chat") { exchange ->
        val requestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        assertTrue(requestBody.contains("\"model\":\"llama3\""))
        assertTrue(requestBody.contains("\"keep_alive\":\"30m\""))
        assertTrue(requestBody.contains("\"content\":\"Hello\""))
        assertTrue(exchange.requestHeaders.getFirst("Authorization")?.contains("test-key") == true)

        writeNdjsonResponse(
            exchange,
            listOf(
                """{"model":"llama3","message":{"role":"assistant","content":"Hel"},"done":false}""",
                """{"model":"llama3","message":{"role":"assistant","content":"lo"},"done":false}""",
                """{"model":"llama3","done":true}"""
            )
        )
    } { port ->
        val tokens = runBlocking {
            service.streamOllama(
                apiKey = "test-key",
                baseUrl = "http://127.0.0.1:$port",
                model = "llama3",
                messages = listOf(ApiMessage(role = "user", content = "Hello")),
                temperature = 0.1f,
                keepAlive = "30m"
            ).toList()
        }

        assertEquals(listOf("Hel", "lo"), tokens)
    }

    @Test
    fun streamOllama_ignoresMalformedLines() = withHttpServer("/api/chat-malformed") { exchange ->
        writeNdjsonResponse(
            exchange,
            listOf(
                """{"model":"llama3","message":{"role":"assistant","content":"Hel"},"done":false}""",
                "not-json",
                """{"model":"llama3","message":{"role":"assistant","content":"lo"},"done":false}""",
                """{"model":"llama3","done":true}"""
            )
        )
    } { port ->
        val tokens = runBlocking {
            service.streamOllama(
                apiKey = "",
                baseUrl = "http://127.0.0.1:$port/api",
                model = "llama3",
                messages = listOf(ApiMessage(role = "user", content = "Hello")),
                temperature = 0.1f,
                keepAlive = "30m"
            ).toList()
        }

        assertEquals(listOf("Hel", "lo"), tokens)
    }

    @Test
    fun streamOllama_throwsOnProviderErrorResponse() = withHttpServer("/api/chat-error") { exchange ->
        val body = """{"error":"model not found"}"""
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(500, body.toByteArray().size.toLong())
        exchange.responseBody.use { output ->
            output.write(body.toByteArray())
        }
    } { port ->
        try {
            runBlocking {
                service.streamOllama(
                    apiKey = "test-key",
                    baseUrl = "http://127.0.0.1:$port",
                    model = "llama3",
                    messages = listOf(ApiMessage(role = "user", content = "Hello")),
                    temperature = 0.1f,
                    keepAlive = "30m"
                ).toList()
            }
            fail("Expected Ollama error to throw")
        } catch (e: Exception) {
            assertTrue(e.message.orEmpty().contains("Ollama streaming error (500)"))
            assertTrue(e.message.orEmpty().contains("model not found"))
        }
    }

    private inline fun <T> withHttpServer(
        path: String,
        crossinline handler: (HttpExchange) -> Unit,
        block: (Int) -> T
    ): T {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext(path) { exchange ->
            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()

        return try {
            block(server.address.port)
        } finally {
            server.stop(0)
        }
    }

    private fun writeSseResponse(exchange: HttpExchange, lines: List<String>) {
        exchange.responseHeaders.add("Content-Type", "text/event-stream")
        exchange.sendResponseHeaders(200, 0)
        exchange.responseBody.use { output ->
            lines.forEach { line ->
                output.write((line + "\n\n").toByteArray())
                output.flush()
            }
        }
    }

    private fun writeNdjsonResponse(exchange: HttpExchange, lines: List<String>) {
        exchange.responseHeaders.add("Content-Type", "application/x-ndjson")
        exchange.sendResponseHeaders(200, 0)
        exchange.responseBody.use { output ->
            lines.forEach { line ->
                output.write((line + "\n").toByteArray())
                output.flush()
            }
        }
    }
}
