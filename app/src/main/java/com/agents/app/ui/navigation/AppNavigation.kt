package com.agents.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.agents.app.models.AIProvider
import com.agents.app.ui.AgentViewModel
import com.agents.app.ui.screens.*

@Composable
fun AppNavigation(viewModel: AgentViewModel = viewModel()) {
    val navController = rememberNavController()
    val agents by viewModel.agents.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val ollamaTestMessage by viewModel.ollamaTestMessage.collectAsState()
    val isOllamaTesting by viewModel.isOllamaTesting.collectAsState()
    val availableOllamaModels by viewModel.availableOllamaModels.collectAsState()
    val availableOpenRouterModels by viewModel.availableOpenRouterModels.collectAsState()
    val availableZenModels by viewModel.availableZenModels.collectAsState()

    // V3 state (wird in Task 5b verdrahtet)
    var currentProject by remember { mutableStateOf<com.agents.app.models.ProjectEntity?>(null) }

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
                projects = emptyList(),
                onProjectSelected = { project ->
                    currentProject = project
                    navController.navigate("project/${project.id}")
                },
                onCreateProject = {
                    // Task 5b
                },
                onDeleteProject = { project ->
                    // Task 5b
                }
            )
        }

        // ===== 2. Project Detail with Tabs =====
        composable(
            route = "project/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val project = currentProject ?: return@composable

            ProjectDetailWithTabsScreen(
                project = project,
                agents = agents,
                sessions = emptyMap(),
                onCreateAgent = {
                    navController.navigate("create-agent/${projectId}")
                },
                onSessionSelected = { session ->
                    // Task 6: Chat modal
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ===== 3. Create Agent (in Projekt) =====
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
                onNavigateBack = { navController.popBackStack() },
                onCreateAgent = { name, description, provider, systemPrompt, model, temperature ->
                    viewModel.createAgent(name, description, provider, systemPrompt, model, temperature)
                    navController.popBackStack()
                }
            )
        }

        // ===== 4. Settings (global) =====
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
}
