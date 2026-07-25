package com.agents.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agents.app.models.AIProvider
import com.agents.app.models.AgentType
import com.agents.app.models.OllamaModel
import com.agents.app.models.OpenAIModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAgentScreen(
    availableOllamaModels: List<OllamaModel> = emptyList(),
    availableOpenRouterModels: List<OpenAIModel> = emptyList(),
    availableZenModels: List<OpenAIModel> = emptyList(),
    onFetchOllamaModels: (String, String) -> Unit = { _, _ -> },
    onFetchOpenRouterModels: (String) -> Unit = { _ -> },
    onFetchZenModels: (String) -> Unit = { _ -> },
    ollamaBaseUrl: String = "",
    ollamaApiKey: String = "",
    openRouterApiKey: String = "",
    zenApiKey: String = "",
    onNavigateBack: () -> Unit,
    onCreateAgent: (
        name: String,
        description: String,
        type: AgentType,
        provider: AIProvider,
        systemPrompt: String,
        model: String,
        temperature: Float
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("You are a helpful AI assistant.") }
    var model by remember { mutableStateOf("gpt-4") }
    var temperature by remember { mutableStateOf(0.7f) }
    var selectedType by remember { mutableStateOf(AgentType.GENERAL) }
    var selectedProvider by remember { mutableStateOf(AIProvider.OPENROUTER) }

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
                                model = when (provider) {
                                    AIProvider.OPENROUTER -> availableOpenRouterModels.firstOrNull()?.id ?: ""
                                    AIProvider.OLLAMA -> availableOllamaModels.firstOrNull()?.name ?: ""
                                    AIProvider.ZEN -> availableZenModels.firstOrNull()?.id ?: ""
                                }
                            }
                        )
                    }
                }
            }

            // Model selection per provider
            when (selectedProvider) {
                AIProvider.OLLAMA -> OllamaModelPicker(
                    model = model,
                    onModelChange = { model = it },
                    availableModels = availableOllamaModels,
                    onFetchModels = {
                        if (ollamaBaseUrl.isNotBlank()) {
                            onFetchOllamaModels(ollamaBaseUrl, ollamaApiKey)
                        }
                    }
                )
                AIProvider.OPENROUTER -> OpenAICompatibleModelPicker(
                    model = model,
                    onModelChange = { model = it },
                    availableModels = availableOpenRouterModels,
                    onFetchModels = {
                        if (openRouterApiKey.isNotBlank()) {
                            onFetchOpenRouterModels(openRouterApiKey)
                        }
                    },
                    providerName = "OpenRouter"
                )
                AIProvider.ZEN -> OpenAICompatibleModelPicker(
                    model = model,
                    onModelChange = { model = it },
                    availableModels = availableZenModels,
                    onFetchModels = {
                        if (zenApiKey.isNotBlank()) {
                            onFetchZenModels(zenApiKey)
                        }
                    },
                    providerName = "Zen"
                )
            }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OllamaModelPicker(
    model: String,
    onModelChange: (String) -> Unit,
    availableModels: List<OllamaModel>,
    onFetchModels: () -> Unit
) {
    LaunchedEffect(Unit) { onFetchModels() }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = model,
            onValueChange = { onModelChange(it) },
            label = { Text("Model") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            placeholder = { Text("Select or type model name") }
        )
        if (availableModels.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModels.forEach { ollamaModel ->
                    val displayName = ollamaModel.name ?: ollamaModel.model ?: ""
                    val detail = listOfNotNull(
                        ollamaModel.details?.parameter_size,
                        ollamaModel.details?.quantization_level
                    ).joinToString(" ")
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(displayName)
                                if (detail.isNotBlank()) {
                                    Text(
                                        detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onModelChange(displayName)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenAICompatibleModelPicker(
    model: String,
    onModelChange: (String) -> Unit,
    availableModels: List<OpenAIModel>,
    onFetchModels: () -> Unit,
    providerName: String
) {
    LaunchedEffect(Unit) { onFetchModels() }

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = model,
            onValueChange = { onModelChange(it) },
            label = { Text("Model") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            placeholder = { Text("Select or type $providerName model") }
        )
        if (availableModels.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableModels.forEach { openAIModel ->
                    val modelId = openAIModel.id ?: ""
                    val displayName = openAIModel.name ?: modelId
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(displayName)
                                if (!openAIModel.description.isNullOrBlank()) {
                                    Text(
                                        openAIModel.description?.take(80) ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onModelChange(modelId)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
