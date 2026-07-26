package com.agents.app

import com.agents.app.db.AgentDatabase
import com.agents.app.models.*
import kotlinx.coroutines.flow.Flow

class AgentRepository(private val database: AgentDatabase) {
    private val agentDao = database.agentDao()
    private val messageDao = database.messageDao()

    // Agent operations
    fun getAllAgents(): Flow<List<Agent>> = agentDao.getAllAgents()

    fun getActiveAgents(): Flow<List<Agent>> = agentDao.getActiveAgents()

    suspend fun getAgentById(agentId: String): Agent? = agentDao.getAgentById(agentId)

    suspend fun createAgent(agent: Agent) = agentDao.insertAgent(agent)

    suspend fun updateAgent(agent: Agent) = agentDao.updateAgent(agent)

    suspend fun deleteAgent(agent: Agent) = agentDao.deleteAgent(agent)

    suspend fun deleteAgentById(agentId: String) = agentDao.deleteAgentById(agentId)

    // Message operations
    fun getMessagesByAgent(agentId: String): Flow<List<Message>> =
        messageDao.getMessagesByAgent(agentId)

    suspend fun addMessage(message: Message) = messageDao.insertMessage(message)

    suspend fun clearMessages(agentId: String) = messageDao.deleteMessagesByAgent(agentId)
}
