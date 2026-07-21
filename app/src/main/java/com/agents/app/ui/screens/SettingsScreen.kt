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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    apiKey: String,
    ollamaUrl: String,
    onUpdateApiKey: (String) -> Unit,
    onUpdateOllamaUrl: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKey by remember { mutableStateOf(false) }
    var localApiKey by remember { mutableStateOf(apiKey) }
    var localOllamaUrl by remember { mutableStateOf(ollamaUrl) }

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
            Text(
                text = "AI Provider Settings",
                style = MaterialTheme.typography.headlineSmall
            )

            Divider()

            // API Key Section
            Text(
                text = "OpenAI / Anthropic API Key",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = localApiKey,
                onValueChange = { localApiKey = it },
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

            Divider()

            // Ollama Section
            Text(
                text = "Ollama (Local LLM)",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = localOllamaUrl,
                onValueChange = { localOllamaUrl = it },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("http://localhost:11434") }
            )

            Divider()

            Button(
                onClick = {
                    onUpdateApiKey(localApiKey)
                    onUpdateOllamaUrl(localOllamaUrl)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            // Info Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Supported Providers",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "• OpenAI: GPT-4, GPT-3.5-turbo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Anthropic: Claude 3 Opus, Sonnet",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Ollama: Any locally hosted model",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• OpenCode: Local AI coding agent (port 4096)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
