package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfrastructureScreen(onNavigate: (String) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Infraestructura de Sistema", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "system", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text("Estado de Servidores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            SystemItem("Base de Datos Principal", "En Línea", true)
            SystemItem("Servicio de Autenticación", "En Línea", true)
            SystemItem("API de Pagos", "Mantenimiento", false)
            SystemItem("Servidor de Almacenamiento", "En Línea", true)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reiniciar Servicios", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SystemItem(name: String, status: String, isOnline: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFFC107))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(status, style = MaterialTheme.typography.labelMedium, color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFFFC107))
        }
    }
}
