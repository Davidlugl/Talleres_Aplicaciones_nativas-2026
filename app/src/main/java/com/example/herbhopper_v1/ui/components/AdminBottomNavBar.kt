package com.example.herbhopper_v1.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R

@Composable
fun AdminBottomNavBar(currentRoute: String, onNavigate: (String) -> Unit = {}) {
    NavigationBar(
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("store_governance") },
            icon = { Icon(Icons.Default.Dashboard, null) },
            label = { Text(stringResource(id = R.string.stores)) }
        )
        NavigationBarItem(
            selected = currentRoute == "audit",
            onClick = { onNavigate("audit_panel") },
            icon = { Icon(Icons.Default.GppGood, null) },
            label = { Text(stringResource(id = R.string.audit)) }
        )
        NavigationBarItem(
            selected = currentRoute == "analytics",
            onClick = { onNavigate("global_analytics") },
            icon = { Icon(HerbHopperIcons.QueryStats, null) },
            label = { Text(stringResource(id = R.string.analytics)) }
        )
        NavigationBarItem(
            selected = currentRoute == "users",
            onClick = { onNavigate("user_management") },
            icon = { Icon(Icons.Default.People, null) },
            label = { Text("Usuarios") }
        )
        NavigationBarItem(
            selected = currentRoute == "system",
            onClick = { onNavigate("system_infrastructure") },
            icon = { Icon(Icons.Default.Dns, null) },
            label = { Text(stringResource(id = R.string.system)) }
        )
    }
}
