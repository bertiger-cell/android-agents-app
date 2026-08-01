package com.agents.app.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agents.app.AgentRepository
import com.agents.app.ScaffoldParseResult
import com.agents.app.AgentsApplication
import com.agents.app.ai.AIProviderService
import com.agents.app.data.ArchitectConfigRepository
import com.agents.app.data.ProviderCredentials
import com.agents.app.data.ProviderCredentialsRepository
import com.agents.app.models.*
import com.agents.app.data.ProjectFileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ProjectFileInfo(
    val name: String,
    val content: String,
    val isMarkdown: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

class AgentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AgentRepository
    private val credentialsRepository = ProviderCredentialsRepository(application)
    private val architectConfigRepository = ArchitectConfigRepository(application)
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

    // ===== Sessions (agentId -> list) =====
    private val _sessions = MutableStateFlow<Map<String, List<ChatSessionEntity>>>(emptyMap())
    val sessions: StateFlow<Map<String, List<ChatSessionEntity>>> = _sessions.asStateFlow()

    // ===== Messages =====
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentError = MutableStateFlow<String?>(null)
    val currentError: StateFlow<String?> = _currentError.asStateFlow()

    private var lastSubmittedMessage: String? = null

    private val _isGeneratingFiles = MutableStateFlow(false)
    val isGeneratingFiles: StateFlow<Boolean> = _isGeneratingFiles.asStateFlow()

    // Event: wird 1x emitted wenn createAgentsFromScaffold fertig ist
    private val _generationComplete = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val generationComplete: SharedFlow<Boolean> = _generationComplete.asSharedFlow()

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

    val architectProvider: StateFlow<String> =
        architectConfigRepository.architectProvider.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            "openrouter"
        )

    val architectModel: StateFlow<String> =
        architectConfigRepository.architectModel.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            "gpt-4o"
        )

    // ===== Architect Discovery State =====
    private val _projectScaffold = MutableStateFlow<ProjectScaffold?>(null)
    val projectScaffold: StateFlow<ProjectScaffold?> = _projectScaffold.asStateFlow()

    private val _isAutoSummaryEnabled = MutableStateFlow(true)
    val isAutoSummaryEnabled: StateFlow<Boolean> = _isAutoSummaryEnabled.asStateFlow()

    private val _showArchitectSummary = MutableStateFlow(false)
    val showArchitectSummary: StateFlow<Boolean> = _showArchitectSummary.asStateFlow()
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

    fun updateProject(projectId: String, name: String, description: String) {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val updated = project.copy(
                name = name.trim(),
                description = description.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            _selectedProject.value = updated
        }
    }

    fun selectProject(project: ProjectEntity) {
        _selectedProject.value = project
        _agents.value = emptyList()
        _sessions.value = emptyMap()
        _messages.value = emptyList()
        _selectedSession.value = null
        messagesJob?.cancel()
        agentsJob?.cancel()

        agentsJob = viewModelScope.launch {
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

    fun restartArchitectDiscovery(projectId: String) {
        viewModelScope.launch {
            val session = createArchitectDiscoverySession(projectId)
            _selectedSession.value = session
        }
    }

    // ===== PROJECT FILES =====

    fun getProjectFiles(projectId: String, onResult: (List<ProjectFileInfo>) -> Unit) {
        viewModelScope.launch {
            val files = withContext(Dispatchers.IO) {
                val projectPath = resolveProjectPath(projectId)
                val dir = File(projectPath)
                if (!dir.exists()) return@withContext emptyList()

                dir.listFiles()
                    ?.filter { it.isFile && (it.name.endsWith(".md") || it.name.endsWith(".json")) }
                    ?.sortedBy { it.name }
                    ?.map { file ->
                        ProjectFileInfo(
                            name = file.name,
                            content = file.readText(),
                            isMarkdown = file.name.endsWith(".md"),
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    } ?: emptyList()
            }
            onResult(files)
        }
    }

    private fun resolveProjectPath(projectId: String): String {
        val projectFolder = File(getApplication<AgentsApplication>().filesDir, "projects")
        val projects = projectFolder.listFiles() ?: emptyArray()
        val projectDir = projects.firstOrNull { it.isDirectory && it.name.endsWith("_$projectId") }
        return projectDir?.absolutePath ?: "$projectFolder/project_$projectId"
    }

    // ===== ARCHITECT AGENT =====

    suspend fun createOrGetArchitectAgent(projectId: String): Agent {
        // Check if Architect agent already exists in this project
        val existingAgents = repository.getAgentsByProject(projectId).first()
        val existing = existingAgents.find { it.name == "Architect" }
        if (existing != null) return existing

        val customPrompt = architectConfigRepository.architectSystemPrompt.first()
        val providerStr = architectConfigRepository.architectProvider.first()
        val modelStr = architectConfigRepository.architectModel.first()
        val providerEnum = try {
            AIProvider.valueOf(providerStr.uppercase())
        } catch (e: IllegalArgumentException) {
            AIProvider.OPENROUTER
        }
        // Let repository create the agent (handles UUID internally)
        repository.createAgent(
            projectId = projectId,
            name = "Architect",
            description = "Project Discovery & Planning Agent",
            provider = providerEnum,
            systemPrompt = customPrompt,
            model = modelStr,
            temperature = 0.7f
        )

        // Query back the agent we just created (consistent UUID)
        val agents = repository.getAgentsByProject(projectId).first()
        val architect = agents.find { it.name == "Architect" }
            ?: throw Exception("Architect agent creation failed")

        // Add to in-memory state immediately (agentsJob is async)
        _agents.value = _agents.value + architect
        return architect

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
    // ===== ARCHITECT DISCOVERY PARSING =====

    suspend fun extractAndParseArchitectJSON(
        projectId: String,
        architectMessage: String
    ) {
        val session = _selectedSession.value ?: return
        val start = architectMessage.indexOf('{')
        val end = architectMessage.lastIndexOf('}')

        if (start < 0 || end <= start) {
            val message = "Es konnte kein Projektplan (JSON) in der Antwort gefunden werden. " +
                "Bitte sage dem Architect z.B. 'Mach mir einen Plan!'"
            Log.w("ArchitectJSON", message)
            addArchitectFeedback(session.id, message)
            return
        }

        val jsonString = architectMessage.substring(start..end)
        when (val result = repository.saveProjectScaffold(projectId, jsonString)) {
            is ScaffoldParseResult.Success -> {
                _projectScaffold.value = result.scaffold

                if (_isAutoSummaryEnabled.value) {
                    _showArchitectSummary.value = true
                } else {
                    addArchitectFeedback(
                        session.id,
                        "Projektplan erstellt. Oeffne die Summary oder starte die Discovery neu."
                    )
                }
            }
            is ScaffoldParseResult.Error -> {
                Log.e("ArchitectJSON", result.message, result.cause)
                addArchitectFeedback(
                    session.id,
                    "Der Projektplan konnte nicht verarbeitet werden. Details: ${result.message}"
                )
            }
        }
    }

    private suspend fun addArchitectFeedback(sessionId: String, message: String) {
        val feedback = Message(
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = message
        )
        repository.addMessage(feedback)
        _messages.value = _messages.value + feedback
    }

    fun resetArchitectSummary() {
        _showArchitectSummary.value = false
    }

    fun updateAutoSummaryEnabled(enabled: Boolean) {
        _isAutoSummaryEnabled.value = enabled
    }
    fun createAgentsFromScaffold(projectId: String, scaffold: ProjectScaffold) {
        viewModelScope.launch {
            _isGeneratingFiles.value = true

            try {
                // 1. Project-Daten laden
                val project = repository.getProjectById(projectId) ?: return@launch

                // 2. Projekt-Dateien schreiben
                val fileWriter = ProjectFileWriter(getApplication())
                fileWriter.writeProjectFiles(projectId, project.name, scaffold)

                // 3. Agenten aus suggestedAgents erstellen
                scaffold.suggestedAgents.forEach { agentSpec ->
                    val providerEnum = try {
                        AIProvider.valueOf(agentSpec.provider.uppercase())
                    } catch (e: IllegalArgumentException) {
                        AIProvider.OPENROUTER
                    }

                    repository.createAgent(
                        projectId = projectId,
                        name = agentSpec.name,
                        description = agentSpec.description,
                        provider = providerEnum,
                        systemPrompt = agentSpec.systemPrompt,
                        model = agentSpec.model,
                        temperature = agentSpec.temperature
                    )
                }

                // 4. Agents verladen (selektiertes Projekt)
                selectProject(project)

                Log.d("AgentViewModel", "createAgentsFromScaffold: ${scaffold.suggestedAgents.size} Agenten erstellt")

                _generationComplete.tryEmit(true)
            } catch (e: Exception) {
                Log.e("AgentViewModel", "createAgentsFromScaffold failed", e)
                _generationComplete.tryEmit(false)
            } finally {
                _isGeneratingFiles.value = false
                // 5. Summary schliessen (auch bei Fehler)
                resetArchitectSummary()
            }
        }
    }



    // ===== SESSION MANAGEMENT =====

    fun selectSession(session: ChatSessionEntity?) {
        _selectedSession.value = session
        _messages.value = emptyList()
        _currentError.value = null
        lastSubmittedMessage = null
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
            _currentError.value = null
            lastSubmittedMessage = content

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
                _currentError.value = "Agent nicht gefunden."
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
                _currentError.value = "Kein API-Key fur ${agent.provider.name} konfiguriert."
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
                    _currentError.value = null
                    val assistantMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = result.output,
                        tokenCount = result.tokensUsed
                    )
                    addMessage(assistantMsg)

                    repository.updateAgent(agent.copy(lastRunAt = System.currentTimeMillis()))

                    // Architect Flow: JSON aus Antwort extrahieren, wenn Architect geantwortet hat
                    if (agent.name == "Architect") {
                        extractAndParseArchitectJSON(
                            projectId = session.projectId,
                            architectMessage = result.output
                        )
                    }
                } else {
                    val errorText = result.error ?: "Unbekannter Fehler"
                    Log.e("AgentViewModel", "Chat error: $errorText")
                    val errorMsg = Message(
                        sessionId = session.id,
                        role = MessageRole.ASSISTANT,
                        content = "Fehler: $errorText"
                    )
                    addMessage(errorMsg)
                    _currentError.value = errorText
                }

            } catch (e: Exception) {
                Log.e("AgentViewModel", "sendMessage failed", e)
                val errorText = e.message ?: "Unbekannter Fehler"
                val errorMsg = Message(
                    sessionId = session.id,
                    role = MessageRole.ASSISTANT,
                    content = "Fehler: $errorText"
                )
                addMessage(errorMsg)
                _currentError.value = errorText
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLastMessage() {
        val message = lastSubmittedMessage ?: return
        sendMessage(message)
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

    fun updateArchitectProvider(provider: String) {
        viewModelScope.launch {
            architectConfigRepository.updateArchitectProvider(provider)
        }
    }

    fun updateArchitectModel(model: String) {
        viewModelScope.launch {
            architectConfigRepository.updateArchitectModel(model)
        }
    }

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
