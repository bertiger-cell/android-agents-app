package com.agents.app.data

import android.content.Context
import com.agents.app.db.AgentDatabase
import com.agents.app.models.Agent
import com.agents.app.models.AIProvider
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.Message
import com.agents.app.models.MessageAttachment
import com.agents.app.models.MessageRole
import com.agents.app.models.ProjectEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val TRANSFER_FORMAT_VERSION = 1
private const val MANIFEST_NAME = "manifest.json"

// ---- Transfer-Datenmodelle (Gson-serialisierbar) ----

data class TransferManifest(
    val formatVersion: Int = TRANSFER_FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val project: TransferProject,
    val agents: List<TransferAgent> = emptyList(),
    val sessions: List<TransferSession> = emptyList(),
    val messages: List<TransferMessage> = emptyList(),
    val attachments: List<TransferAttachment> = emptyList()
)

data class TransferProject(
    val name: String,
    val description: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val color: String = "#6200EE",
    val tags: String = "[]"
)

data class TransferAgent(
    val name: String,
    val description: String = "",
    val provider: AIProvider,
    val model: String,
    val systemPrompt: String,
    val temperature: Float,
    val maxTokens: Int,
    val lastRunAt: Long? = null
)

data class TransferSession(
    val agentIndex: Int,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean = false
)

data class TransferMessage(
    val sessionIndex: Int,
    val role: MessageRole,
    val content: String,
    val isInternalThought: Boolean = false,
    val tokenCount: Int = 0,
    val timestamp: Long
)

data class TransferAttachment(
    val sessionIndex: Int,
    val messageIndex: Int,
    val displayName: String,
    val mimeType: String = "application/octet-stream",
    val relativePath: String,
    val sizeBytes: Long = 0
)

// ---- Pure (JVM-testbare) Helfer ----

fun buildTransferManifest(
    project: ProjectEntity,
    agents: List<Agent>,
    sessions: List<ChatSessionEntity>,
    messages: List<Message>,
    attachments: List<MessageAttachment> = emptyList()
): TransferManifest {
    val agentIndexById = agents.mapIndexed { index, agent -> agent.id to index }.toMap()
    val sessionIndexById = sessions.mapIndexed { index, session -> session.id to index }.toMap()
    val messageIndexById = messages.mapIndexed { index, message -> message.id to index }.toMap()
    return TransferManifest(
        project = TransferProject(
            name = project.name,
            description = project.description,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            color = project.color,
            tags = project.tags
        ),
        agents = agents.map { agent ->
            TransferAgent(
                name = agent.name,
                description = agent.description,
                provider = agent.provider,
                model = agent.model,
                systemPrompt = agent.systemPrompt,
                temperature = agent.temperature,
                maxTokens = agent.maxTokens,
                lastRunAt = agent.lastRunAt
            )
        },
        sessions = sessions.map { session ->
            TransferSession(
                agentIndex = agentIndexById[session.agentId] ?: -1,
                title = session.title,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                isArchived = session.isArchived
            )
        },
        messages = messages.map { message ->
            TransferMessage(
                sessionIndex = sessionIndexById[message.sessionId] ?: -1,
                role = message.role,
                content = message.content,
                isInternalThought = message.isInternalThought,
                tokenCount = message.tokenCount,
                timestamp = message.timestamp
            )
        },
        attachments = attachments.map { attachment ->
            TransferAttachment(
                sessionIndex = sessionIndexById[attachment.sessionId] ?: -1,
                messageIndex = messageIndexById[attachment.messageId] ?: -1,
                displayName = attachment.displayName,
                mimeType = attachment.mimeType,
                relativePath = attachment.localPath
                    .removePrefix(project.folderPath + File.separator)
                    .replace(File.separatorChar, '/'),
                sizeBytes = attachment.sizeBytes
            )
        }
    )
}

fun parseTransferManifest(json: String): TransferManifest {
    val manifest = transferGson.fromJson(json, TransferManifest::class.java)
        ?: throw IllegalArgumentException("manifest.json konnte nicht geparst werden")
    if (manifest.formatVersion != TRANSFER_FORMAT_VERSION) {
        throw IllegalArgumentException("Nicht unterstuetzte Export-Format-Version: ${manifest.formatVersion}")
    }
    if (manifest.project.name.isBlank()) {
        throw IllegalArgumentException("manifest.json enthaelt keinen Projektnamen")
    }
    return manifest
}

fun writeZipEntries(entries: List<Pair<String, ByteArray>>, output: OutputStream) {
    ZipOutputStream(output).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
}

fun readZipEntries(input: InputStream): List<Pair<String, ByteArray>> {
    val entries = mutableListOf<Pair<String, ByteArray>>()
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory) {
                entries.add(entry.name to zip.readBytes())
            }
            zip.closeEntry()
        }
    }
    return entries
}

private val transferGson: Gson = GsonBuilder().setPrettyPrinting().create()

class ProjectTransferRepository(
    private val database: AgentDatabase,
    private val context: Context
) {
    private val agentDao = database.agentDao()
    private val messageDao = database.messageDao()
    private val projectDao = database.projectDao()
    private val chatSessionDao = database.chatSessionDao()
    private val messageAttachmentDao = database.messageAttachmentDao()

    suspend fun exportProject(project: ProjectEntity, output: OutputStream) {
        val agents = agentDao.getAgentsByProject(project.id).first()
        val sessions = chatSessionDao.getSessionsByProject(project.id).first()
        val messages = sessions.flatMap { session ->
            messageDao.getMessagesBySession(session.id).first()
        }
        val attachments = sessions.flatMap { session ->
            messageAttachmentDao.getAttachmentsBySession(session.id)
        }
        val manifest = buildTransferManifest(project, agents, sessions, messages, attachments)

        val entries = mutableListOf<Pair<String, ByteArray>>()
        entries.add(MANIFEST_NAME to transferGson.toJson(manifest).toByteArray(Charsets.UTF_8))
        collectProjectFiles(File(project.folderPath)).forEach { (relativePath, bytes) ->
            entries.add(relativePath to bytes)
        }

        withContext(Dispatchers.IO) {
            writeZipEntries(entries, output)
        }
    }

    suspend fun importProject(input: InputStream): ProjectEntity {
        val entries = withContext(Dispatchers.IO) { readZipEntries(input) }
        val manifestEntry = entries.find { it.first == MANIFEST_NAME }
            ?: throw IllegalArgumentException("Ungueltiges ZIP: manifest.json fehlt")
        val manifest = parseTransferManifest(String(manifestEntry.second, Charsets.UTF_8))

        val newProjectId = UUID.randomUUID().toString()
        val newFolder = File(
            context.filesDir,
            "projects/${sanitizeFolderName(manifest.project.name)}_$newProjectId"
        )
        newFolder.mkdirs()
        File(newFolder, "media").mkdirs()
        File(newFolder, "audio").mkdirs()
        File(newFolder, "exports").mkdirs()

        val projectEntity = ProjectEntity(
            id = newProjectId,
            name = manifest.project.name,
            description = manifest.project.description,
            createdAt = manifest.project.createdAt,
            updatedAt = System.currentTimeMillis(),
            folderPath = newFolder.absolutePath,
            color = manifest.project.color,
            tags = manifest.project.tags
        )
        projectDao.insertProject(projectEntity)

        val newAgentIds = manifest.agents.mapIndexed { index, agent ->
            val newId = UUID.randomUUID().toString()
            agentDao.insertAgent(
                Agent(
                    id = newId,
                    projectId = newProjectId,
                    name = agent.name,
                    description = agent.description,
                    provider = agent.provider,
                    model = agent.model,
                    systemPrompt = agent.systemPrompt,
                    temperature = agent.temperature,
                    maxTokens = agent.maxTokens,
                    lastRunAt = agent.lastRunAt
                )
            )
            index to newId
        }.toMap()

        val newSessionIds = manifest.sessions.mapIndexed { index, session ->
            val newId = UUID.randomUUID().toString()
            val agentId = newAgentIds[session.agentIndex]
                ?: newAgentIds.values.firstOrNull()
                ?: ""
            chatSessionDao.insertSession(
                ChatSessionEntity(
                    id = newId,
                    projectId = newProjectId,
                    agentId = agentId,
                    title = session.title,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt,
                    isArchived = session.isArchived
                )
            )
            index to newId
        }.toMap()

        val newMessageIds = manifest.messages.mapIndexedNotNull { index, message ->
            val sessionId = newSessionIds[message.sessionIndex]
            if (sessionId != null) {
                val newId = UUID.randomUUID().toString()
                messageDao.insertMessage(
                    Message(
                        id = newId,
                        sessionId = sessionId,
                        role = message.role,
                        content = message.content,
                        isInternalThought = message.isInternalThought,
                        tokenCount = message.tokenCount,
                        timestamp = message.timestamp
                    )
                )
                index to newId
            } else {
                null
            }
        }.toMap()

        manifest.attachments.forEach { attachment ->
            val sessionId = newSessionIds[attachment.sessionIndex]
            val messageId = newMessageIds[attachment.messageIndex]
            if (sessionId != null && messageId != null) {
                messageAttachmentDao.insertAttachment(
                    MessageAttachment(
                        id = UUID.randomUUID().toString(),
                        messageId = messageId,
                        sessionId = sessionId,
                        displayName = attachment.displayName,
                        mimeType = attachment.mimeType,
                        localPath = File(newFolder, attachment.relativePath).absolutePath,
                        sizeBytes = attachment.sizeBytes
                    )
                )
            }
        }

        entries.forEach { (name, bytes) ->
            if (name != MANIFEST_NAME) {
                val target = File(newFolder, name)
                target.parentFile?.mkdirs()
                target.writeBytes(bytes)
            }
        }

        return projectEntity
    }

    private fun collectProjectFiles(root: File): List<Pair<String, ByteArray>> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val result = mutableListOf<Pair<String, ByteArray>>()
        root.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relative = file.relativeTo(root).path.replace(File.separatorChar, '/')
                result.add(relative to file.readBytes())
            }
        }
        return result
    }

    private fun sanitizeFolderName(name: String): String =
        name.replace(Regex("[^a-zA-Z0-9_-]+"), "_").trim('_').ifBlank { "projekt" }
}
