package com.example.herbhopper_v1.ui.admin

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.UserProfile
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

/**
 * Pantalla de Control de Cuentas de Usuario (User Management Screen).
 * Muestra el listado completo de perfiles de usuario registrados en la base de datos local.
 * Proporciona un botón flotante de acción (FAB) para abrir un diálogo interactivo de creación
 * de nuevo usuario, ordenación por orden alfabético o roles, y listado reactivo para editar o eliminar.
 *
 * @param onBack Función callback para retornar a la pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(onBack: () -> Unit) {
    val users = remember {
        mutableStateListOf(
            UserProfile("1", "Juan Perez", "juan@mail.com", role = "PATIENT"),
            UserProfile("2", "Maria Gomez", "maria@mail.com", role = "SELLER"),
            UserProfile("3", "Admin Master", "admin@herbhopper.com", role = "ADMIN"),
            UserProfile("4", "Carlos Ruiz", "carlos@mail.com", role = "PATIENT")
        )
    }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var sortByName by remember { mutableStateOf(true) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("PATIENT") }
    val context = LocalContext.current

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Crear Usuario") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEmail, onValueChange = { newEmail = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rol:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = newRole == "PATIENT", onClick = { newRole = "PATIENT" }, label = { Text("Paciente") })
                        FilterChip(selected = newRole == "SELLER", onClick = { newRole = "SELLER" }, label = { Text("Vendedor") })
                        FilterChip(selected = newRole == "ADMIN", onClick = { newRole = "ADMIN" }, label = { Text("Admin") })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank() && newEmail.isNotBlank()) {
                        users.add(UserProfile(System.currentTimeMillis().toString(), newName, newEmail, role = newRole))
                        showCreateDialog = false
                        newName = ""
                        newEmail = ""
                        newRole = "PATIENT"
                        Toast.makeText(context, "Usuario Creado", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.user_management_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        sortByName = !sortByName
                        if (sortByName) {
                            users.sortBy { it.name }
                        } else {
                            users.sortBy { it.role }
                        }
                    }) { Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_user))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(users) { user ->
                UserItem(
                    user = user,
                    onEdit = { /* Editar */ },
                    onDelete = { users.remove(user) }
                )
            }
        }
    }
}

/**
 * Fila representativa de un usuario registrado en el sistema.
 * Colorea de forma dinámica el avatar inicial basándose en el rol del usuario (ADMIN, SELLER, PATIENT)
 * y provee botones de control rápido para editar y eliminar.
 *
 * @param user El objeto [UserProfile] a representar.
 * @param onEdit Función callback para procesar la edición del perfil de usuario.
 * @param onDelete Función callback para procesar la eliminación definitiva del usuario de la lista.
 */
@Composable
fun UserItem(
    user: UserProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = when (user.role) {
                    "ADMIN" -> MaterialTheme.colorScheme.errorContainer
                    "SELLER" -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = when (user.role) {
                            "ADMIN" -> MaterialTheme.colorScheme.error
                            "SELLER" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    user.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
