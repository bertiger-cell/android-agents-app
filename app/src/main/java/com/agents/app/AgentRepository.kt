package com.agents.app

import com.agents.app.ai.AIProviderService
import com.agents.app.db.AgentDatabase
import com.agents.app.models.*
import kotlinx.coroutines.flow.Flow

class AgentRepository(private val database: AgentDatabase) {
    private val agentDao = database.agentDao()
    private val messageDao = database.messageDao()
    private val aiService = AIProviderService()

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

    // AI Operations
    suspend fun chat(
        agent: Agent,
        userMessage: String,
        apiKey: String,
        baseUrl: String
    ): AgentResult {
        // Save user message
        addMessage(
            Message(
                agentId = agent.id,
                role = MessageRole.USER,
                content = userMessage
            )
        )

        // Get conversation history
        val messages = mutableListOf<ApiMessage>()
        messages.add(ApiMessage(role = "system", content = agent.systemPrompt))
        messages.add(ApiMessage(role = "user", content = userMessage))

        // Call AI Provider
        val result = aiService.sendMessage(
            provider = agent.provider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = agent.model,
            messages = messages,
            maxTokens = agent.maxTokens,
            temperature = agent.temperature
        )

        // Save assistant response
        if (result.success) {
            addMessage(
                Message(
                    agentId = agent.id,
                    role = MessageRole.ASSISTANT,
                    content = result.output,
                    tokenCount = result.tokensUsed
                )
            )

            // Update agent last run time
            updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))
        }

        return result
    }

    // Automation
    suspend fun executeAgentTask(agent: Agent, apiKey: String, baseUrl: String): AgentResult {
        return chat(
            agent = agent,
            userMessage = "Execute your automation task.",
            apiKey = apiKey,
            baseUrl = baseUrl
        )
    }
}
