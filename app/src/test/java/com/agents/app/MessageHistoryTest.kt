package com.agents.app

import com.agents.app.models.Message
import com.agents.app.models.MessageRole
import org.junit.Test
import org.junit.Assert.*

class MessageHistoryTest {

    private fun message(index: Int, role: MessageRole = MessageRole.USER): Message =
        Message(
            id = "msg-$index",
            sessionId = "session-1",
            role = role,
            content = "Nachricht $index"
        )

    @Test
    fun buildApiMessages_includesSystemPromptFirst() {
        val messages = buildApiMessages(
            systemPrompt = "System",
            history = listOf(message(1), message(2))
        )

        assertEquals(3, messages.size)
        assertEquals("system", messages[0].role)
        assertEquals("System", messages[0].content)
    }

    @Test
    fun buildApiMessages_keepsAllWhenUnderLimit() {
        val history = (1..10).map { message(it) }
        val messages = buildApiMessages("System", history)

        assertEquals(11, messages.size)
        assertEquals("user", messages[1].role)
        assertEquals("Nachricht 1", messages[1].content)
        assertEquals("Nachricht 10", messages.last().content)
    }

    @Test
    fun buildApiMessages_truncatesToLastFifty() {
        val history = (1..80).map { message(it) }
        val messages = buildApiMessages("System", history)

        // System + letzte 50
        assertEquals(51, messages.size)
        assertEquals("Nachricht 31", messages[1].content)
        assertEquals("Nachricht 80", messages.last().content)
    }

    @Test
    fun buildApiMessages_preservesOrder() {
        val history = (1..60).map { message(it) }
        val messages = buildApiMessages("System", history)

        val contents = messages.drop(1).map { it.content }
        assertEquals(contents, contents.sortedBy { it.removePrefix("Nachricht ").toInt() })
    }

    @Test
    fun buildApiMessages_emptyHistory_onlySystem() {
        val messages = buildApiMessages("System", emptyList())

        assertEquals(1, messages.size)
        assertEquals("System", messages[0].content)
    }
}
