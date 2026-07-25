package com.agents.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agents.app.models.Agent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    agents: List<Agent>,
    onSelectAgent: (Agent) -> Unit,
    onShowAllAgents: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Live clock
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
            currentDate = SimpleDateFormat("EEEE, d. MMMM yyyy", Locale.getDefault()).format(now.time)
            delay(1000)
        }
    }

    // Last 3 agents by lastRunAt
    val recentAgents = remember(agents) {
        agents.filter { it.lastRunAt != null }
            .sortedByDescending { it.lastRunAt }
            .take(3)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android Agents") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Clock
            Text(
                text = currentTime,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = currentDate,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            HorizontalDivider()

            // Recent agents
            Text(
                text = "Recent Agents",
                style = MaterialTheme.typography.titleMedium
            )

            if (recentAgents.isEmpty()) {
                Text(
                    text = "No recent activity yet. Start a chat to see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentAgents, key = { it.id }) { agent ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectAgent(agent) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = agent.name,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = agent.model,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(Date(agent.lastRunAt!!)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick actions
            Button(
                onClick = onShowAllAgents,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show All Agents")
            }
        }
    }
}
