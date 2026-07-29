package com.agents.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.agents.app.models.AIProvider
import com.agents.app.ui.AgentViewModel
import com.agents.app.ui.screens.*

@Composable
fun AppNavigation(viewModel: AgentViewModel = viewModel()) {
    val navController = rememberNavController()

    // ViewModel states
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
    val availableOllamaModels by viewModel.availableOllamaModels.collectAsState()
    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsState()
    val availableZenModels by viewModel.availableZenModels.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "projects"
        ) {
            // ===== 1. Project List (Startseite) =====
            composable("projects") {
                ProjectListScreen(
                    projects = projects,
                    onProjectSelected = { project ->
                        viewModel.selectProject(project)
                        navController.navigate("project/${project.id}")
                    },
                    onCreateProject = { name, description ->
                        viewModel.createProject(name, description)
                    },
                    onDeleteProject = { project ->
                        viewModel.deleteProject(project)
                    },
                    onSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            // ===== 2. Project Detail mit Tabs =====
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
                    onStartChat = { agent ->
                        viewModel.startChat(agent.id)
                    },
                    onSessionSelected = { session ->
                        viewModel.selectSession(session)
                    },
                    onNavigateBack = {
                        viewModel.selectProject(null)
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
                    projectId = projectId,
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

        // ===== Chat Overlay (fullscreen) =====
        val session = selectedSession
        if (session != null) {
            BackHandler {
                viewModel.selectSession(null)
            }
            val sessionAgent = agents.find { it.id == session.agentId }

            if (sessionAgent != null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        agent = sessionAgent,
                        session = session,
                        messages = messages,
                        isLoading = isLoading,
                        credentialsAvailable = when (sessionAgent.provider) {
                            AIProvider.OPENROUTER -> credentials.openRouterKey.isNotBlank()
                            AIProvider.ZEN -> credentials.zenKey.isNotBlank()
                            AIProvider.OLLAMA -> credentials.ollamaBaseUrl.isNotBlank()
                        },
                        onSendMessage = { content ->
                            viewModel.sendMessage(content)
                        },
                        onNavigateBack = {
                            viewModel.selectSession(null)
                        },
                        onSettings = {
                            navController.navigate("settings")
                        }
                    )
                }
            }
        }
    }
}
