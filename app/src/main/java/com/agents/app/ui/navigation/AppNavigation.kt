package com.agents.app.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.agents.app.models.Agent
import com.agents.app.ui.AgentViewModel
import com.agents.app.ui.screens.*

@Composable
fun AppNavigation(viewModel: AgentViewModel = viewModel()) {
    val navController = rememberNavController()
    val agents by viewModel.agents.collectAsState()
    val selectedAgent by viewModel.selectedAgent.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val ollamaUrl by viewModel.ollamaUrl.collectAsState()

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
                }
            )
        }

        // Create Agent Screen
        composable("create") {
            CreateAgentScreen(
                currentApiKey = apiKey,
                currentOllamaUrl = ollamaUrl,
                onNavigateBack = { navController.popBackStack() },
                onCreateAgent = { name, description, type, provider, systemPrompt, model, temperature ->
                    viewModel.createAgent(
                        name = name,
                        description = description,
                        type = type,
                        provider = provider,
                        systemPrompt = systemPrompt,
                        model = model,
                        temperature = temperature
                    )
                },
                onUpdateApiKey = { viewModel.updateApiKey(it) },
                onUpdateOllamaUrl = { viewModel.updateOllamaUrl(it) }
            )
        }

        // Chat Screen
        composable("chat") {
            selectedAgent?.let { agent ->
                ChatScreen(
                    agent = agent,
                    messages = messages,
                    isLoading = isLoading,
                    apiKeyAvailable = apiKey.isNotBlank(),
                    onSendMessage = { message ->
                        viewModel.sendMessage(message)
                    },
                    onNavigateBack = {
                        viewModel.selectAgent(null)
                        navController.popBackStack()
                    },
                    onSettings = {
                        navController.navigate("settings")
                    }
                )
            }
        }

        // Settings Screen
        composable("settings") {
            SettingsScreen(
                apiKey = apiKey,
                ollamaUrl = ollamaUrl,
                onUpdateApiKey = { viewModel.updateApiKey(it) },
                onUpdateOllamaUrl = { viewModel.updateOllamaUrl(it) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
