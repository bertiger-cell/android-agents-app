package com.agents.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agents.app.AgentRepository
import com.agents.app.AgentsApplication
import com.agents.app.ai.AIProviderService
import com.agents.app.data.ProviderCredentials
import com.agents.app.data.ProviderCredentialsRepository
import com.agents.app.models.*
import com.agents.app.models.ApiMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AgentRepository
    private val credentialsRepository = ProviderCredentialsRepository(application)
    private val aiService = AIProviderService()
    private var messagesJob: Job? = null

    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _selectedAgent = MutableStateFlow<Agent?>(null)
    val selectedAgent: StateFlow<Agent?> = _selectedAgent.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ollamaTestMessage = MutableStateFlow<String?>(null)
    val ollamaTestMessage: StateFlow<String?> = _ollamaTestMessage.asStateFlow()

    private val _isOllamaTesting = MutableStateFlow(false)
    val isOllamaTesting: StateFlow<Boolean> = _isOllamaTesting.asStateFlow()

    // Ollama models
    private val _availableOllamaModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val availableOllamaModels: StateFlow<List<OllamaModel>> = _availableOllamaModels.asStateFlow()

    // OpenRouter models
    private val _availableOpenRouterModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableOpenRouterModels: StateFlow<List<OpenAIModel>> = _availableOpenRouterModels.asStateFlow()

    // Zen models
    private val _availableZenModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableZenModels: StateFlow<List<OpenAIModel>> = _availableZenModels.asStateFlow()

    val credentials: StateFlow<ProviderCredentials> =
        credentialsRepository.credentials.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ProviderCredentials()
        )

    init {
        val app = application as AgentsApplication
        repository = AgentRepository(app.database)

        viewModelScope.launch {
            repository.getAllAgents().collect { agentList ->
                _agents.value = agentList
            }
        }
    }

    fun selectAgent(agent: Agent?) {
        messagesJob?.cancel()
        _selectedAgent.value = agent
        if (agent != null) {
            messagesJob = viewModelScope.launch {
                repository.getMessagesByAgent(agent.id).collect { messages ->
                    _messages.value = messages
                }
            }
        } else {
            _messages.value = emptyList()
        }
    }

    fun createAgent(
        name: String,
        description: String,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) {
        viewModelScope.launch {
            val agent = Agent(
                name = name,
                description = description,
                provider = provider,
                systemPrompt = systemPrompt,
                model = model,
                temperature = temperature
            )
            repository.createAgent(agent)
        }
    }

    fun deleteAgent(agent: Agent) {
        viewModelScope.launch {
            repository.deleteAgent(agent)
            if (_selectedAgent.value?.id == agent.id) {
                _selectedAgent.value = null
            }
        }
    }

    fun sendMessage(content: String) {
        val agent = _selectedAgent.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            val creds = credentials.value
            val (apiKey, baseUrl) = when (agent.provider) {
                AIProvider.OPENROUTER -> creds.openRouterKey to ""
                AIProvider.ZEN -> creds.zenKey to ""
                AIProvider.OLLAMA -> creds.ollamaApiKey to creds.ollamaBaseUrl
            }

            // Build message list with history
            val history = repository.getMessagesByAgent(agent.id).first()
            val messages = mutableListOf<ApiMessage>()
            messages.add(ApiMessage(role = "system", content = agent.systemPrompt))
            messages.addAll(history.map { ApiMessage(role = it.role.name.lowercase(), content = it.content) })
            messages.add(ApiMessage(role = "user", content = content))

            // Save user message to DB
            repository.addMessage(
                Message(agentId = agent.id, role = MessageRole.USER, content = content)
            )

            // Stream response
            val streamingMessageId = java.util.UUID.randomUUID().toString()
            var finalOutput = ""

            aiService.streamMessage(
                provider = agent.provider,
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = agent.model,
                messages = messages,
                maxTokens = agent.maxTokens,
                temperature = agent.temperature
            ).collect { result ->
                finalOutput = result.output
                // Update streaming message in the list
                val streamingMsg = Message(
                    id = streamingMessageId,
                    agentId = agent.id,
                    role = MessageRole.ASSISTANT,
                    content = result.output,
                    tokenCount = result.tokensUsed
                )
                val currentMessages = _messages.value.toMutableList()
                val existingIndex = currentMessages.indexOfFirst { it.id == streamingMessageId }
                if (existingIndex >= 0) {
                    currentMessages[existingIndex] = streamingMsg
                } else {
                    currentMessages.add(streamingMsg)
                }
                _messages.value = currentMessages
            }

            // Save final response to DB
            repository.addMessage(
                Message(
                    agentId = agent.id,
                    role = MessageRole.ASSISTANT,
                    content = finalOutput,
                    tokenCount = finalOutput.split("\\s+".toRegex()).size
                )
            )
            repository.updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))

            _isLoading.value = false
        }
    }

    fun updateOpenRouterKey(key: String) {
        viewModelScope.launch { credentialsRepository.updateOpenRouterKey(key) }
    }

    fun updateZenKey(key: String) {
        viewModelScope.launch { credentialsRepository.updateZenKey(key) }
    }

    fun updateOllamaBaseUrl(url: String) {
        viewModelScope.launch { credentialsRepository.updateOllamaBaseUrl(url) }
    }

    fun updateOllamaApiKey(key: String) {
        viewModelScope.launch { credentialsRepository.updateOllamaApiKey(key) }
    }

    fun testOllamaConnection(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _isOllamaTesting.value = true
            _ollamaTestMessage.value = null

            try {
                val result = aiService.testOllamaConnection(
                    baseUrl = baseUrl,
                    apiKey = apiKey
                )
                _ollamaTestMessage.value = result.message
            } catch (e: Exception) {
                _ollamaTestMessage.value = e.message ?: "Unbekannter Fehler"
            } finally {
                _isOllamaTesting.value = false
            }
        }
    }

    fun fetchAvailableOllamaModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            val models = aiService.fetchOllamaModels(baseUrl, apiKey)
            _availableOllamaModels.value = models
        }
    }

    fun fetchAvailableOpenRouterModels(apiKey: String) {
        viewModelScope.launch {
            val models = aiService.fetchOpenAICompatibleModels(
                endpoint = "https://openrouter.ai/api/v1/models",
                apiKey = apiKey
            )
            _availableOpenRouterModels.value = models
        }
    }

    fun fetchAvailableZenModels(apiKey: String) {
        viewModelScope.launch {
            val models = aiService.fetchOpenAICompatibleModels(
                endpoint = "https://opencode.ai/zen/v1/models",
                apiKey = apiKey
            )
            _availableZenModels.value = models
        }
    }

    fun clearChat() {
        val agent = _selectedAgent.value ?: return
        viewModelScope.launch {
            repository.clearMessages(agent.id)
        }
    }
}
