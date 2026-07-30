package com.agents.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agents.app.AgentRepository
import com.agents.app.AgentsApplication
import com.agents.app.ai.AIProviderService
import com.agents.app.data.ArchitectConfigRepository
import com.agents.app.data.ProviderCredentials
import com.agents.app.data.ProviderCredentialsRepository
import com.agents.app.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AgentRepository
    private val credentialsRepository = ProviderCredentialsRepository(application)
    private val architectConfigRepository = ArchitectConfigRepository(application)
    private val aiService = AIProviderService()
    private var messagesJob: Job? = null

    // ===== Projects =====
    private val _projects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val projects: StateFlow<List<ProjectEntity>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<ProjectEntity?>(null)
    val selectedProject: StateFlow<ProjectEntity?> = _selectedProject.asStateFlow()

    // ===== Agents (gefiltert nach selectedProject) =====
    private val _agents = MutableStateFlow<List<Agent>>(emptyList())
    val agents: StateFlow<List<Agent>> = _agents.asStateFlow()

    // ===== Sessions (agentId -> list) =====
    private val _sessions = MutableStateFlow<Map<String, List<ChatSessionEntity>>>(emptyMap())
    val sessions: StateFlow<Map<String, List<ChatSessionEntity>>> = _sessions.asStateFlow()

    // ===== Messages =====
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ===== Current Session =====
    private val _selectedSession = MutableStateFlow<ChatSessionEntity?>(null)
    val selectedSession: StateFlow<ChatSessionEntity?> = _selectedSession.asStateFlow()

    // ===== Model-Fetch States (CreateAgentScreen) =====
    private val _availableOllamaModels = MutableStateFlow<List<OllamaModel>>(emptyList())
    val availableOllamaModels: StateFlow<List<OllamaModel>> = _availableOllamaModels.asStateFlow()

    private val _availableOpenRouterModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableOpenRouterModels: StateFlow<List<OpenAIModel>> = _availableOpenRouterModels.asStateFlow()

    private val _availableZenModels = MutableStateFlow<List<OpenAIModel>>(emptyList())
    val availableZenModels: StateFlow<List<OpenAIModel>> = _availableZenModels.asStateFlow()

    // ===== Test States (SettingsScreen) =====
    private val _ollamaTestMessage = MutableStateFlow<String?>(null)
    val ollamaTestMessage: StateFlow<String?> = _ollamaTestMessage.asStateFlow()

    private val _isOllamaTesting = MutableStateFlow(false)
    val isOllamaTesting: StateFlow<Boolean> = _isOllamaTesting.asStateFlow()

    // ===== Credentials =====
    val credentials: StateFlow<ProviderCredentials> =
        credentialsRepository.credentials.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ProviderCredentials()
        )

    // ===== Architect Config =====
    val architectSystemPrompt: StateFlow<String> =
        architectConfigRepository.architectSystemPrompt.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DEFAULT_ARCHITECT_PROMPT
        )

    init {
        val app = application as AgentsApplication
        repository = AgentRepository(app.database, app)

        viewModelScope.launch {
            repository.getAllProjects().collect { projectList ->
                _projects.value = projectList
            }
        }
    }

    // ===== PROJECT MANAGEMENT =====

    suspend fun createProject(name: String, description: String = ""): Project {
        val entity = repository.createProject(name, description)
        return Project(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            folderPath = entity.folderPath
        )
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun selectProject(project: ProjectEntity) {
        _selectedProject.value = project
        _agents.value = emptyList()
        _sessions.value = emptyMap()
        _messages.value = emptyList()
        _selectedSession.value = null
        messagesJob?.cancel()

        messagesJob = viewModelScope.launch {
            repository.getAgentsByProject(project.id)
                .flatMapLatest { agentList ->
                    _agents.value = agentList
                    if (agentList.isEmpty()) {
                        _sessions.value = emptyMap()
                        return@flatMapLatest flowOf(emptyMap())
                    }
                    combine(
                        agentList.map { agent ->
                            repository.getSessionsByAgent(agent.id)
                                .map { sessions -> agent.id to sessions }
                        }
                    ) { pairs -> pairs.toMap() }
                }
                .collect { sessionMap ->
                    _sessions.value = sessionMap
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

    fun startChat(agentId: String) {
        viewModelScope.launch {
            val session = repository.getOrCreateSession(agentId)
            _selectedSession.value = session
        }
    }

    // ===== ARCHITECT AGENT =====

    suspend fun createOrGetArchitectAgent(projectId: String): Agent {
        // Check if Architect agent already exists in this project
        val existingAgents = repository.getAgentsByProject(projectId).first()
        val existing = existingAgents.find { it.name == "Architect" }
        if (existing != null) return existing

        // Get custom prompt from DataStore (or default)
        val customPrompt = architectConfigRepository.architectSystemPrompt.first()

        // Create new Architect agent
        val architectAgent = Agent(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            name = "Architect",
            description = "Project Discovery & Planning Agent",
            provider = AIProvider.OPENROUTER,
            model = "gpt-4o",
            systemPrompt = customPrompt,
            temperature = 0.7f,
            maxTokens = 8192
        )
        repository.createAgent(
            projectId = projectId,
            name = architectAgent.name,
            description = architectAgent.description,
            provider = architectAgent.provider,
            systemPrompt = architectAgent.systemPrompt,
            model = architectAgent.model,
            temperature = architectAgent.temperature
        )
        return architectAgent
    }

    suspend fun createArchitectDiscoverySession(projectId: String): ChatSessionEntity {
        val architect = createOrGetArchitectAgent(projectId)

        val session = repository.getOrCreateSession(
            agentId = architect.id,
            title = "Project Discovery"
        )

        val introMessage = """
            Hallo! Ich bin dein Projekt-Architect.
            
            Lass mich dein Projekt verstehen. Erzaehl mir:
            - Was moechtest du bauen?
            - Was ist die grosse Idee dahinter?
            
            (Keine Eile – lass uns brainstormen!)
        """.trimIndent()

        repository.addMessage(
            sessionId = session.id,
            role = MessageRole.ASSISTANT,
            content = introMessage,
            isInternalThought = false
        )

        return session
    }

    // ===== SESSION MANAGEMENT =====

    fun selectSession(session: ChatSessionEntity?) {
        _selectedSession.value = session
        _messages.value = emptyList()
        messagesJob?.cancel()

        if (session != null) {
            messagesJob = viewModelScope.launch {
                repository.getMessagesBySession(session.id).collect { messageList ->
                    _messages.value = messageList
                }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            if (session != null) {
                repository.updateSession(session.copy(title = newTitle.trim()))
                _selectedSession.value = _selectedSession.value?.let {
                    if (it.id == sessionId) it.copy(title = newTitle.trim()) else null
                }
            }
        }
    }

    // ===== CHAT =====

    fun sendMessage(content: String) {
        val session = _selectedSession.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true

            // Load agent from DB as fallback if not in _agents state
            val agent = _agents.value.find { it.id == session.agentId }
                ?: repository.getAgentById(session.agentId)
            if (agent == null) {
                val errorMsg = Message(
                    sessionId = session.id,
                    role = MessageRole.ASSISTANT,
                    content = "Fehler: Agent nicht gefunden."
                )
                addMessage(errorMsg)
                _isLoading.value = false
                return@launch
            }

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
                    content = "Fehler: Kein API-Key fur ${agent.provider.name} konfiguriert. Gehe zu Einstellungen."
                )
                addMessage(errorMsg)
                _isLoading.value = false
                return@launch
            }

            try {
                // Add user message to in-memory list only.
                // repository.chat() handles DB insertion + history building.
                val userMsg = Message(
                    sessionId = session.id,
                    role = MessageRole.USER,
                    content = content
                )
                addMessage(userMsg)

                val result = repository.chat(
                    agentId = agent.id,
                    userMessage = content,
                    apiKey = apiKey,
                    baseUrl = baseUrl
                )

                if (result.success && result.output.isNotBlank()) {
                    val assistantMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = result.output,
                        tokenCount = result.tokensUsed
                    )
                    addMessage(assistantMsg)

                    repository.updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))
                } else {
                    val errorText = result.error ?: "Unbekannter Fehler"
                    Log.e("AgentViewModel", "Chat error: $errorText")
                    val errorMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = "Fehler: $errorText"
                    )
                    addMessage(errorMsg)
                }

            } catch (e: Exception) {
                Log.e("AgentViewModel", "sendMessage failed", e)
                val errorMsg = Message(
                    sessionId = session.id,
                    role = MessageRole.ASSISTANT,
                    content = "Fehler: ${e.message ?: "Unbekannter Fehler"}"
                )
                addMessage(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper: add message to in-memory list
    private fun addMessage(msg: Message) {
        _messages.value = _messages.value + msg
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

    // ===== ARCHITECT SETTINGS =====

    fun updateArchitectSystemPrompt(prompt: String) {
        viewModelScope.launch {
            architectConfigRepository.updateArchitectSystemPrompt(prompt)
        }
    }

    // ===== TEST CONNECTION =====

    fun testOllamaConnection(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            _isOllamaTesting.value = true
            _ollamaTestMessage.value = null

            try {
                val result = aiService.testOllamaConnection(baseUrl, apiKey)
                _ollamaTestMessage.value = result.message
            } catch (e: Exception) {
                _ollamaTestMessage.value = e.message ?: "Unbekannter Fehler"
            } finally {
                _isOllamaTesting.value = false
            }
        }
    }

    // ===== MODEL FETCHING =====

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
}
