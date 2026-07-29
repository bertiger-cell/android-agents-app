package com.agents.app.ui

import android.app.Application
import android.util.Log
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
    private var agentsJob: Job? = null

    // ===== Projects =====
    private val _projects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val projects: StateFlow<List<ProjectEntity>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // ===== Agents (gefiltert nach selectedProject) =====
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    // ===== Sessions (agentId → list of sessions) =====
    private val _sessions = MutableStateFlow<Map<String, List<ChatSessionEntity>>>(emptyMap())
    val sessions: StateFlow<Map<String, List<ChatSessionEntity>>> = _sessions.asStateFlow()

    // ===== Messages (einer Session) =====
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // ===== Current session for chat =====
    private val _selectedSession = MutableStateFlow<ChatSessionEntity?>(null)
    val selectedSession: StateFlow<ChatSessionEntity?> = _selectedSession.asStateFlow()

    // ===== Loading state =====
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ===== Credentials =====
    val credentials: StateFlow<ProviderCredentials> =
        credentialsRepository.credentials.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ProviderCredentials()
        )

    // ===== Test & Model states (unchanged) =====
    private val _ollamaTestMessage = MutableStateFlow<String?>(null)
    val ollamaTestMessage: StateFlow<String?> = _ollamaTestMessage.asStateFlow()

    private val _isOllamaTesting = MutableStateFlow(false)
    val isOllamaTesting: StateFlow<Boolean> = _isOllamaTesting.asStateFlow()

    private val _availableOllamaModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val availableOllamaModels: StateFlow<List<OllamaModel>> = _availableOllamaModels.asStateFlow()

    private val _availableOpenRouterModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableOpenRouterModels: StateFlow<List<OpenAIModel>> = _availableOpenRouterModels.asStateFlow()

    private val _availableZenModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableZenModels: StateFlow<List<OpenAIModel>> = _availableZenModels.asStateFlow()

    init {
        val app = application as AgentsApplication
        repository = AgentRepository(app.database, app)

        // Alle Projekte laden
        viewModelScope.launch {
            repository.getAllProjects().collect { projectList ->
                _projects.value = projectList
            }
        }
    }

    // ===== PROJECT MANAGEMENT =====

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            repository.createProject(name, description)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
        }
    }

    fun selectProject(project: ProjectEntity?) {
        messagesJob?.cancel()
        agentsJob?.cancel()
        _selectedProject.value = project
        _agents.value = emptyList()
        _sessions.value = emptyMap()
        _messages.value = emptyList()
        _selectedSession.value = null

        if (project != null) {
            agentsJob = viewModelScope.launch {
                repository.getAgentsByProject(project.id).collect { agentList ->
                    _agents.value = agentList
                    // Sessions werden einzeln pro Agent geladen
                    _sessions.value = emptyMap()
                    agentList.forEach { agent ->
                        launch {
                            repository.getSessionsByAgent(agent.id).collect { sessionList ->
                                _sessions.value = _sessions.value + (agent.id to sessionList)
                            }
                        }
                    }
                }
            }
        }
    }

    // ===== AGENT MANAGEMENT =====

    fun createAgent(
        projectId: String,
        name: String,
        description: String,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) {
        viewModelScope.launch {
            repository.createAgent(
                projectId = projectId,
                name = name,
                description = description,
                provider = provider,
                systemPrompt = systemPrompt,
                model = model,
                temperature = temperature
            )
        }
    }

    fun deleteAgent(agent: Agent) {
        viewModelScope.launch {
            repository.deleteAgent(agent)
        }
    }

    // ===== SESSION MANAGEMENT =====

    fun selectSession(session: ChatSessionEntity?) {
        messagesJob?.cancel()
        _selectedSession.value = session
        _messages.value = emptyList()

        if (session != null) {
            messagesJob = viewModelScope.launch {
                repository.getMessagesBySession(session.id).collect { msgList ->
                    _messages.value = msgList
                }
            }
        }
    }

    /**
     * Startet einen Chat für einen Agenten.
     * Erstellt eine Session falls keine existiert.
     */
    fun startChat(agentId: String) {
        viewModelScope.launch {
            try {
                val session = repository.getOrCreateSession(agentId)
                selectSession(session)
            } catch (e: Exception) {
                Log.e("AgentViewModel", "startChat failed", e)
            }
        }
    }

    fun clearChat() {
        val session = _selectedSession.value ?: return
        viewModelScope.launch {
            repository.deleteMessagesBySession(session.id)
        }
    }

    // ===== CHAT =====

    fun sendMessage(content: String) {
        val session = _selectedSession.value ?: return
        val agent = _agents.value.find { it.id == session.agentId } ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val creds = credentials.value
                val (apiKey, baseUrl) = when (agent.provider) {
                    AIProvider.OPENROUTER -> creds.openRouterKey to ""
                    AIProvider.ZEN -> creds.zenKey to ""
                    AIProvider.OLLAMA -> creds.ollamaApiKey to creds.ollamaBaseUrl
                }

                if (apiKey.isBlank() && agent.provider != AIProvider.OLLAMA) {
                    val errorMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = "Fehler: Kein API-Key fuer ${agent.provider.name} konfiguriert. Bitte in den Einstellungen hinterlegen."
                    )
                    repository.addMessage(errorMsg)
                    _messages.value = _messages.value + errorMsg
                    return@launch
                }

                val history = repository.getMessagesBySession(session.id).first()
                val messages = mutableListOf<ApiMessage>()
                messages.add(ApiMessage(role = "system", content = agent.systemPrompt))
                messages.addAll(history.map { ApiMessage(role = it.role.name.lowercase(), content = it.content) })
                messages.add(ApiMessage(role = "user", content = content))

                repository.addMessage(
                    Message(sessionId = session.id, role = MessageRole.USER, content = content)
                )

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
                    if (result.success) {
                        finalOutput = result.output
                        val streamingMsg = Message(
                            id = streamingMessageId,
                            sessionId = session.id,
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
                    } else {
                        val errorText = result.error ?: "Unbekannter Fehler"
                        Log.e("AgentViewModel", "Streaming error: $errorText")
                        val errorMsg = Message(
                            sessionId = session.id,
                            role = MessageRole.ASSISTANT,
                            content = "Fehler: $errorText"
                        )
                        val currentMessages = _messages.value.toMutableList()
                        val existingIndex = currentMessages.indexOfFirst { it.id == streamingMessageId }
                        if (existingIndex >= 0) {
                            currentMessages[existingIndex] = errorMsg
                        } else {
                            currentMessages.add(errorMsg)
                        }
                        _messages.value = currentMessages
                    }
                }

                if (finalOutput.isNotBlank()) {
                    repository.addMessage(
                        Message(
                            sessionId = session.id,
                            role = MessageRole.ASSISTANT,
                            content = finalOutput,
                            tokenCount = finalOutput.split("\\s+".toRegex()).size
                        )
                    )
                    repository.updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))
                }

            } catch (e: Exception) {
                Log.e("AgentViewModel", "sendMessage failed", e)
                val session = _selectedSession.value
                if (session != null) {
                    val errorMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = "Fehler: ${e.message ?: "Unbekannter Fehler"}"
                    )
                    repository.addMessage(errorMsg)
                    _messages.value = _messages.value + errorMsg
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== CREDENTIALS =====

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
                val result = aiService.testOllamaConnection(baseUrl, apiKey)
                _ollamaTestMessage.value = result.message
            } catch (e: Exception) {
                _ollamaTestMessage.value = e.message ?: "Fehler"
            } finally {
                _isOllamaTesting.value = false
            }
        }
    }

    // ===== MODEL FETCHING =====

    fun fetchAvailableOllamaModels(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _availableOllamaModels.value = aiService.fetchOllamaModels(baseUrl, apiKey)
        }
    }

    fun fetchAvailableOpenRouterModels(apiKey: String) {
        viewModelScope.launch {
            _availableOpenRouterModels.value = aiService.fetchOpenAICompatibleModels(
                endpoint = "https://openrouter.ai/api/v1/models",
                apiKey = apiKey
            )
        }
    }

    fun fetchAvailableZenModels(apiKey: String) {
        viewModelScope.launch {
            _availableZenModels.value = aiService.fetchOpenAICompatibleModels(
                endpoint = "https://opencode.ai/zen/v1/models",
                apiKey = apiKey
            )
        }
    }
}
