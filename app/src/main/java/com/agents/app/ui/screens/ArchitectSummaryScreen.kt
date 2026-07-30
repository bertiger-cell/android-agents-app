package com.agents.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agents.app.models.AgentSpec
import com.agents.app.models.ProjectScaffold

data class AgentSpecWithEditedPrompt(
    val name: String,
    val description: String,
    val systemPrompt: String,
    val provider: String = "openrouter",
    val model: String = "gpt-4o",
    val temperature: Float = 0.7f
) {
    constructor(spec: AgentSpec) : this(
        name = spec.name,
        description = spec.description,
        systemPrompt = spec.systemPrompt,
        provider = spec.provider,
        model = spec.model,
        temperature = spec.temperature
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitectSummaryScreen(
    scaffold: ProjectScaffold,
    onConfirm: (updatedAgents: List<AgentSpecWithEditedPrompt>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingAgentIndex by remember { mutableStateOf<Int?>(null) }
    var editedPrompt by remember { mutableStateOf("") }

    // Track edited prompts
    val editedAgents = remember {
        scaffold.suggestedAgents.toMutableList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
            ) {
                // ===== TOP BAR =====
                TopAppBar(
                    title = { Text("Projekt-Plan") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Schliessen")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )

                // ===== CONTENT =====
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ===== DISCOVERY CONTEXT =====
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Projekt-Kontext",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Domain: ${scaffold.discoveryContext.domain}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Technologien: ${scaffold.discoveryContext.technologies.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Level: ${scaffold.discoveryContext.experienceLevel}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Groesse: ${scaffold.discoveryContext.projectSize}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (scaffold.discoveryContext.concerns.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Concerns: ${scaffold.discoveryContext.concerns.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // ===== PHASES =====
                    item {
                        Text(
                            "Roadmap (${scaffold.phases.size} Phasen)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(scaffold.phases) { phase ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "${phase.phaseNumber}. ${phase.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    phase.description,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Duration: ${phase.duration}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        "Focus: ${phase.focus}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // ===== RULES =====
                    if (scaffold.rules.isNotEmpty()) {
                        item {
                            Text(
                                "Regeln",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(scaffold.rules) { rule ->
                            Text(
                                "• $rule",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    // ===== SUGGESTED AGENTS =====
                    item {
                        Text(
                            "Deine Agenten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(editedAgents.withIndex().toList()) { (index, agentSpec) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            agentSpec.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            agentSpec.description,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingAgentIndex = index
                                            editedPrompt = agentSpec.systemPrompt
                                        }
                                    ) {
                                        Icon(Icons.Filled.Edit, "Prompt bearbeiten")
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // ===== ACTION BUTTONS =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Abbrechen")
                    }
                    Button(
                        onClick = {
                            onConfirm(editedAgents.map { AgentSpecWithEditedPrompt(it) })
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fertig!")
                    }
                }
            }
        }
    }

    // ===== EDIT PROMPT DIALOG =====
    if (editingAgentIndex != null) {
        EditAgentPromptDialog(
            agentName = editedAgents[editingAgentIndex!!].name,
            prompt = editedPrompt,
            onPromptChange = { editedPrompt = it },
            onConfirm = {
                editedAgents[editingAgentIndex!!] =
                    editedAgents[editingAgentIndex!!].copy(systemPrompt = editedPrompt)
                editingAgentIndex = null
            },
            onDismiss = { editingAgentIndex = null }
        )
    }
}

@Composable
private fun EditAgentPromptDialog(
    agentName: String,
    prompt: String,
    onPromptChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var editedText by remember { mutableStateOf(prompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("System Prompt: $agentName") },
        text = {
            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 300.dp),
                minLines = 6,
                maxLines = 15
            )
        },
        confirmButton = {
            Button(onClick = {
                onPromptChange(editedText)
                onConfirm()
            }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
