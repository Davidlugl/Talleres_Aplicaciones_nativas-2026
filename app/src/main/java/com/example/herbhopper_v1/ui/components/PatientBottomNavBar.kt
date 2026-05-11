package com.example.herbhopper_v1.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.Background
import com.example.herbhopper_v1.ui.theme.Primary

@Composable
fun PatientBottomNavBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    NavigationBar(
        containerColor = Background,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text(stringResource(id = R.string.home)) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Primary, selectedTextColor = Primary)
        )
        NavigationBarItem(
            selected = currentRoute == "orders",
            onClick = onOrdersClick,
            icon = { Icon(HerbHopperIcons.ReceiptLong, null) },
            label = { Text(stringResource(id = R.string.orders)) }
        )
        NavigationBarItem(
            selected = currentRoute == "scripts",
            onClick = onScriptsClick,
            icon = { Icon(HerbHopperIcons.Prescriptions, null) },
            label = { Text(stringResource(id = R.string.scripts)) }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onProfileClick,
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text(stringResource(id = R.string.profile)) }
        )
    }
}
