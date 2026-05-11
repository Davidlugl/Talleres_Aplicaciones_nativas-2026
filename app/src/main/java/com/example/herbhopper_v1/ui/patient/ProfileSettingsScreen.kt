package com.example.herbhopper_v1.ui.patient

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    title: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (title) {
                "Configuración" -> AccountSettings()
                "Métodos de Pago" -> PaymentSettings()
                "Direcciones" -> AddressSettings()
                "Notificaciones" -> NotificationSettings()
            }
        }
    }
}

@Composable
fun PaymentSettings() {
    val context = LocalContext.current
    val viewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val currentUser = auth?.currentUser
    val uid = currentUser?.uid ?: "dummy_uid_patient"

    var showDialog by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // Usamos collectAsState para observar cambios en tiempo real
    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Añadir/Actualizar Tarjeta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cardNumber, onValueChange = { if (it.length <= 16) cardNumber = it }, label = { Text("Número de Tarjeta") }, placeholder = { Text("16 dígitos") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = {
                                val clean = it.filter { char -> char.isDigit() }
                                if (clean.length <= 4) {
                                    expiry = when {
                                        clean.length >= 3 -> "${clean.take(2)}/${clean.drop(2)}"
                                        else -> clean
                                    }
                                }
                            },
                            label = { Text("Vencimiento") },
                            placeholder = { Text("MM/AA") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(value = cvv, onValueChange = { if (it.length <= 3) cvv = it }, label = { Text("CVV") }, placeholder = { Text("123") }, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cardNumber.length == 16) {
                        val method = "**** **** **** ${cardNumber.takeLast(4)}"
                        val updated = profileFromDb?.copy(paymentMethod = method) ?: com.example.herbhopper_v1.data.UserProfile(
                            uid = uid,
                            name = currentUser?.displayName ?: "Usuario",
                            email = currentUser?.email ?: "",
                            role = "PATIENT",
                            paymentMethod = method
                        )
                        viewModel.saveProfile(updated)
                        Toast.makeText(context, "Método de pago guardado", Toast.LENGTH_SHORT).show()
                        showDialog = false
                        cardNumber = ""; expiry = ""; cvv = ""
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tus Tarjetas Guardadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        profileFromDb?.paymentMethod?.let { method ->
            SettingItem(
                title = method,
                subtitle = "Método Predeterminado",
                icon = Icons.Default.CreditCard,
                onDelete = { 
                    profileFromDb?.let { profile ->
                        val updated = profile.copy(paymentMethod = null)
                        viewModel.saveProfile(updated)
                    }
                }
            )
        } ?: Text("No tienes tarjetas guardadas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (profileFromDb?.paymentMethod == null) "Añadir Nuevo Método" else "Cambiar Tarjeta")
        }
    }
}

@Composable
fun AddressSettings() {
    val context = LocalContext.current
    val viewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val currentUser = auth?.currentUser
    val uid = currentUser?.uid ?: "dummy_uid_patient"

    var showDialog by remember { mutableStateOf(false) }
    var addressText by remember { mutableStateOf("") }
    
    // Observación reactiva
    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Dirección de Entrega") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addressText, 
                        onValueChange = { addressText = it }, 
                        label = { Text("Dirección completa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (addressText.isNotBlank()) {
                        val updated = profileFromDb?.copy(address = addressText) ?: com.example.herbhopper_v1.data.UserProfile(
                            uid = uid,
                            name = currentUser?.displayName ?: "Usuario",
                            email = currentUser?.email ?: "",
                            role = "PATIENT",
                            address = addressText
                        )
                        viewModel.saveProfile(updated)
                        Toast.makeText(context, "Dirección guardada", Toast.LENGTH_SHORT).show()
                        showDialog = false
                        addressText = ""
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Direcciones de Entrega", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        profileFromDb?.address?.let { addr ->
            SettingItem(
                title = "Principal",
                subtitle = addr,
                icon = Icons.Default.Home,
                onEdit = {
                    addressText = addr
                    showDialog = true
                },
                onDelete = { 
                    profileFromDb?.let { profile ->
                        val updated = profile.copy(address = null)
                        viewModel.saveProfile(updated)
                    }
                }
            )
        } ?: Text("No tienes direcciones guardadas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = { addressText = ""; showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (profileFromDb?.address == null) "Añadir Nueva Dirección" else "Cambiar Dirección")
        }
    }
}

@Composable
fun AccountSettings() {
    val context = LocalContext.current
    val viewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val currentUser = auth?.currentUser
    val uid = currentUser?.uid ?: "dummy_uid_patient"
    
    // Usamos collectAsState para que la UI reaccione a la base de datos REAL
    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    // Actualizar campos de texto cuando cambie el perfil en la base de datos
    LaunchedEffect(profileFromDb) {
        profileFromDb?.let {
            name = it.name
            phone = it.phone ?: ""
            email = it.email
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Información Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it }, 
            label = { Text("Nombre Completo") }, 
            modifier = Modifier.fillMaxWidth(), 
            leadingIcon = { Icon(Icons.Default.Person, null) }
        )
        OutlinedTextField(
            value = phone, 
            onValueChange = { phone = it }, 
            label = { Text("Teléfono") }, 
            modifier = Modifier.fillMaxWidth(), 
            leadingIcon = { Icon(Icons.Default.Phone, null) }
        )
        OutlinedTextField(
            value = email, 
            onValueChange = { email = it }, 
            label = { Text("Correo Electrónico") }, 
            modifier = Modifier.fillMaxWidth(), 
            leadingIcon = { Icon(Icons.Default.Email, null) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { 
                val updatedProfile = profileFromDb?.copy(
                    name = name,
                    email = email,
                    phone = phone
                ) ?: com.example.herbhopper_v1.data.UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    role = "PATIENT"
                )
                
                android.util.Log.d("ProfileSettings", "Click en Guardar para UID: $uid")
                viewModel.saveProfile(updatedProfile)
                Toast.makeText(context, "Guardando cambios...", Toast.LENGTH_SHORT).show()
            }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationSettings() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Preferencias de Notificación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        NotificationToggle(title = "Notificaciones Push", initial = true)
        NotificationToggle(title = "Promociones por Email", initial = false)
        NotificationToggle(title = "Alertas de Pedido", initial = true)
        NotificationToggle(title = "Seguridad de la Cuenta", initial = true)
    }
}

@Composable
fun NotificationToggle(title: String, initial: Boolean) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun SettingItem(title: String, subtitle: String, icon: ImageVector, onEdit: (() -> Unit)? = null, onDelete: () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onEdit != null) {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
        }
    }
}
