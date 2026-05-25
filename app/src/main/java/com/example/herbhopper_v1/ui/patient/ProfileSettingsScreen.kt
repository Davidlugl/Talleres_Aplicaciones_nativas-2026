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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Pantalla General de Ajustes de Perfil (Profile Settings Screen).
 * Actúa como contenedor dinámico para mostrar diferentes sub-pantallas según la sección
 * seleccionada por el usuario (Configuración de Cuenta, Métodos de Pago, Direcciones, Notificaciones).
 *
 * @param title El título de la sub-sección seleccionada a renderizar (ej. "Configuración", "Métodos de Pago").
 * @param onBack Función callback para regresar a la pantalla anterior.
 */
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

/**
 * Formulario y gestión para los Ajustes de Métodos de Pago.
 * Permite agregar una nueva tarjeta de crédito mediante un diálogo modal, validando la longitud
 * y formato de la entrada de datos, y vinculándola al perfil persistente a través de [ProfileViewModel].
 */
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
                        onValueChange = { cardNumber = it.filter { c -> c.isDigit() }.take(16) },
                        label = { Text(stringResource(id = R.string.card_number_label)) },
                        placeholder = { Text(stringResource(id = R.string.card_digits_placeholder)) },
                        visualTransformation = ProfileCreditCardTransformation()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = { expiry = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text(stringResource(id = R.string.expiry_label)) },
                            placeholder = { Text(stringResource(id = R.string.expiry_placeholder)) },
                            modifier = Modifier.weight(1f),
                            visualTransformation = ProfileExpiryDateTransformation()
                        )
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text(stringResource(id = R.string.cvv_label)) },
                            placeholder = { Text(stringResource(id = R.string.cvv_placeholder)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (cardNumber.length != 16) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_card), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (expiry.length != 4) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_expiry), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (cvv.length != 3) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_cvv), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val method = "**** **** **** ${cardNumber.takeLast(4)}"
                    val updated = profileFromDb?.copy(paymentMethod = method) ?: com.example.herbhopper_v1.data.UserProfile(
                        uid = uid,
                        name = currentUser?.displayName ?: "Usuario",
                        email = currentUser?.email ?: "",
                        role = "PATIENT",
                        paymentMethod = method
                    )
                    viewModel.saveProfile(updated)
                    Toast.makeText(context, context.getString(R.string.toast_payment_method_saved), Toast.LENGTH_SHORT).show()
                    showDialog = false
                    cardNumber = ""; expiry = ""; cvv = ""
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

/**
 * Formulario y gestión para los Ajustes de Direcciones de Entrega.
 * Permite al usuario registrar o actualizar su dirección principal mediante un diálogo,
 * editando la información existente o eliminándola de la base de datos persistente.
 */
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

/**
 * Formulario para los Ajustes de Datos de la Cuenta del usuario.
 * Permite modificar campos clave como Nombre Completo, Teléfono y Correo Electrónico
 * y guardar los cambios directamente en el repositorio persistente.
 */
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
            val prefixVal = context.getString(R.string.colombia_prefix).trim()
            phone = it.phone?.removePrefix(prefixVal)?.trim() ?: ""
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
            onValueChange = { input -> phone = input.filter { it.isDigit() }.take(10) },
            label = { Text(stringResource(id = R.string.phone_label)) },
            prefix = { Text(stringResource(id = R.string.colombia_prefix)) },
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
                val cleanPhone = phone.filter { it.isDigit() }.take(10)
                if (cleanPhone.length != 10) {
                    Toast.makeText(context, context.getString(R.string.toast_invalid_phone), Toast.LENGTH_LONG).show()
                    return@Button
                }

                val prefixVal = context.getString(R.string.colombia_prefix)
                val formattedPhone = "$prefixVal$cleanPhone"
                val updatedProfile = profileFromDb?.copy(
                    name = name,
                    email = email,
                    phone = formattedPhone
                ) ?: com.example.herbhopper_v1.data.UserProfile(
                    uid = uid,
                    name = name,
                    email = email,
                    phone = formattedPhone,
                    role = "PATIENT"
                )

                android.util.Log.d("ProfileSettings", "Click en Guardar para UID: $uid")
                viewModel.saveProfile(updatedProfile)
                Toast.makeText(context, context.getString(R.string.toast_saving_changes), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(id = R.string.save_changes_btn), fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Panel de Ajustes de Notificaciones.
 * Proporciona interruptores (Switches) para configurar preferencias de avisos Push,
 * promociones por email, alertas de pedidos y alertas de seguridad.
 */
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

/**
 * Fila individual con interruptor Switch para una preferencia de notificación.
 *
 * @param title Nombre de la notificación o preferencia a activar/desactivar.
 * @param initial Estado booleano inicial del interruptor.
 */
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

/**
 * Componente visual reutilizable de tarjeta para representar una dirección o tarjeta de pago guardada.
 * Permite opcionalmente editar y/o borrar el registro mediante llamadas callback.
 *
 * @param title Título o etiqueta principal del registro.
 * @param subtitle Subtítulo o valor extendido (ej. dirección completa o máscara de tarjeta).
 * @param icon Icono ilustrativo de tipo [ImageVector].
 * @param onEdit Función callback opcional ejecutada al presionar el botón de editar.
 * @param onDelete Función callback ejecutada al presionar el botón de eliminar.
 */
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

/**
 * Transformación visual para tarjetas de crédito (agrupa dígitos de 4 en 4 separados por espacios).
 */
private class ProfileCreditCardTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0, 16) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i < 15) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                if (offset <= 12) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * Transformación visual para fecha de vencimiento en formato MM/AA.
 */
private class ProfileExpiryDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0, 4) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
