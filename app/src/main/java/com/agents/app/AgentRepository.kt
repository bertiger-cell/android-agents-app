package com.agents.app

import android.content.Context
import com.agents.app.ai.AIProviderService
import com.agents.app.db.AgentDatabase
import com.agents.app.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


import android.util.Log
 
import org.json.JSONArray
import org.json.JSONObject
class AgentRepository(
    private val database: AgentDatabase,
    private val context: Context
) {
    private val agentDao = database.agentDao()
    private val messageDao = database.messageDao()
    private val projectDao = database.projectDao()
    private val chatSessionDao = database.chatSessionDao()
    private val aiService = AIProviderService()

    // --- Project operations ---

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(projectId: String): ProjectEntity? =
        projectDao.getProjectById(projectId)

    suspend fun createProject(name: String, description: String = ""): ProjectEntity {
        val projectId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val folderPath = context.filesDir.path + "/projects/${name}_$projectId"
        File(folderPath).mkdirs()
        // Leeres Projekt-Tagebuch anlegen
        File(folderPath, "diary.md").writeText("# Project Diary – $name\n\n")

        val projectEntity = ProjectEntity(
            id = projectId,
            name = name,
            description = description,
            createdAt = timestamp,
            updatedAt = timestamp,
            folderPath = folderPath,
            color = "#6200EE",
            tags = "[]"
        )

        projectDao.insertProject(projectEntity)
        return projectEntity
    }

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProject(projectId: String) = projectDao.deleteProjectById(projectId)

    // --- Agent operations ---

    fun getAllAgents(): Flow<List<Agent>> = agentDao.getAllAgents()

    fun getAgentsByProject(projectId: String): Flow<List<Agent>> =
        agentDao.getAgentsByProject(projectId)

    suspend fun getAgentById(agentId: String): Agent? = agentDao.getAgentById(agentId)

    suspend fun createAgent(
        projectId: String,
        name: String,
        description: String,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) {
        val agent = Agent(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            name = name,
            description = description,
            provider = provider,
            model = model,
            systemPrompt = systemPrompt,
            temperature = temperature,
            maxTokens = 4096
        )
        agentDao.insertAgent(agent)
    }

    suspend fun updateAgent(agent: Agent) = agentDao.updateAgent(agent)

    suspend fun deleteAgent(agent: Agent) = agentDao.deleteAgent(agent)

    suspend fun deleteAgentById(agentId: String) = agentDao.deleteAgentById(agentId)

    // --- Session operations ---

    fun getSessionsByProject(projectId: String): Flow<List<ChatSessionEntity>> =
        chatSessionDao.getSessionsByProject(projectId)

    fun getSessionsByAgent(agentId: String): Flow<List<ChatSessionEntity>> =
        chatSessionDao.getSessionsByAgent(agentId)

    suspend fun getSessionById(sessionId: String): ChatSessionEntity? =
        chatSessionDao.getSessionById(sessionId)

    suspend fun getLatestSessionForAgent(agentId: String): ChatSessionEntity? =
        chatSessionDao.getLatestSessionForAgent(agentId)

    suspend fun getOrCreateSession(
        agentId: String,
        title: String = "Chat ${System.currentTimeMillis()}"
    ): ChatSessionEntity {
        var session = chatSessionDao.getLatestSessionForAgent(agentId)

        if (session == null) {
            val agent = agentDao.getAgentById(agentId)
                ?: throw Exception("Agent not found")

            session = ChatSessionEntity(
                id = UUID.randomUUID().toString(),
                projectId = agent.projectId,
                agentId = agentId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isArchived = false
            )
            chatSessionDao.insertSession(session)
        }

        return session
    }

    suspend fun createSession(session: ChatSessionEntity) = chatSessionDao.insertSession(session)

    suspend fun updateSession(session: ChatSessionEntity) = chatSessionDao.updateSession(session)

    suspend fun deleteSession(session: ChatSessionEntity) = chatSessionDao.deleteSession(session)

    // --- Message operations ---

    fun getMessagesBySession(sessionId: String): Flow<List<Message>> =
        messageDao.getMessagesBySession(sessionId)

    // Backward-compatible overload (used by ViewModel)
    suspend fun addMessage(message: Message) = messageDao.insertMessage(message)

    suspend fun addMessage(
        sessionId: String,
        role: MessageRole,
        content: String,
        isInternalThought: Boolean = false
    ) {
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = role,
            content = content,
            isInternalThought = isInternalThought,
            tokenCount = 0,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessagesBySession(sessionId: String) =
        messageDao.deleteMessagesBySession(sessionId)

    // --- Project Diary --------------------------------------------------

    suspend fun readDiary(projectId: String): String {
        val project = projectDao.getProjectById(projectId) ?: return ""
        val file = File("${project.folderPath}/diary.md")
        return if (file.exists()) file.readText() else ""
    }

    suspend fun appendToDiary(projectId: String, role: String, content: String) {
        val project = projectDao.getProjectById(projectId) ?: return
        val file = File("${project.folderPath}/diary.md")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val entry = "\n## $timestamp – $role\n$content\n"
        file.appendText(entry)
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    // --- Chat (hybrid session lifecycle) ---

    suspend fun chat(
        agentId: String,
        userMessage: String,
        apiKey: String,
        baseUrl: String
    ): AgentResult {
        // 1. Agent laden
        val agent = agentDao.getAgentById(agentId)
            ?: throw Exception("Agent not found")

        // 2. Session erstellen oder laden
        val session = getOrCreateSession(agentId)

        // 3. User-Message speichern
        addMessage(
            sessionId = session.id,
            role = MessageRole.USER,
            content = userMessage
        )

        // 4. Message-History laden
        val history = messageDao.getMessagesBySession(session.id).first()
        val messages = mutableListOf<ApiMessage>()
        messages.add(ApiMessage(role = "system", content = agent.systemPrompt))
        history.forEach { msg ->
            messages.add(ApiMessage(
                role = msg.role.name.lowercase(),
                content = msg.content
            ))
        }

        // 5. AI Provider aufrufen (Streaming sammeln)
        var finalResult = AgentResult(success = false, output = "")
        aiService.streamMessage(
            provider = agent.provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = agent.model,
            messages = messages,
            maxTokens = agent.maxTokens,
            temperature = agent.temperature
        ).collect { result ->
            finalResult = result
        }

        // 6. Assistant-Response speichern
        if (finalResult.success && finalResult.output.isNotBlank()) {
            addMessage(
                sessionId = session.id,
                role = MessageRole.ASSISTANT,
                content = finalResult.output,
                isInternalThought = false
            )

            // Agent + Session updaten
            agentDao.updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))
            chatSessionDao.updateSession(session.copy(updatedAt = System.currentTimeMillis()))

            // Titel generieren (nur beim ersten Chat)
            if (session.title.startsWith("Chat ")) {
                generateAndUpdateTitle(session.id, agent, apiKey, baseUrl, userMessage)
            }
        }

        return finalResult
    }

    private suspend fun generateAndUpdateTitle(
        sessionId: String,
        agent: Agent,
        apiKey: String,
        baseUrl: String,
        firstMessage: String
    ) {
        val titlePrompt = listOf(
            ApiMessage(role = "system", content = "Generate a short 3-5 word title for this conversation. Respond with ONLY the title, no quotes, no punctuation."),
            ApiMessage(role = "user", content = firstMessage)
        )
        val result = aiService.streamMessage(
            provider = agent.provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = agent.model,
            messages = titlePrompt,
            maxTokens = 30,
            temperature = 0.3f
        ).first()

        if (result.success && result.output.isNotBlank()) {
            val title = result.output.trim().removeSurrounding("\"").removeSurrounding("'")
            val session = chatSessionDao.getSessionById(sessionId) ?: return
            chatSessionDao.updateSession(session.copy(title = title))
        }
    }

    suspend fun saveProjectScaffold(projectId: String, scaffoldJson: String): ProjectScaffold? {
        return try {
            // 1. Project aus DB holen für folderPath
            val project = projectDao.getProjectById(projectId) ?: return null
            val discoveryFile = File(project.folderPath, "discovery.json")

            // 2. Parse JSON (robust)
            val jsonObject = JSONObject(scaffoldJson)

            // 3. Extract Discovery Context
            val discoveryObj = jsonObject.optJSONObject("discovery_context") ?: JSONObject()
            val discovery = ProjectDiscovery(
                projectId = projectId,
                domain = discoveryObj.optString("domain", ""),
                technologies = discoveryObj.optJSONArray("technologies")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList(),
                experienceLevel = discoveryObj.optString("experience_level", ""),
                projectSize = discoveryObj.optString("project_size", ""),
                concerns = discoveryObj.optJSONArray("concerns")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }
                } ?: emptyList()
            )

            // 4. Extract Phases
            val phasesArr = jsonObject.optJSONArray("phases") ?: return null
            val phases = (0 until phasesArr.length()).map { i ->
                val phase = phasesArr.getJSONObject(i)
                ScaffoldPhase(
                    phaseNumber = phase.optInt("phase_number", i + 1),
                    name = phase.optString("name", ""),
                    description = phase.optString("description", ""),
                    duration = phase.optString("duration", ""),
                    focus = phase.optString("focus", "")
                )
            }

            // 5. Extract Rules
            val rulesArr = jsonObject.optJSONArray("rules") ?: JSONArray()
            val rules = (0 until rulesArr.length()).map { rulesArr.getString(it) }

            // 6. Extract Suggested Agents
            val agentsArr = jsonObject.optJSONArray("suggested_agents") ?: JSONArray()
            val agents = (0 until agentsArr.length()).map { i ->
                val agent = agentsArr.getJSONObject(i)
                AgentSpec(
                    name = agent.optString("name", "Agent ${i + 1}"),
                    description = agent.optString("description", ""),
                    systemPrompt = agent.optString("system_prompt", ""),
                    provider = agent.optString("provider", "openrouter"),
                    model = agent.optString("model", "gpt-4o"),
                    temperature = agent.optDouble("temperature", 0.7).toFloat()
                )
            }

            // 7. Build ProjectScaffold
            val scaffold = ProjectScaffold(
                discoveryContext = discovery,
                architecture = jsonObject.optString("architecture", ""),
                phases = phases,
                rules = rules,
                suggestedAgents = agents,
                diaryEntry = jsonObject.optString("diary_entry", "")
            )

            // 8. Original JSON in Project-Ordner speichern
            discoveryFile.writeText(scaffoldJson)

            scaffold
        } catch (e: Exception) {
            Log.e("ArchitectJSON", "Fehler beim Parsen: ${e.message}")
            null
        }
    }
}
