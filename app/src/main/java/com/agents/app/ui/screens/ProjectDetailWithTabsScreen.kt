package com.agents.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.ProjectEntity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailWithTabsScreen(
    project: ProjectEntity,
    agents: List<Agent>,
    sessions: Map<String, List<ChatSessionEntity>>,
    onCreateAgent: () -> Unit,
    onStartChat: (Agent) -> Unit,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableStateOf(0) }

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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateAgent) {
                Icon(Icons.Filled.Add, contentDescription = "Agent hinzufügen")
            }
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
                    onSessionSelected = onSessionSelected,
                    onCreateAgent = onCreateAgent
                )
                1 -> SessionsTab(
                    sessions = sessions.values.flatten(),
                    onSessionSelected = onSessionSelected
                )
            }
        }
    }
}

@Composable
private fun AgentsTab(
    agents: List<Agent>,
    sessions: Map<String, List<ChatSessionEntity>>,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onStartChat: (Agent) -> Unit,
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
            Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keine Agenten", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onCreateAgent) {
                Text("Agent erstellen")
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
                    onStartChat = onStartChat
                )
            }
            item {
                Button(
                    onClick = onCreateAgent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agent hinzufügen")
                }
            }
        }
    }
}

@Composable
private fun SessionsTab(
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
            Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Keine Sessions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Starte einen Chat um eine Session zu erstellen",
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
private fun AgentCardWithSessions(
    agent: Agent,
    agentSessions: List<ChatSessionEntity>,
    onSessionSelected: (ChatSessionEntity) -> Unit,
    onStartChat: (Agent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(agent.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${agent.provider.name} • ${agent.model}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (agent.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    agent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onStartChat(agent) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chat starten")
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            if (agentSessions.isEmpty()) {
                Text(
                    "Noch keine Chat-Sessions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    agentSessions.forEach { session ->
                        SessionListItem(session, onSessionSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionListItem(
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
                Text(session.title, style = MaterialTheme.typography.bodyMedium)
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
