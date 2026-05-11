package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar
import com.example.herbhopper_v1.ui.components.HerbHopperIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditPanelScreen(onNavigate: (String) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.audit_panel_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "audit", onNavigate = onNavigate) }
    ) { padding ->
        val auditLogs = listOf(
            AuditLog("Acceso denegado", "IP: 192.168.1.1 - Intento de login fallido", "10:30 AM", true),
            AuditLog("Actualización de stock", "Vendedor: Botanical Precision - Item: Blue Dream", "09:45 AM", false),
            AuditLog("Nueva tienda autorizada", "Admin: David - Tienda: MedLeaf", "Yesterday", false)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(auditLogs) { log ->
                AuditCard(log)
            }
        }
    }
}

data class AuditLog(val title: String, val description: String, val time: String, val isWarning: Boolean)

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
