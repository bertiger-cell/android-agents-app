package com.agents.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agents.app.data.ArchitectConfigRepository
import com.agents.app.data.ProviderCredentials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentials: ProviderCredentials,
    ollamaTestMessage: String?,
    isOllamaTesting: Boolean,
    architectSystemPrompt: String,
    onUpdateArchitectSystemPrompt: (String) -> Unit,
    architectProvider: String,
    onUpdateArchitectProvider: (String) -> Unit,
    architectModel: String,
    onUpdateArchitectModel: (String) -> Unit,
    onUpdateOpenRouterKey: (String) -> Unit,
    onUpdateZenKey: (String) -> Unit,
    onUpdateOllamaBaseUrl: (String) -> Unit,
    onUpdateOllamaApiKey: (String) -> Unit,
    onTestOllamaConnection: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showZenKey by remember { mutableStateOf(false) }
    var showOllamaKey by remember { mutableStateOf(false) }
    var openRouterKey by remember { mutableStateOf(credentials.openRouterKey) }
    var zenKey by remember { mutableStateOf(credentials.zenKey) }
    var ollamaBaseUrl by remember { mutableStateOf(credentials.ollamaBaseUrl) }
    var ollamaApiKey by remember { mutableStateOf(credentials.ollamaApiKey) }
    var isArchitectExpanded by remember { mutableStateOf(false) }
    var editedArchitectPrompt by remember { mutableStateOf(architectSystemPrompt) }
    var editedArchitectProvider by remember { mutableStateOf(architectProvider) }
    var editedArchitectModel by remember { mutableStateOf(architectModel) }
    val context = LocalContext.current
    val architectConfigRepository = remember(context) { ArchitectConfigRepository(context) }
    val isArchitectEnabled by architectConfigRepository.isArchitectEnabled.collectAsState(initial = false)

    LaunchedEffect(credentials) {
        openRouterKey = credentials.openRouterKey
        zenKey = credentials.zenKey
        ollamaBaseUrl = credentials.ollamaBaseUrl
        ollamaApiKey = credentials.ollamaApiKey
    }

    LaunchedEffect(architectSystemPrompt) {
        editedArchitectPrompt = architectSystemPrompt
    }

    LaunchedEffect(architectProvider) {
        editedArchitectProvider = architectProvider
    }

    LaunchedEffect(architectModel) {
        editedArchitectModel = architectModel
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("AI Provider Settings", style = MaterialTheme.typography.headlineSmall)
            Divider()

            Text("OpenRouter", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = openRouterKey,
                onValueChange = { openRouterKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                        Icon(if (showOpenRouterKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                    }
                }
            )
            Divider()

            Text("OpenCode Zen", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = zenKey,
                onValueChange = { zenKey = it },
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showZenKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showZenKey = !showZenKey }) {
                        Icon(if (showZenKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                    }
                }
            )
            Divider()

            Text("Ollama (Local & Cloud)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = ollamaBaseUrl,
                onValueChange = { ollamaBaseUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://ollama.com") }
            )
            OutlinedTextField(
                value = ollamaApiKey,
                onValueChange = { ollamaApiKey = it },
                label = { Text("API Key (fuer Cloud erforderlich)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showOllamaKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showOllamaKey = !showOllamaKey }) {
                        Icon(if (showOllamaKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onTestOllamaConnection(ollamaBaseUrl, ollamaApiKey)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isOllamaTesting
                ) {
                    Text(if (isOllamaTesting) "Test..." else "Test Connection")
                }
                if (ollamaTestMessage != null) {
                    Text(
                        text = ollamaTestMessage.orEmpty(),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Text(
                text = "Ollama Cloud: https://ollama.com + API Key erforderlich. Lokal: http://127.0.0.1:11434",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider()

            // ===== ARCHITECT SETTINGS =====
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Architect Discovery",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Auto-generate project structure and agents.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isArchitectEnabled,
                            onCheckedChange = { enabled ->
                                architectConfigRepository.setArchitectEnabled(enabled)
                            }
                        )
                    }

                    if (isArchitectEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Enabled projects start with the Architect interview and generate the scaffold files automatically.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isArchitectExpanded = !isArchitectExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Architect Agent System Prompt",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            if (isArchitectExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isArchitectExpanded) {
                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Provider Dropdown
                        Text(
                            "Provider",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        var providerExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = providerExpanded,
                            onExpandedChange = { providerExpanded = !providerExpanded }
                        ) {
                            OutlinedTextField(
                                value = editedArchitectProvider,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = providerExpanded,
                                onDismissRequest = { providerExpanded = false }
                            ) {
                                listOf("openrouter", "zen", "ollama").forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.replaceFirstChar { it.uppercase() }) },
                                        onClick = {
                                            editedArchitectProvider = provider
                                            providerExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Model Text Field
                        OutlinedTextField(
                            value = editedArchitectModel,
                            onValueChange = { editedArchitectModel = it },
                            label = { Text("Model") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("z.B. gpt-4o, claude-3-opus, ...") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = editedArchitectPrompt,
                            onValueChange = { editedArchitectPrompt = it },
                            label = { Text("System Prompt") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp),
                            minLines = 8,
                            maxLines = 20
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onUpdateArchitectSystemPrompt(editedArchitectPrompt)
                                    onUpdateArchitectProvider(editedArchitectProvider)
                                    onUpdateArchitectModel(editedArchitectModel)
                                    isArchitectExpanded = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Speichern")
                            }

                            OutlinedButton(
                                onClick = {
                                    editedArchitectPrompt = architectSystemPrompt
                                    editedArchitectProvider = architectProvider
                                    editedArchitectModel = architectModel
                                    isArchitectExpanded = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Abbrechen")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Der Architect-Prompt bestimmt die Qualitaet der " +
                            "Projekt-Discovery. Experimentiere, um bessere Plaene zu bekommen.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider()

            Button(
                onClick = {
                    onUpdateOpenRouterKey(openRouterKey)
                    onUpdateZenKey(zenKey)
                    onUpdateOllamaBaseUrl(ollamaBaseUrl)
                    onUpdateOllamaApiKey(ollamaApiKey)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }
        }
    }
}
