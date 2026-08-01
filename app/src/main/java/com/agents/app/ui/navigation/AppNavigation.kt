package com.agents.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.agents.app.models.ProjectEntity
import com.agents.app.models.AgentSpec
import com.agents.app.models.ChatSessionEntity
import com.agents.app.ui.AgentViewModel
import com.agents.app.feature.agents.ArchitectSummaryScreen
import com.agents.app.feature.agents.CreateAgentScreen
import com.agents.app.feature.chat.ChatScreen
import com.agents.app.feature.projects.ProjectDetailWithTabsScreen
import com.agents.app.feature.projects.ProjectListScreen
import com.agents.app.feature.settings.SettingsScreen
import android.widget.Toast
import kotlinx.coroutines.launch

enum class TopLevelDestination {
    PROJECTS,
    CHAT,
    SETTINGS
}

internal fun resolveTopLevelDestination(
    currentRoute: String?,
    hasSelectedSession: Boolean
): TopLevelDestination {
    return when {
        currentRoute == "settings" -> TopLevelDestination.SETTINGS
        hasSelectedSession -> TopLevelDestination.CHAT
        else -> TopLevelDestination.PROJECTS
    }
}

internal fun shouldShowChatOverlay(
    currentRoute: String?,
    selectedSession: ChatSessionEntity?
): Boolean = selectedSession != null && currentRoute != "settings"

@Composable
fun AppNavigation(viewModel: AgentViewModel = viewModel()) {
    val navController = rememberNavController()
    val navScope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // V3 states
    val projects by viewModel.projects.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val agents by viewModel.agents.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val ollamaTestMessage by viewModel.ollamaTestMessage.collectAsState()
    val isOllamaTesting by viewModel.isOllamaTesting.collectAsState()
    val projectScaffold by viewModel.projectScaffold.collectAsState()
    val showArchitectSummary by viewModel.showArchitectSummary.collectAsState()
    val isGeneratingFiles by viewModel.isGeneratingFiles.collectAsState()
    val isTransferring by viewModel.isTransferring.collectAsState()
    val transferMessage by viewModel.transferMessage.collectAsState()
    val agentTemplates by viewModel.agentTemplates.collectAsState()
    val pendingAttachments by viewModel.pendingAttachments.collectAsState()
    val attachmentsByMessage by viewModel.attachmentsByMessage.collectAsState()
    val uiError by viewModel.uiError.collectAsState()
    val topLevelDestination = resolveTopLevelDestination(currentRoute, selectedSession != null)

    // Generation-Complete Event: Toast bei Erfolg/Fehler
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.generationComplete.collect { success ->
            if (success) {
                Toast.makeText(context, "Projekt-Dateien erstellt und Agenten angelegt", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Fehler bei der Generierung. Bitte Logs pruefen.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val availableOllamaModels by viewModel.availableOllamaModels.collectAsState()
    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsState()
    val availableZenModels by viewModel.availableZenModels.collectAsState()
    val architectSystemPrompt by viewModel.architectSystemPrompt.collectAsState()
    val architectProvider by viewModel.architectProvider.collectAsState()
    val architectModel by viewModel.architectModel.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = topLevelDestination == TopLevelDestination.PROJECTS,
                        onClick = {
                            viewModel.selectSession(null)
                            navController.navigate("projects") {
                                popUpTo("projects") { inclusive = false }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) },
                        label = { Text("Projects") }
                    )
                    NavigationBarItem(
                        selected = topLevelDestination == TopLevelDestination.CHAT,
                        onClick = {
                            val fallbackSession = selectedSession ?: sessions.values.flatten().lastOrNull()
                            if (currentRoute == "settings") {
                                navController.popBackStack()
                            }
                            if (fallbackSession != null && selectedSession?.id != fallbackSession.id) {
                                viewModel.selectSession(fallbackSession)
                            }
                        },
                        icon = { Icon(Icons.Filled.ChatBubble, contentDescription = null) },
                        label = { Text("Chat") }
                    )
                    NavigationBarItem(
                        selected = topLevelDestination == TopLevelDestination.SETTINGS,
                        onClick = {
                            navController.navigate("settings") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "projects",
                enterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 4 } },
                exitTransition = { fadeOut(tween(300)) },
                popEnterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 4 } },
                popExitTransition = { fadeOut(tween(300)) },
                modifier = Modifier.padding(padding)
            ) {
                // ===== 1. Projects =====
                composable("projects") {
                    ProjectListScreen(
                        projects = projects,
                        onProjectSelected = { project ->
                            viewModel.selectProject(project)
                            navController.navigate("project/${project.id}")
                        },
                        onCreateProjectWithArchitect = { name, description ->
                            navScope.launch {
                                val project = viewModel.createProject(name, description)
                                val session = viewModel.createArchitectDiscoverySession(project.id)
                                viewModel.selectProject(
                                    ProjectEntity(
                                        id = project.id,
                                        name = project.name,
                                        description = project.description,
                                        createdAt = project.createdAt,
                                        updatedAt = project.updatedAt,
                                        folderPath = project.folderPath
                                    )
                                )
                                viewModel.selectSession(session)
                            }
                        },
                        onCreateProjectNormal = { name, description ->
                            navScope.launch {
                                val project = viewModel.createProject(name, description)
                                viewModel.selectProject(
                                    ProjectEntity(
                                        id = project.id,
                                        name = project.name,
                                        description = project.description,
                                        createdAt = project.createdAt,
                                        updatedAt = project.updatedAt,
                                        folderPath = project.folderPath
                                    )
                                )
                                viewModel.selectSession(null)
                                navController.navigate("project/${project.id}")
                            }
                        },
                        onDeleteProject = { project ->
                            viewModel.deleteProject(project.id)
                        },
                        onSettings = {
                            navController.navigate("settings")
                        }
                    )
                }

                // ===== 2. Project Detail with Tabs =====
                composable(
                    route = "project/{projectId}",
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable

                    val project = selectedProject ?: return@composable

                    ProjectDetailWithTabsScreen(
                        project = project,
                        agents = agents,
                        sessions = sessions,
                        onCreateAgent = {
                            navController.navigate("create-agent/${projectId}")
                        },
                        onSessionSelected = { session ->
                            viewModel.selectSession(session)
                        },
                        onNewChat = { agent ->
                            viewModel.startChat(agent.id)
                        },
                        onDeleteAgent = { agent ->
                            viewModel.deleteAgent(agent)
                        },
                        onDeleteSession = { session ->
                            viewModel.deleteSession(session)
                        },
                        onRestartArchitectDiscovery = {
                            viewModel.restartArchitectDiscovery(projectId)
                        },
                        onUpdateProject = { id, name, description ->
                            viewModel.updateProject(id, name, description)
                        },
                        onExportProject = { uri ->
                            viewModel.exportProject(project, uri)
                        },
                        onImportProject = { uri ->
                            viewModel.importProject(uri) { imported ->
                                viewModel.selectProject(imported)
                                navController.navigate("project/${imported.id}") {
                                    popUpTo("projects") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        isTransferring = isTransferring,
                        transferMessage = transferMessage,
                        onTransferMessageShown = {
                            viewModel.clearTransferMessage()
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }

                // ===== 3. Create Agent =====
                composable(
                    route = "create-agent/{projectId}",
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable

                    CreateAgentScreen(
                        availableOllamaModels = availableOllamaModels,
                        availableOpenRouterModels = availableOpenRouterModels,
                        availableZenModels = availableZenModels,
                        onFetchOllamaModels = { baseUrl, apiKey ->
                            viewModel.fetchAvailableOllamaModels(baseUrl, apiKey)
                        },
                        onFetchOpenRouterModels = { apiKey ->
                            viewModel.fetchAvailableOpenRouterModels(apiKey)
                        },
                        onFetchZenModels = { apiKey ->
                            viewModel.fetchAvailableZenModels(apiKey)
                        },
                        ollamaBaseUrl = credentials.ollamaBaseUrl,
                        ollamaApiKey = credentials.ollamaApiKey,
                        openRouterApiKey = credentials.openRouterKey,
                        zenApiKey = credentials.zenKey,
                        templates = agentTemplates,
                        onSaveTemplate = { template ->
                            viewModel.saveAgentTemplate(template)
                        },
                        onDeleteTemplate = { template ->
                            viewModel.deleteAgentTemplate(template)
                        },
                        projectId = projectId,
                        onNavigateBack = { navController.popBackStack() },
                        onCreateAgent = { name, description, provider, systemPrompt, model, temperature ->
                            viewModel.createAgent(
                                projectId = projectId,
                                name = name,
                                description = description,
                                provider = provider,
                                systemPrompt = systemPrompt,
                                model = model,
                                temperature = temperature
                            )
                            navController.popBackStack()
                        }
                    )
                }

                // ===== 4. Settings =====
                composable("settings") {
                    SettingsScreen(
                        credentials = credentials,
                        ollamaTestMessage = ollamaTestMessage,
                        isOllamaTesting = isOllamaTesting,
                        architectSystemPrompt = architectSystemPrompt,
                        onUpdateArchitectSystemPrompt = { viewModel.updateArchitectSystemPrompt(it) },
                        architectProvider = architectProvider,
                        onUpdateArchitectProvider = { viewModel.updateArchitectProvider(it) },
                        architectModel = architectModel,
                        onUpdateArchitectModel = { viewModel.updateArchitectModel(it) },
                        onUpdateOpenRouterKey = { viewModel.updateOpenRouterKey(it) },
                        onUpdateZenKey = { viewModel.updateZenKey(it) },
                        onUpdateOllamaBaseUrl = { viewModel.updateOllamaBaseUrl(it) },
                        onUpdateOllamaApiKey = { viewModel.updateOllamaApiKey(it) },
                        onTestOllamaConnection = { baseUrl, apiKey ->
                            viewModel.testOllamaConnection(baseUrl, apiKey)
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // ===== Chat Modal (fullscreen overlay) =====
        if (shouldShowChatOverlay(currentRoute, selectedSession) && selectedProject != null) {
            BackHandler {
                viewModel.selectSession(null)
            }
            val sessionAgent = agents.find { it.id == selectedSession!!.agentId }

            if (sessionAgent != null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        session = selectedSession!!,
                        agent = sessionAgent,
                        messages = messages,
                        isLoading = isLoading,
                        pendingAttachments = pendingAttachments,
                        attachmentsByMessage = attachmentsByMessage,
                        onAttachmentPicked = { uri ->
                            viewModel.addPendingAttachment(uri)
                        },
                        onRemovePendingAttachment = { attachment ->
                            viewModel.removePendingAttachment(attachment)
                        },
                        uiError = uiError,
                        onClearUiError = {
                            viewModel.clearUiError()
                        },
                        onSendMessage = { content, attachments ->
                            viewModel.sendMessage(content, attachments)
                        },
                        onNavigateBack = {
                            viewModel.selectSession(null)
                        },
                        onSettings = {
                            navController.navigate("settings")
                        },
                        onRenameSession = { newTitle ->
                            viewModel.renameSession(selectedSession!!.id, newTitle)
                        }
                    )
                }
            }
        }

        // ===== Loading Overlay =====
        if (isGeneratingFiles) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.padding(16.dp))
                    Text(
                        "Erstelle Projekt-Dateien...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // ===== ARCHITECT SUMMARY MODAL =====
        if (showArchitectSummary && projectScaffold != null) {
            ArchitectSummaryScreen(
                scaffold = projectScaffold!!,
                onConfirm = { updatedAgents ->
                    val updatedScaffold = projectScaffold!!.copy(
                        suggestedAgents = updatedAgents.map { edited ->
                            AgentSpec(
                                name = edited.name,
                                description = edited.description,
                                systemPrompt = edited.systemPrompt,
                                provider = edited.provider,
                                model = edited.model,
                                temperature = edited.temperature
                            )
                        }
                    )

                    // Task 8e/8f triggern
                    viewModel.createAgentsFromScaffold(
                        projectId = selectedProject!!.id,
                        scaffold = updatedScaffold
                    )

                    // Summary schließen
                    viewModel.resetArchitectSummary()
                },
                onDismiss = {
                    viewModel.resetArchitectSummary()
                }
            )
        }
    }
}
