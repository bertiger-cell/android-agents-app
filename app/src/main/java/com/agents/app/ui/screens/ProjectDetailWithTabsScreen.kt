package com.agents.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.ProjectEntity
import com.agents.app.ui.ProjectFileInfo
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailWithTabsScreen(
    project: ProjectEntity,
    agents: List<Agent>,
    sessions: Map<String, List<ChatSessionEntity>>,
    onCreateAgent: () -> Unit,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onNewChat: (Agent) -> Unit = {},
    onDeleteAgent: (Agent) -> Unit = {},
    onRestartArchitectDiscovery: () -> Unit = {},
    onUpdateProject: (projectId: String, name: String, description: String) -> Unit = { _, _, _ -> },
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var deleteConfirmAgent by remember { mutableStateOf<Agent?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    // Chat-Modal handled via AppNavigation Dialog
    // Chat-Modal handled via AppNavigation Dialog

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${agents.size} Agenten",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zuruck")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Projekt bearbeiten")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Agenten (${agents.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Sessions") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Dateien") }
                )
            }

            when (selectedTabIndex) {
                0 -> AgentsTab(
                    agents = agents,
                    sessions = sessions,
                    onSessionSelected = { session ->
                        onSessionSelected(session)
                    },
                    onNewChat = onNewChat,
                    onDeleteAgent = { agent -> deleteConfirmAgent = agent },
                    onRestartArchitectDiscovery = onRestartArchitectDiscovery,
                    onCreateAgent = onCreateAgent
                )
                1 -> SessionsTab(
                    sessions = sessions.values.flatten(),
                    onSessionSelected = { session ->
                        onSessionSelected(session)
                    }
                )
                2 -> ProjectFilesTab(
                    project = project
                )
            }
        }
    }

    // Edit Project Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(project.name) }
        var editDescription by remember { mutableStateOf(project.description) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Projekt bearbeiten") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Projektname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("Beschreibung") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProject(project.id, editName, editDescription)
                        showEditDialog = false
                    },
                    enabled = editName.isNotBlank()
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Delete Confirmation Dialog for Agent
    if (deleteConfirmAgent != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmAgent = null },
            title = { Text("Agent loschen?") },
            text = {
                Text("Bist du sicher, dass du den Agenten '${deleteConfirmAgent!!.name}' loschen moechtest?\n\n" +
                     "Alle zugehoerigen Sessions und Nachrichten werden ebenfalls geloescht.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAgent(deleteConfirmAgent!!)
                        deleteConfirmAgent = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Loschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmAgent = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun AgentsTab(
    agents: List<Agent>,
    sessions: Map<String, List<ChatSessionEntity>>,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onNewChat: (Agent) -> Unit = {},
    onDeleteAgent: (Agent) -> Unit = {},
    onRestartArchitectDiscovery: () -> Unit = {},
    onCreateAgent: () -> Unit
) {
    if (agents.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keine Agenten", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateAgent) {
                Text("Agent erstellen")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRestartArchitectDiscovery) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Discovery wiederholen")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(agents, key = { it.id }) { agent ->
                AgentCardWithSessions(
                    agent = agent,
                    agentSessions = sessions[agent.id] ?: emptyList(),
                    onSessionSelected = onSessionSelected,
                    onNewChat = onNewChat
                )
            }
            item {
                Button(
                    onClick = onCreateAgent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agent hinzufugen")
                }
                OutlinedButton(
                    onClick = onRestartArchitectDiscovery,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discovery wiederholen")
                }
            }
        }
    }
}

@Composable
fun SessionsTab(
    sessions: List<ChatSessionEntity>,
    onSessionSelected: (ChatSessionEntity) -> Unit
) {
    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.ChatBubble,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keine Sessions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Erstelle einen Agenten und starte einen Chat",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    onSelected = { onSessionSelected(session) }
                )
            }
        }
    }
}

@Composable
fun AgentCardWithSessions(
    agent: Agent,
    agentSessions: List<ChatSessionEntity>,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onNewChat: (Agent) -> Unit = {},
    onDeleteAgent: (Agent) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(agent.name, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${agent.provider.name}  ${agent.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = { onDeleteAgent(agent) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Agent loschen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (agentSessions.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    agentSessions.forEach { session ->
                        SessionListItem(
                            session = session,
                            onSelected = { onSessionSelected(session) }
                        )
                    }
                    OutlinedButton(
                        onClick = { onNewChat(agent) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Neuer Chat")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Noch keine Sessions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onNewChat(agent) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat starten")
                }
            }
        }
    }
}

@Composable
fun SessionListItem(
    session: ChatSessionEntity,
    onSelected: (ChatSessionEntity) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(session) },
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDate(session.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ChatModalPlaceholder(
    session: ChatSessionEntity,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.6f)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Chat  ${session.title}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "(Task 6: ChatScreen wird hier eingefugt)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss) {
                    Text("Schliessen")
                }
            }
        }
    }
}

@Composable
fun ProjectFilesTab(
    project: ProjectEntity,
    modifier: Modifier = Modifier
) {
    val viewModel: com.agents.app.ui.AgentViewModel = viewModel()
    var files by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(project.id) {
        isLoading = true
        viewModel.getProjectFiles(project.id) { result ->
            files = result
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Keine Dateien gefunden",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Fuehre zuerst den Architect-Discovery durch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedFile = file },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (file.isMarkdown) Icons.Filled.Description else Icons.Filled.Description,
                            contentDescription = null,
                            tint = if (file.isMarkdown) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            file.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // File Content Dialog
    if (selectedFile != null) {
        AlertDialog(
            onDismissRequest = { selectedFile = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selectedFile!!.isMarkdown) Icons.Filled.Description else Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedFile!!.name)
                }
            },
            text = {
                rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = selectedFile!!.content,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFile = null }) {
                    Text("Schliessen")
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
