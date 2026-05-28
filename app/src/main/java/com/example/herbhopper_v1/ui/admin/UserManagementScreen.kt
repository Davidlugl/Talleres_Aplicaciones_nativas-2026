package com.example.herbhopper_v1.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.ForgotPasswordRequest
import com.example.herbhopper_v1.data.network.PasswordResetResponse
import com.example.herbhopper_v1.data.network.RegisterRequest
import com.example.herbhopper_v1.data.network.UpdateUserRequest
import com.example.herbhopper_v1.data.network.UserResponse
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar
import kotlinx.coroutines.launch

/**
 * Pantalla de Control de Cuentas de Usuario y Soporte de Claves (Admin User Management Panel).
 * Permite listar compradores, vendedores y pedidos de restablecimiento de contraseña en tiempo real.
 * Implementa la edición directa de credenciales (nombres, correos, contraseñas y roles) y eliminación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Compradores", "Vendedores", "Pedidos Clave")
    
    var usersList by remember { mutableStateOf<List<UserResponse>>(emptyList()) }
    var resetsList by remember { mutableStateOf<List<PasswordResetResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dialog state for Creating a User
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("PATIENT") }

    // Dialog state for Editing a User
    var showEditDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserResponse?>(null) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("") }

    // Refresh function
    fun refreshData() {
        isLoading = true
        scope.launch {
            try {
                usersList = ApiService.instance.getAllUsers()
                resetsList = ApiService.instance.getPasswordResets()
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                Toast.makeText(context, "Error al conectar al servidor: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    val buyers = usersList.filter { it.role.uppercase() == "PATIENT" }
    val sellers = usersList.filter { it.role.uppercase() == "SELLER" }

    // Create User Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Nuevo Usuario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text("Rol de Usuario:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = newRole == "PATIENT",
                            onClick = { newRole = "PATIENT" },
                            label = { Text("Comprador") }
                        )
                        FilterChip(
                            selected = newRole == "SELLER",
                            onClick = { newRole = "SELLER" },
                            label = { Text("Vendedor") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank() || newEmail.isBlank() || newPassword.isBlank()) {
                            Toast.makeText(context, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                ApiService.instance.register(
                                    RegisterRequest(newName.trim(), newEmail.trim(), newPassword, newRole)
                                )
                                showCreateDialog = false
                                newName = ""
                                newEmail = ""
                                newPassword = ""
                                newRole = "PATIENT"
                                Toast.makeText(context, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show()
                                refreshData()
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Error al crear: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Edit User Dialog
    if (showEditDialog && editingUser != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Corregir Usuario", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text("Nueva Contraseña (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Dejar en blanco para no cambiar") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Text("Rol de Usuario:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = editRole == "PATIENT",
                            onClick = { editRole = "PATIENT" },
                            label = { Text("Comprador") }
                        )
                        FilterChip(
                            selected = editRole == "SELLER",
                            onClick = { editRole = "SELLER" },
                            label = { Text("Vendedor") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isBlank() || editEmail.isBlank()) {
                            Toast.makeText(context, "Nombre y Email son requeridos", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            try {
                                val pwd = if (editPassword.trim().isEmpty()) null else editPassword
                                ApiService.instance.updateUser(
                                    editingUser!!.id,
                                    UpdateUserRequest(editName.trim(), editEmail.trim(), pwd, editRole)
                                )
                                showEditDialog = false
                                Toast.makeText(context, "Usuario modificado con éxito", Toast.LENGTH_SHORT).show()
                                refreshData()
                            } catch (e: Exception) {
                                isLoading = false
                                Toast.makeText(context, "Error al modificar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Administración", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "users", onNavigate = onNavigate) },
        floatingActionButton = {
            if (selectedTab < 2) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar Usuario")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Buyers
                    UserListSection(
                        users = buyers,
                        onEdit = { user ->
                            editingUser = user
                            editName = user.name
                            editEmail = user.email
                            editPassword = ""
                            editRole = user.role
                            showEditDialog = true
                        },
                        onDelete = { user ->
                            isLoading = true
                            scope.launch {
                                try {
                                    ApiService.instance.deleteUser(user.id)
                                    Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                    refreshData()
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
                1 -> {
                    // Sellers
                    UserListSection(
                        users = sellers,
                        onEdit = { user ->
                            editingUser = user
                            editName = user.name
                            editEmail = user.email
                            editPassword = ""
                            editRole = user.role
                            showEditDialog = true
                        },
                        onDelete = { user ->
                            isLoading = true
                            scope.launch {
                                try {
                                    ApiService.instance.deleteUser(user.id)
                                    Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
                                    refreshData()
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
                2 -> {
                    // Resets Requests
                    ResetsRequestSection(
                        resets = resetsList,
                        onResolve = { resetId ->
                            isLoading = true
                            scope.launch {
                                try {
                                    ApiService.instance.resolvePasswordReset(resetId)
                                    Toast.makeText(context, "Pedido marcado como resuelto", Toast.LENGTH_SHORT).show()
                                    refreshData()
                                } catch (e: Exception) {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserListSection(
    users: List<UserResponse>,
    onEdit: (UserResponse) -> Unit,
    onDelete: (UserResponse) -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No hay usuarios registrados en este rol.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = if (user.role.uppercase() == "SELLER") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    user.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.role.uppercase() == "SELLER") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row {
                            IconButton(onClick = { onEdit(user) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(user) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResetsRequestSection(
    resets: List<PasswordResetResponse>,
    onResolve: (Int) -> Unit
) {
    if (resets.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No hay solicitudes de restablecimiento de contraseña.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(resets) { reset ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (reset.status == "PENDING") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (reset.status == "PENDING") Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (reset.status == "PENDING") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(reset.email, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Fecha: ${formatTimestamp(reset.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Estado: ${if (reset.status == "PENDING") "PENDIENTE" else "RESUELTO"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (reset.status == "PENDING") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        if (reset.status == "PENDING") {
                            Button(
                                onClick = { onResolve(reset.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Resolver", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        "Fecha inválida"
    }
}
