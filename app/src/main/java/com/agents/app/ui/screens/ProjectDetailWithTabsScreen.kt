package com.agents.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.ProjectEntity
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
    onNavigateBack: () -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var deleteConfirmAgent by remember { mutableStateOf<Agent?>(null) }
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
            }

            when (selectedTabIndex) {
                0 -> AgentsTab(
                    agents = agents,
                    sessions = sessions,
                    onSessionSelected = { session ->
                        onSessionSelected(session)
                    },
                    onNewChat = onNewChat,
                    onDeleteAgent = onDeleteAgent,
                    onCreateAgent = onCreateAgent
                )
                1 -> SessionsTab(
                    sessions = sessions.values.flatten(),
                    onSessionSelected = { session ->
                        onSessionSelected(session)
                    }
                )
            }
        }
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
                    onClick = { deleteConfirmAgent = agent },
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

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
