package com.example.herbhopper_v1.ui.patient

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.DeliveryAddress
import com.example.herbhopper_v1.data.SessionManager
import com.example.herbhopper_v1.viewmodel.DeliveryAddressViewModel
import com.example.herbhopper_v1.viewmodel.GpsState
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel

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

    val uid = com.example.herbhopper_v1.data.SessionManager.getUid()

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

                    val formattedMethod = "Tarjeta **** " + cardNumber.takeLast(4)
                    val updated = profileFromDb?.copy(paymentMethod = formattedMethod) ?: com.example.herbhopper_v1.data.UserProfile(
                        uid = uid,
                        name = com.example.herbhopper_v1.data.SessionManager.getName().ifEmpty { "Usuario" },
                        email = com.example.herbhopper_v1.data.SessionManager.getEmail(),
                        role = "PATIENT",
                        paymentMethod = formattedMethod
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
 * Pantalla de gestión de Direcciones de Entrega.
 *
 * Permite al usuario:
 * - Ver todas sus direcciones guardadas.
 * - Agregar nuevas direcciones manualmente o usando su ubicación GPS actual.
 * - Editar y eliminar direcciones existentes.
 * - Marcar una dirección como predeterminada.
 *
 * Usa [DeliveryAddressViewModel] para el acceso a Room y el proveedor de ubicación.
 */
@Composable
fun AddressSettings() {
    val context = LocalContext.current
    val addressVM: DeliveryAddressViewModel = viewModel()
    val uid = SessionManager.getUid()

    val addresses by addressVM.observeAddresses(uid).collectAsState(initial = emptyList())
    val gpsState by addressVM.gpsState.collectAsState()

    // ── Estado del diálogo ────────────────────────────────────────────────────
    var showDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<DeliveryAddress?>(null) }
    var labelText by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var savedLat by remember { mutableStateOf<Double?>(null) }
    var savedLon by remember { mutableStateOf<Double?>(null) }

    // ── Launcher de permisos de ubicación ─────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            addressVM.fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Cuando GPS retorna un resultado, pre-rellena el diálogo ───────────────
    LaunchedEffect(gpsState) {
        if (gpsState is GpsState.Success) {
            val s = gpsState as GpsState.Success
            addressText = s.addressText
            savedLat = s.latitude
            savedLon = s.longitude
        } else if (gpsState is GpsState.Error) {
            Toast.makeText(context, (gpsState as GpsState.Error).message, Toast.LENGTH_LONG).show()
            addressVM.resetGpsState()
        }
    }

    // ── Diálogo Agregar / Editar ──────────────────────────────────────────────
    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                addressVM.resetGpsState()
            },
            title = {
                Text(
                    if (editingAddress == null) "Nueva dirección de entrega" else "Editar dirección",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // Etiqueta
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        label = { Text("Etiqueta (ej. Casa, Trabajo)") },
                        leadingIcon = { Icon(Icons.Default.Label, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Dirección completa
                    OutlinedTextField(
                        value = addressText,
                        onValueChange = {
                            addressText = it
                            // Si el usuario escribe manualmente, borramos las coords GPS
                            savedLat = null
                            savedLon = null
                        },
                        label = { Text("Dirección completa") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )

                    // Botón "Usar mi ubicación actual"
                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = gpsState !is GpsState.Loading
                    ) {
                        if (gpsState is GpsState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Obteniendo ubicación...")
                        } else {
                            Icon(Icons.Default.MyLocation, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Usar mi ubicación actual")
                        }
                    }

                    // Coordenadas mostradas si se obtuvo GPS
                    if (savedLat != null && savedLon != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "GPS: ${"%.4f".format(savedLat)}, ${
                                    "%.4f".format(savedLon)
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Predeterminada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Establecer como predeterminada", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lbl = labelText.ifBlank { "Dirección" }
                        if (addressText.isBlank()) {
                            Toast.makeText(context, "Ingresa una dirección", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val addr = editingAddress?.copy(
                            label = lbl,
                            fullAddress = addressText,
                            latitude = savedLat,
                            longitude = savedLon,
                            isDefault = isDefault
                        ) ?: DeliveryAddress(
                            uid = uid,
                            label = lbl,
                            fullAddress = addressText,
                            latitude = savedLat,
                            longitude = savedLon,
                            isDefault = isDefault
                        )
                        addressVM.saveAddress(addr)
                        Toast.makeText(context, "Dirección guardada", Toast.LENGTH_SHORT).show()
                        showDialog = false
                        addressVM.resetGpsState()
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    addressVM.resetGpsState()
                }) { Text("Cancelar") }
            }
        )
    }

    // ── Lista de direcciones ──────────────────────────────────────────────────
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Direcciones de entrega",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (addresses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No tienes direcciones guardadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            addresses.forEach { addr ->
                DeliveryAddressCard(
                    address = addr,
                    onEdit = {
                        editingAddress = addr
                        labelText = addr.label
                        addressText = addr.fullAddress
                        isDefault = addr.isDefault
                        savedLat = addr.latitude
                        savedLon = addr.longitude
                        showDialog = true
                    },
                    onDelete = { addressVM.deleteAddress(addr.id) },
                    onSetDefault = { addressVM.setDefault(uid, addr.id) }
                )
            }
        }

        Button(
            onClick = {
                editingAddress = null
                labelText = ""
                addressText = ""
                isDefault = addresses.isEmpty() // primera dirección = predeterminada automáticamente
                savedLat = null
                savedLon = null
                addressVM.resetGpsState()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddLocationAlt, null)
            Spacer(Modifier.width(8.dp))
            Text("Agregar nueva dirección")
        }
    }
}

/**
 * Tarjeta visual para una [DeliveryAddress] individual.
 * Muestra etiqueta, dirección, badge GPS si tiene coordenadas, y acciones de
 * edición, borrado y marcado como predeterminada.
 */
@Composable
fun DeliveryAddressCard(
    address: DeliveryAddress,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (address.isDefault)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (address.isDefault)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (address.isDefault)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    address.label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (address.isDefault) {
                    Spacer(Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "Predeterminada",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Text(
                address.fullAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Badge GPS si tiene coordenadas
            if (address.latitude != null && address.longitude != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "GPS verificado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!address.isDefault) {
                    TextButton(onClick = onSetDefault) {
                        Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Predeterminar", style = MaterialTheme.typography.labelMedium)
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
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

    val uid = com.example.herbhopper_v1.data.SessionManager.getUid()

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
