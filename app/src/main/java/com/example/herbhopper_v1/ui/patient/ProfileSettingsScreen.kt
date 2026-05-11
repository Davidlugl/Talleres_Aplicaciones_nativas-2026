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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
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

    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(id = R.string.add_update_card_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16) cardNumber = it },
                        label = { Text(stringResource(id = R.string.card_number_label)) },
                        placeholder = { Text(stringResource(id = R.string.card_digits_placeholder)) }
                    )
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
                            label = { Text(stringResource(id = R.string.expiry_label)) },
                            placeholder = { Text(stringResource(id = R.string.expiry_placeholder)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { if (it.length <= 3) cvv = it },
                            label = { Text(stringResource(id = R.string.cvv_label)) },
                            placeholder = { Text(stringResource(id = R.string.cvv_placeholder)) },
                            modifier = Modifier.weight(1f)
                        )
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
                }) { Text(stringResource(id = R.string.save_btn)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(id = R.string.cancel_btn)) } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = R.string.saved_cards_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        profileFromDb?.paymentMethod?.let { method ->
            SettingItem(
                title = method,
                subtitle = stringResource(id = R.string.default_method),
                icon = Icons.Default.CreditCard,
                onDelete = {
                    profileFromDb?.let { profile ->
                        val updated = profile.copy(paymentMethod = null)
                        viewModel.saveProfile(updated)
                    }
                }
            )
        } ?: Text(stringResource(id = R.string.no_saved_cards), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (profileFromDb?.paymentMethod == null) stringResource(id = R.string.add_new_method)
                else stringResource(id = R.string.change_card)
            )
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

    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(id = R.string.delivery_address_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = { addressText = it },
                        label = { Text(stringResource(id = R.string.complete_address_label)) },
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
                }) { Text(stringResource(id = R.string.save_btn)) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(id = R.string.cancel_btn)) } }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(id = R.string.delivery_addresses_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        profileFromDb?.address?.let { addr ->
            SettingItem(
                title = stringResource(id = R.string.main_address_label),
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
        } ?: Text(stringResource(id = R.string.no_saved_addresses), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Button(onClick = { addressText = ""; showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (profileFromDb?.address == null) stringResource(id = R.string.add_new_address)
                else stringResource(id = R.string.change_address)
            )
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

    val profileFromDb by viewModel.observeProfile(uid).collectAsState(initial = null)

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    LaunchedEffect(profileFromDb) {
        profileFromDb?.let {
            name = it.name
            phone = it.phone ?: ""
            email = it.email
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(id = R.string.personal_info_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(id = R.string.full_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) }
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(id = R.string.phone_label)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, null) }
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(id = R.string.email_settings_label)) },
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
            Text(stringResource(id = R.string.save_changes_btn), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NotificationSettings() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(id = R.string.notification_prefs_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        NotificationToggle(title = stringResource(id = R.string.push_notifications), initial = true)
        NotificationToggle(title = stringResource(id = R.string.email_promotions), initial = false)
        NotificationToggle(title = stringResource(id = R.string.order_alerts), initial = true)
        NotificationToggle(title = stringResource(id = R.string.account_security), initial = true)
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
