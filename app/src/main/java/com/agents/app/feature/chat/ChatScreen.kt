package com.agents.app.feature.chat

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.agents.app.models.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.agents.app.models.Agent
import com.agents.app.models.ChatSessionEntity
import com.agents.app.models.Message
import com.agents.app.models.MessageRole
import com.agents.app.ui.AgentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    session: ChatSessionEntity,
    agent: Agent,
    messages: List<Message>,
    isLoading: Boolean,
    pendingAttachments: List<MessageAttachment> = emptyList(),
    attachmentsByMessage: Map<String, List<MessageAttachment>> = emptyMap(),
    onAttachmentPicked: (Uri) -> Unit = {},
    onRemovePendingAttachment: (MessageAttachment) -> Unit = {},
    uiError: String? = null,
    onClearUiError: () -> Unit = {},
    onSendMessage: (String, List<MessageAttachment>) -> Unit,
    onNavigateBack: () -> Unit,
    onSettings: () -> Unit = {},
    onRenameSession: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: AgentViewModel = viewModel()
    val currentError by viewModel.currentError.collectAsState()
    var inputText by rememberSaveable(session.id) { mutableStateOf("") }
    var showTitleDialog by remember { mutableStateOf(false) }
    var editTitle by rememberSaveable(session.id) { mutableStateOf(session.title) }
    val listState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiError) {
        uiError?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearUiError()
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onAttachmentPicked) }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onAttachmentPicked) }

    LaunchedEffect(session.id) {
        editTitle = session.title
        inputText = ""
    }

    LaunchedEffect(messages.size, currentError) {
        if (messages.isNotEmpty() || currentError != null) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val targetIndex = when {
                currentError != null -> messages.size
                else -> messages.lastIndex
            }
            val nearBottom = lastVisible >= targetIndex - 3
            if (targetIndex >= 0) {
                if (nearBottom) {
                    listState.animateScrollToItem(targetIndex)
                } else {
                    listState.scrollToItem(targetIndex)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.clickable {
                                editTitle = session.title
                                showTitleDialog = true
                            }
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Agent: ${agent.name} | Session: ${session.id.take(8)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        attachments = attachmentsByMessage[message.id].orEmpty()
                    )
                }

                if (isLoading && messages.lastOrNull()?.role == MessageRole.USER) {
                    item {
                        ThinkingBubble()
                    }
                }

                if (!currentError.isNullOrBlank()) {
                    item {
                        ErrorBubble(
                            message = currentError.orEmpty(),
                            onRetry = { viewModel.retryLastMessage() }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider()
                    if (pendingAttachments.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(pendingAttachments.size) { index ->
                                val attachment = pendingAttachments[index]
                                PendingAttachmentChip(
                                    attachment = attachment,
                                    onRemove = { onRemovePendingAttachment(attachment) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("Type a message...") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        IconButton(
                            onClick = { imageLauncher.launch(null) },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Filled.Image,
                                contentDescription = "Bild anhaengen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                fileLauncher.launch(arrayOf("*/*"))
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Filled.AttachFile,
                                contentDescription = "Datei anhaengen",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) {
                                    onSendMessage(inputText.trim(), pendingAttachments)
                                    inputText = ""
                                }
                            },
                            enabled = (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) && !isLoading
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTitleDialog) {
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("Chat umbenennen") },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = editTitle.trim()
                    if (trimmed.isNotBlank()) {
                        onRenameSession(trimmed)
                        showTitleDialog = false
                    }
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun ThinkingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ErrorBubble(
    message: String,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = "Network Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onRetry) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    attachments: List<MessageAttachment> = emptyList()
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            tonalElevation = if (isUser) 0.dp else 2.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = if (isUser) "You" else "Assistant",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (attachments.isNotEmpty()) {
                    attachments.forEach { attachment ->
                        if (attachment.mimeType.startsWith("image/")) {
                            AttachmentImage(
                                attachment = attachment,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            )
                        } else {
                            AttachmentChip(
                                attachment = attachment,
                                isUser = isUser,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                if (message.tokenCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${message.tokenCount} tokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentImage(
    attachment: MessageAttachment,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(attachment.localPath) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(attachment.localPath) {
        bitmap = withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(attachment.localPath, options)?.asImageBitmap()
        }
    }

    val current = bitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = attachment.displayName,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        Surface(
            modifier = modifier.clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = attachment.displayName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun AttachmentChip(
    attachment: MessageAttachment,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isUser) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column {
                Text(
                    text = attachment.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (attachment.sizeBytes > 0) {
                    Text(
                        text = formatAttachmentSize(attachment.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingAttachmentChip(
    attachment: MessageAttachment,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = attachment.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Entfernen",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun formatAttachmentSize(sizeBytes: Long): String {
    return when {
        sizeBytes >= 1024 * 1024 -> "%.1f MB".format(sizeBytes / (1024.0 * 1024.0))
        sizeBytes >= 1024 -> "%.1f KB".format(sizeBytes / 1024.0)
        else -> "$sizeBytes B"
    }
}
