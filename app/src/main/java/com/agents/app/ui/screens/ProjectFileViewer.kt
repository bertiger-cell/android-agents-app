package com.agents.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

data class ProjectFileInfo(
    val name: String,
    val path: String,
    val lastModified: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectFileViewerScreen(
    folderPath: String,
    onNavigateBack: () -> Unit
) {
    var files by remember { mutableStateOf<List<ProjectFileInfo>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<ProjectFileInfo?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(folderPath) {
        isLoading = true
        val projectDir = File(folderPath)
        if (projectDir.exists()) {
            files = projectDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".md") }
                ?.map { ProjectFileInfo(it.name, it.absolutePath, it.lastModified()) }
                ?.sortedByDescending { it.lastModified }
                ?: emptyList()
        }
        isLoading = false
    }

    if (selectedFile != null) {
        // File content view
        FileContentViewer(
            fileName = selectedFile!!.name,
            content = fileContent,
            onBack = {
                selectedFile = null
                fileContent = ""
            }
        )
    } else {
        // File list view
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Projekt-Dateien") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, "Zurueck")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (files.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine .md Dateien gefunden",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(files, key = { it.path }) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFile = file
                                    fileContent = File(file.path).readText()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        formatFileSize(file.path),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileContentViewer(
    fileName: String,
    content: String,
    onBack: () -> Unit
) {
    var showRaw by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Zurueck")
                    }
                },
                actions = {
                    TextButton(onClick = { showRaw = !showRaw }) {
                        Text(if (showRaw) "Vorschau" else "Raw")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (showRaw) {
                item {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // Simple rendered view: split by ## headers
                val sections = content.split("\n## ")
                item {
                    sections.forEachIndexed { index, section ->
                        if (index == 0) {
                            Text(
                                section.trim(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                section.substringBefore("\n"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                section.substringAfter("\n", "").trim(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(filePath: String): String {
    val file = File(filePath)
    val size = file.length()
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}
