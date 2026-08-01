package com.agents.app

import com.agents.app.data.TRANSFER_FORMAT_VERSION
import com.agents.app.data.buildTransferManifest
import com.agents.app.data.parseTransferManifest
import com.agents.app.data.readZipEntries
import com.agents.app.data.writeZipEntries
import com.agents.app.models.AIProvider
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.Message
import com.agents.app.models.MessageAttachment
import com.agents.app.models.MessageRole
import com.agents.app.models.ProjectEntity
import com.google.gson.Gson
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ProjectTransferTest {

    private val project = ProjectEntity(
        id = "p1",
        name = "Testprojekt",
        description = "Beschreibung",
        createdAt = 1000L,
        updatedAt = 2000L,
        folderPath = "/tmp/p1",
        color = "#123456",
        tags = "[\"a\",\"b\"]"
    )

    private val agent = Agent(
        id = "a1",
        projectId = "p1",
        name = "Coder",
        description = "desc",
        provider = AIProvider.OPENROUTER,
        model = "gpt-4o",
        systemPrompt = "Du bist ein Coder.",
        temperature = 0.3f,
        maxTokens = 2048,
        lastRunAt = 1500L
    )

    private val session = ChatSessionEntity(
        id = "s1",
        projectId = "p1",
        agentId = "a1",
        title = "Chat 1",
        createdAt = 1000L,
        updatedAt = 1500L,
        isArchived = false
    )

    private val message = Message(
        id = "m1",
        sessionId = "s1",
        role = MessageRole.USER,
        content = "Hallo",
        tokenCount = 3,
        timestamp = 1200L
    )

    @Test
    fun manifest_roundtrip_preservesData() {
        val manifest = buildTransferManifest(project, listOf(agent), listOf(session), listOf(message))
        val json = Gson().toJson(manifest)
        val parsed = parseTransferManifest(json)

        assertEquals(TRANSFER_FORMAT_VERSION, parsed.formatVersion)
        assertEquals("Testprojekt", parsed.project.name)
        assertEquals("#123456", parsed.project.color)
        assertEquals(1, parsed.agents.size)
        assertEquals("Coder", parsed.agents[0].name)
        assertEquals(AIProvider.OPENROUTER, parsed.agents[0].provider)
        assertEquals(0.3f, parsed.agents[0].temperature)
        assertEquals(1, parsed.sessions.size)
        assertEquals(0, parsed.sessions[0].agentIndex)
        assertEquals("Chat 1", parsed.sessions[0].title)
        assertEquals(1, parsed.messages.size)
        assertEquals(0, parsed.messages[0].sessionIndex)
        assertEquals(MessageRole.USER, parsed.messages[0].role)
        assertEquals("Hallo", parsed.messages[0].content)
    }

    @Test
    fun manifest_roundtrip_includesAttachments() {
        val attachment = MessageAttachment(
            id = "att1",
            messageId = "m1",
            sessionId = "s1",
            displayName = "bild.png",
            mimeType = "image/png",
            localPath = "/tmp/p1/media/bild.png",
            sizeBytes = 2048
        )
        val manifest = buildTransferManifest(
            project,
            listOf(agent),
            listOf(session),
            listOf(message),
            listOf(attachment)
        )
        val parsed = parseTransferManifest(Gson().toJson(manifest))

        assertEquals(1, parsed.attachments.size)
        assertEquals("bild.png", parsed.attachments[0].displayName)
        assertEquals("image/png", parsed.attachments[0].mimeType)
        assertEquals(0, parsed.attachments[0].sessionIndex)
        assertEquals(0, parsed.attachments[0].messageIndex)
        assertEquals("media/bild.png", parsed.attachments[0].relativePath)
        assertEquals(2048L, parsed.attachments[0].sizeBytes)
    }

    @Test
    fun zip_roundtrip_preservesEntries() {
        val entries = listOf(
            "manifest.json" to "{\"formatVersion\":1}".toByteArray(),
            "diary.md" to "# Diary".toByteArray(),
            "media/bild.png" to byteArrayOf(1, 2, 3)
        )
        val bytes = ByteArrayOutputStream().also { writeZipEntries(entries, it) }.toByteArray()
        val read = readZipEntries(ByteArrayInputStream(bytes))

        assertEquals(entries.size, read.size)
        assertEquals("manifest.json", read[0].first)
        assertArrayEquals(entries[0].second, read[0].second)
        assertEquals("media/bild.png", read[2].first)
        assertArrayEquals(byteArrayOf(1, 2, 3), read[2].second)
    }

    @Test
    fun parseTransferManifest_rejectsWrongVersion() {
        val json = """{"formatVersion":99,"project":{"name":"X","createdAt":1,"updatedAt":1}}"""
        try {
            parseTransferManifest(json)
            fail("IllegalArgumentException erwartet")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Format-Version"))
        }
    }

    @Test
    fun parseTransferManifest_rejectsBlankName() {
        val json = """{"formatVersion":1,"project":{"name":"  ","createdAt":1,"updatedAt":1}}"""
        try {
            parseTransferManifest(json)
            fail("IllegalArgumentException erwartet")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("Projektnamen"))
        }
    }
}
