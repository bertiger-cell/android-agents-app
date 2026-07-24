package com.agents.app.ui.navigation

import androidx.compose.runtime.*
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
    val agents by viewModel.agents.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val credentials by viewModel.credentials.collectAsState()
    val ollamaModels = remember(agents) {
        agents
            .asSequence()
            .filter { it.provider == AIProvider.OLLAMA }
            .map { it.model }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    NavHost(navController = navController, startDestination = "agents") {
        // Agent List Screen
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

        // Create Agent Screen
        composable("create") {
            CreateAgentScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreateAgent = { name, description, type, provider, systemPrompt, model, temperature ->
                    viewModel.createAgent(name, description, type, provider, systemPrompt, model, temperature)
                }
            )
        }

        // Chat Screen
        composable("chat") {
            selectedAgent?.let { agent ->
                ChatScreen(
                    agent = agent,
                    messages = messages,
                    isLoading = isLoading,
                    apiKeyAvailable = when (agent.provider) {
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

        // Settings Screen
        composable("settings") {
            SettingsScreen(
                credentials = credentials,
                ollamaModels = ollamaModels,
                onUpdateOpenRouterKey = { viewModel.updateOpenRouterKey(it) },
                onUpdateZenKey = { viewModel.updateZenKey(it) },
                onUpdateOllamaBaseUrl = { viewModel.updateOllamaBaseUrl(it) },
                onUpdateOllamaApiKey = { viewModel.updateOllamaApiKey(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
