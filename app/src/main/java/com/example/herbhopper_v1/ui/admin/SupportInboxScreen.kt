package com.example.herbhopper_v1.ui.admin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.SupportConversationDto
import com.example.herbhopper_v1.data.network.SupportMessageDto
import com.example.herbhopper_v1.data.network.SupportMessageRequest
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.model.ChatMessage
import com.example.herbhopper_v1.ui.patient.ChatBubble
import kotlinx.coroutines.launch

/**
 * Mesa de ayuda (administrador): lista las conversaciones del chat de soporte, resalta las que
 * esperan respuesta y permite contestar. La respuesta aparece en el chat del usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportInboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiService.instance }
    val timeFormat = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("es", "CO")) }
    var conversations by remember { mutableStateOf<List<SupportConversationDto>>(emptyList()) }
    var selected by remember { mutableStateOf<SupportConversationDto?>(null) }
    var messages by remember { mutableStateOf<List<SupportMessageDto>>(emptyList()) }
    var reply by remember { mutableStateOf("") }

    suspend fun loadConversations() {
        try {
            conversations = api.getSupportConversations()
        } catch (e: Exception) {
            Toast.makeText(context, e.apiErrorMessage(), Toast.LENGTH_LONG).show()
        }
    }

    suspend fun loadMessages(userId: String) {
        try {
            messages = api.getSupportConversation(userId)
        } catch (e: Exception) {
            Toast.makeText(context, e.apiErrorMessage(), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(selected) {
        val current = selected
        if (current == null) loadConversations() else loadMessages(current.userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: stringResource(id = R.string.support_inbox_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (selected != null) selected = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch { selected?.let { loadMessages(it.userId) } ?: loadConversations() }
                    }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh)) }
                }
            )
        },
        bottomBar = {
            val current = selected ?: return@Scaffold
            Surface(shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        placeholder = { Text(stringResource(id = R.string.support_reply_hint)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    IconButton(onClick = {
                        val text = reply.trim()
                        if (text.isEmpty()) return@IconButton
                        scope.launch {
                            try {
                                api.replySupport(current.userId, SupportMessageRequest(text))
                                reply = ""
                                loadMessages(current.userId)
                            } catch (e: Exception) {
                                Toast.makeText(context, e.apiErrorMessage(), Toast.LENGTH_LONG).show()
                            }
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) }
                }
            }
        }
    ) { padding ->
        val current = selected
        if (current == null) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                if (conversations.isEmpty()) {
                    item { Text(stringResource(id = R.string.support_inbox_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(conversations) { conversation ->
                    Card(onClick = { selected = conversation }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(conversation.name, fontWeight = FontWeight.Bold)
                                if (conversation.awaitingReply) {
                                    Text(stringResource(id = R.string.support_awaiting_reply), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(conversation.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(conversation.text, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // En la bandeja, "del usuario" = mensajes del equipo de soporte (quedan a la derecha)
                items(messages) { m ->
                    ChatBubble(ChatMessage(m.id.toString(), m.text, m.fromSupport, timeFormat.format(java.util.Date(m.timestamp))))
                }
            }
        }
    }
}
