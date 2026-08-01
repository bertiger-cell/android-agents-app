package com.agents.app.ai

import com.agents.app.models.ApiMessage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AIProviderServiceTest {

    private val service = AIProviderService()

    @Test
    fun streamOpenAiCompatible_emitsTokensInOrder() = withHttpServer("/openai", { exchange ->
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
    }) { port ->
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
    fun streamOpenAiCompatible_ignoresMalformedChunks() = withHttpServer("/openai-malformed", { exchange ->
        writeSseResponse(
            exchange,
            listOf(
                """data: {"choices":[{"delta":{"content":"Hel"}}]}""",
                "data: not-json",
                """data: {"choices":[{"delta":{"content":"lo"}}]}""",
                "data: [DONE]"
            )
        )
    }) { port ->
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
    fun streamOpenAiCompatible_throwsOnProviderErrorResponse() = withHttpServer("/openai-error", { exchange ->
        val body = """{"error":{"message":"Model not found"}}"""
        exchange.responseHeaders["Content-Type"] = "application/json"
        exchange.sendResponseHeaders(400, body.toByteArray().size.toLong())
        exchange.responseBody.use { output ->
            output.write(body.toByteArray())
        }
    }) { port ->
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
    fun streamOpenAiCompatible_returnsEmptyListForDoneOnlyStream() = withHttpServer("/openai-empty", { exchange ->
        writeSseResponse(exchange, listOf("data: [DONE]"))
    }) { port ->
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
    fun streamOllama_emitsTokensInOrder() = withHttpServer("/api/chat", { exchange ->
        val requestBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
        assertTrue(requestBody.contains("\"model\":\"llama3\""))
        assertTrue(requestBody.contains("\"keep_alive\":\"30m\""))
        assertTrue(requestBody.contains("\"content\":\"Hello\""))
        assertTrue(exchange.header("Authorization")?.contains("test-key") == true)

        writeNdjsonResponse(
            exchange,
            listOf(
                """{"model":"llama3","message":{"role":"assistant","content":"Hel"},"done":false}""",
                """{"model":"llama3","message":{"role":"assistant","content":"lo"},"done":false}""",
                """{"model":"llama3","done":true}"""
            )
        )
    }) { port ->
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
    fun streamOllama_ignoresMalformedLines() = withHttpServer("/api/chat", { exchange ->
        writeNdjsonResponse(
            exchange,
            listOf(
                """{"model":"llama3","message":{"role":"assistant","content":"Hel"},"done":false}""",
                "not-json",
                """{"model":"llama3","message":{"role":"assistant","content":"lo"},"done":false}""",
                """{"model":"llama3","done":true}"""
            )
        )
    }) { port ->
        val tokens = runBlocking {
            service.streamOllama(
                apiKey = "",
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
    fun streamOllama_throwsOnProviderErrorResponse() = withHttpServer("/api/chat", { exchange ->
        val body = """{"error":"model not found"}"""
        exchange.responseHeaders["Content-Type"] = "application/json"
        exchange.sendResponseHeaders(500, body.toByteArray().size.toLong())
        exchange.responseBody.use { output ->
            output.write(body.toByteArray())
        }
    }) { port ->
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

    private fun <T> withHttpServer(
        path: String,
        handler: (TestHttpExchange) -> Unit,
        block: (Int) -> T
    ): T {
        val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
        val executor = Executors.newCachedThreadPool()
        val running = AtomicBoolean(true)

        executor.execute {
            while (running.get()) {
                try {
                    val socket = server.accept()
                    executor.execute {
                        try {
                            handleConnection(socket, path, handler)
                        } catch (_: Exception) {
                            // Test server must not tear down the whole suite.
                        }
                    }
                } catch (e: Exception) {
                    if (running.get()) throw e
                }
            }
        }

        return try {
            block(server.localPort)
        } finally {
            running.set(false)
            server.close()
            executor.shutdownNow()
        }
    }

    private fun handleConnection(
        socket: Socket,
        path: String,
        handler: (TestHttpExchange) -> Unit
    ) {
        socket.use { connection ->
            val input = connection.getInputStream()
            val requestLine = readLine(input) ?: return
            val requestTarget = requestLine.split(" ").getOrNull(1) ?: return
            if (requestTarget != path) {
                writeResponse(connection, 404, emptyMap(), "Not found".toByteArray())
                return
            }

            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = readLine(input) ?: break
                if (line.isBlank()) break
                val separator = line.indexOf(':')
                if (separator > 0) {
                    headers[line.substring(0, separator).trim().lowercase()] =
                        line.substring(separator + 1).trim()
                }
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val body = if (contentLength > 0) readBytes(input, contentLength) else ByteArray(0)
            val exchange = TestHttpExchange(
                socket = connection,
                requestBody = ByteArrayInputStream(body),
                requestHeaders = headers
            )

            try {
                handler(exchange)
            } finally {
                exchange.close()
            }
        }
    }

    private fun readLine(input: InputStream): String? {
        val line = ByteArrayOutputStream()
        var sawByte = false
        while (true) {
            val byte = input.read()
            if (byte == -1) return if (sawByte) line.toString(Charsets.UTF_8) else null
            sawByte = true
            if (byte == '\n'.code) break
            if (byte != '\r'.code) line.write(byte)
        }
        return line.toString(Charsets.UTF_8)
    }

    private fun readBytes(input: InputStream, length: Int): ByteArray {
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read < 0) break
            offset += read
        }
        return buffer.copyOf(offset)
    }

    private fun writeResponse(
        socket: Socket,
        status: Int,
        headers: Map<String, String>,
        body: ByteArray
    ) {
        val reason = when (status) {
            400 -> "Bad Request"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            else -> "OK"
        }
        val head = StringBuilder()
            .append("HTTP/1.1 $status $reason\r\n")
        headers.forEach { (name, value) ->
            head.append("$name: $value\r\n")
        }
        head.append("Content-Length: ${body.size}\r\n")
        head.append("Connection: close\r\n\r\n")

        socket.getOutputStream().use { output ->
            output.write(head.toString().toByteArray(Charsets.UTF_8))
            output.write(body)
            output.flush()
        }
    }

    private fun writeSseResponse(exchange: TestHttpExchange, lines: List<String>) {
        exchange.responseHeaders["Content-Type"] = "text/event-stream"
        exchange.sendResponseHeaders(200, 0)
        exchange.responseBody.use { output ->
            lines.forEach { line ->
                output.write((line + "\n\n").toByteArray())
                output.flush()
            }
        }
    }

    private fun writeNdjsonResponse(exchange: TestHttpExchange, lines: List<String>) {
        exchange.responseHeaders["Content-Type"] = "application/x-ndjson"
        exchange.sendResponseHeaders(200, 0)
        exchange.responseBody.use { output ->
            lines.forEach { line ->
                output.write((line + "\n").toByteArray())
                output.flush()
            }
        }
    }
}

private class TestHttpExchange(
    private val socket: Socket,
    val requestBody: InputStream,
    private val requestHeaders: Map<String, String>
) {
    val responseHeaders = mutableMapOf<String, String>()
    val responseBody: OutputStream = ByteArrayOutputStream()
    private var status = 200

    fun header(name: String): String? = requestHeaders[name.lowercase()]

    fun sendResponseHeaders(code: Int, contentLength: Long) {
        status = code
    }

    fun close() {
        val body = (responseBody as ByteArrayOutputStream).toByteArray()
        writeResponseForTest(socket, status, responseHeaders, body)
    }
}

private fun writeResponseForTest(
    socket: Socket,
    status: Int,
    headers: Map<String, String>,
    body: ByteArray
) {
    val reason = when (status) {
        400 -> "Bad Request"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        else -> "OK"
    }
    val head = StringBuilder()
        .append("HTTP/1.1 $status $reason\r\n")
    headers.forEach { (name, value) ->
        head.append("$name: $value\r\n")
    }
    head.append("Content-Length: ${body.size}\r\n")
    head.append("Connection: close\r\n\r\n")

    socket.getOutputStream().use { output ->
        output.write(head.toString().toByteArray(Charsets.UTF_8))
        output.write(body)
        output.flush()
    }
}
