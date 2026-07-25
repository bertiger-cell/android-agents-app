package com.agents.app.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
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

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                agents = agents,
                onSelectAgent = { agent ->
                    viewModel.selectAgent(agent)
                    navController.navigate("chat")
                },
                onShowAllAgents = {
                    navController.navigate("agents")
                },
                onSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("agents") {
            AgentListScreen(
                agents = agents,
                onSelectAgent = { agent ->
                    viewModel.selectAgent(agent)
                    navController.navigate("chat")
                },
                onDeleteAgent = { agent ->
                    viewModel.deleteAgent(agent)
                },
                onCreateAgent = {
                    navController.navigate("create")
                },
                onSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("create") {
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
                onCreateAgent = { name, description, type, provider, systemPrompt, model, temperature ->
                    viewModel.createAgent(name, description, type, provider, systemPrompt, model, temperature)
                }
            )
        }

        composable("chat") {
            selectedAgent?.let { agent ->
                ChatScreen(
                    agent = agent,
                    messages = messages,
                    isLoading = isLoading,
                    credentialsAvailable = when (agent.provider) {
                        AIProvider.OPENROUTER -> credentials.openRouterKey.isNotBlank()
                        AIProvider.ZEN -> credentials.zenKey.isNotBlank()
                        AIProvider.OLLAMA -> credentials.ollamaBaseUrl.isNotBlank()
                    },
                    onSendMessage = { message ->
                        viewModel.sendMessage(message)
                    },
                    onNavigateBack = {
                        viewModel.selectAgent(null)
                        navController.popBackStack()
                    }
                )
            }
        }

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
