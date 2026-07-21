package com.agents.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agents.app.AgentRepository
import com.agents.app.AgentsApplication
import com.agents.app.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AgentRepository

    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    private val _selectedAgent = MutableStateFlow<Agent?>(null)
    val selectedAgent: StateFlow<Agent?> = _selectedAgent.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _ollamaUrl = MutableStateFlow("http://localhost:11434")
    val ollamaUrl: StateFlow<String> = _ollamaUrl.asStateFlow()

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
        _selectedAgent.value = agent
        if (agent != null) {
            viewModelScope.launch {
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
        type: AgentType,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) {
        viewModelScope.launch {
            val agent = Agent(
                name = name,
                description = description,
                type = type,
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
            val result = repository.chat(
                agent = agent,
                userMessage = content,
                apiKey = _apiKey.value,
                baseUrl = _ollamaUrl.value
            )
            _isLoading.value = false
        }
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
    }

    fun updateOllamaUrl(url: String) {
        _ollamaUrl.value = url
    }

    fun clearChat() {
        val agent = _selectedAgent.value ?: return
        viewModelScope.launch {
            repository.clearMessages(agent.id)
        }
    }
}
