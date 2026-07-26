package com.agents.app

import com.agents.app.db.AgentDatabase
import com.agents.app.models.*
import kotlinx.coroutines.flow.Flow

class AgentRepository(private val database: AgentDatabase) {
    private val agentDao = database.agentDao()
    private val messageDao = database.messageDao()
    private val projectDao = database.projectDao()
    private val chatSessionDao = database.chatSessionDao()

    // Agent operations
    fun getAllAgents(): Flow<List<Agent>> = agentDao.getAllAgents()

    fun getAgentsByProject(projectId: String): Flow<List<Agent>> =
        agentDao.getAgentsByProject(projectId)

    suspend fun getAgentById(agentId: String): Agent? = agentDao.getAgentById(agentId)

    suspend fun createAgent(agent: Agent) = agentDao.insertAgent(agent)

    suspend fun updateAgent(agent: Agent) = agentDao.updateAgent(agent)

    suspend fun deleteAgent(agent: Agent) = agentDao.deleteAgent(agent)

    suspend fun deleteAgentById(agentId: String) = agentDao.deleteAgentById(agentId)

    // Message operations
    fun getMessagesBySession(sessionId: String): Flow<List<Message>> =
        messageDao.getMessagesBySession(sessionId)

    suspend fun addMessage(message: Message) = messageDao.insertMessage(message)

    suspend fun deleteMessagesBySession(sessionId: String) =
        messageDao.deleteMessagesBySession(sessionId)

    // Project operations
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(projectId: String): ProjectEntity? =
        projectDao.getProjectById(projectId)

    suspend fun createProject(project: ProjectEntity) = projectDao.insertProject(project)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) = projectDao.deleteProject(project)

    // ChatSession operations
    fun getSessionsByProject(projectId: String): Flow<List<ChatSessionEntity>> =
        chatSessionDao.getSessionsByProject(projectId)

    suspend fun getSessionById(sessionId: String): ChatSessionEntity? =
        chatSessionDao.getSessionById(sessionId)

    suspend fun getLatestSessionForAgent(agentId: String): ChatSessionEntity? =
        chatSessionDao.getLatestSessionForAgent(agentId)

    suspend fun createSession(session: ChatSessionEntity) = chatSessionDao.insertSession(session)

    suspend fun updateSession(session: ChatSessionEntity) = chatSessionDao.updateSession(session)

    suspend fun deleteSession(session: ChatSessionEntity) = chatSessionDao.deleteSession(session)
}
