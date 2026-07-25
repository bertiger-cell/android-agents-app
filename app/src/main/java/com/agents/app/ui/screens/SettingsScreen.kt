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
import com.agents.app.data.ProviderCredentials

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    credentials: ProviderCredentials,
    ollamaTestMessage: String?,
    isOllamaTesting: Boolean,
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

    LaunchedEffect(credentials) {
        openRouterKey = credentials.openRouterKey
        zenKey = credentials.zenKey
        ollamaBaseUrl = credentials.ollamaBaseUrl
        ollamaApiKey = credentials.ollamaApiKey
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
