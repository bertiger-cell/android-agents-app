package com.agents.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.agents.app.ui.AgentViewModel
import com.agents.app.ui.screens.*

@Composable
fun AppNavigation(viewModel: AgentViewModel = viewModel()) {
    val navController = rememberNavController()

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
    val availableOllamaModels by viewModel.availableOllamaModels.collectAsState()
    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsState()
    val availableZenModels by viewModel.availableZenModels.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = navController,
        startDestination = "projects",
        enterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 4 } },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(400)) + slideInHorizontally(tween(400)) { -it / 4 } },
        popExitTransition = { fadeOut(tween(300)) }
    ) {
        // ===== 1. Projects =====
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

            ProjectDetailWithTabsScreen(
                project = selectedProject ?: return@composable,
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

    // ===== Chat Modal (fullscreen overlay) =====
    if (selectedSession != null && selectedProject != null) {
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
    }  // Box close
}
