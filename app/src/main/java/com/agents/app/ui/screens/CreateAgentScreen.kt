package com.agents.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agents.app.models.AIProvider
import com.agents.app.models.AgentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAgentScreen(
    currentApiKey: String,
    currentOllamaUrl: String,
    onNavigateBack: () -> Unit,
    onCreateAgent: (
        name: String,
        description: String,
        type: AgentType,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) -> Unit,
    onUpdateApiKey: (String) -> Unit,
    onUpdateOllamaUrl: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("You are a helpful AI assistant.") }
    var model by remember { mutableStateOf("gpt-4") }
    var temperature by remember { mutableStateOf(0.7f) }
    var selectedType by remember { mutableStateOf(AgentType.GENERAL) }
    var selectedProvider by remember { mutableStateOf(AIProvider.OPENROUTER) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var ollamaUrl by remember { mutableStateOf(currentOllamaUrl) }
    var showApiKey by remember { mutableStateOf(false) }

    val agentTypes = AgentType.entries
    val aiProviders = AIProvider.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Agent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Agent Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Agent Type Dropdown
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Agent Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    agentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            // Provider Dropdown
            var providerExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = !providerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("AI Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    aiProviders.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name) },
                            onClick = {
                                selectedProvider = provider
                                providerExpanded = false
                                // Set default model based on provider
                                model = when (provider) {
                                    AIProvider.OPENROUTER -> "openai/gpt-4o"
                                    AIProvider.OLLAMA -> "llama2"
                                    AIProvider.ZEN -> "big-pickle"
                                }
                            }
                        )
                    }
                }
            }

            // API Key (shown for OpenAI/Anthropic/Zen)
            if (selectedProvider != AIProvider.OLLAMA) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showApiKey) "Hide" else "Show"
                            )
                        }
                    }
                )
            }

            // Ollama URL (shown for Ollama)
            if (selectedProvider == AIProvider.OLLAMA) {
                OutlinedTextField(
                    value = ollamaUrl,
                    onValueChange = { ollamaUrl = it },
                    label = { Text("Ollama Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("http://localhost:11434") }
                )
            }

            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth()
            )

            // Temperature Slider
            Text(
                text = "Temperature: %.2f".format(temperature),
                style = MaterialTheme.typography.bodyMedium
            )
            Slider(
                value = temperature,
                onValueChange = { temperature = it },
                valueRange = 0f..1f,
                steps = 20,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    onUpdateApiKey(apiKey)
                    onUpdateOllamaUrl(ollamaUrl)
                    onCreateAgent(
                        name,
                        description,
                        selectedType,
                        selectedProvider,
                        systemPrompt,
                        model,
                        temperature
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Create Agent")
            }
        }
    }
}
