package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.model.AuditLog
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar

/** Tipos de evento que registra el backend en la bitácora. */
private val LOG_FILTERS = listOf(
    null to "Todos",
    "ACCESS" to "Accesos",
    "TOKEN" to "Tokens",
    "EPAYCO" to "ePayco",
    "ORDER" to "Pedidos",
    "PRESCRIPTION" to "Fórmulas",
    "INVENTORY" to "Inventario",
    "ADMIN" to "Admin",
    "INFRA" to "Infraestructura"
)

/**
 * Panel de Auditoría (administrador): bitácora real del backend (/api/admin/logs) con accesos,
 * tokens emitidos, respuestas de ePayco, cambios de pedidos, fórmulas e inventario.
 * Los eventos de advertencia (accesos fallidos, pagos rechazados, inactivaciones) se resaltan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditPanelScreen(onNavigate: (String) -> Unit = {}) {
    val api = remember { ApiService.instance }
    var selectedType by remember { mutableStateOf<String?>(null) }
    var logs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val timeFormat = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("es", "CO")) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedType, reloadKey) {
        isLoading = true
        try {
            logs = api.getAuditLogs(limit = 200, type = selectedType).map {
                AuditLog(
                    title = LOG_FILTERS.firstOrNull { f -> f.first == it.type }?.second ?: it.type,
                    description = it.message,
                    time = timeFormat.format(java.util.Date(it.timestamp)),
                    isWarning = it.level == "WARNING"
                )
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.apiErrorMessage()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.audit_panel_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { reloadKey++ }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "audit", onNavigate = onNavigate) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LOG_FILTERS.forEach { (type, label) ->
                    FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(label) })
                }
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (logs.isEmpty() && !isLoading && errorMessage == null) {
                    item { Text(stringResource(id = R.string.no_audit_logs), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(logs) { log -> AuditCard(log) }
            }
        }
    }
}

/**
 * Tarjeta de un registro de auditoría. Las advertencias se muestran con fondo rojizo.
 *
 * @param log El objeto [AuditLog] a renderizar.
 */
@Composable
fun AuditCard(log: AuditLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (log.isWarning) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (log.isWarning) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = if (log.isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.title, fontWeight = FontWeight.Bold)
                Text(log.description, style = MaterialTheme.typography.bodySmall)
            }
            Text(log.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
