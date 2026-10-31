package com.example.herbhopper_v1.ui.seller

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.viewmodel.ProductViewModel

/** Categorías del catálogo (las mismas que muestra el paciente). */
val PRODUCT_CATEGORIES = listOf(
    "Líquidos y Aceites", "Comestibles", "Tópicos", "Bienestar y Cuidado",
    "Concentrados y Otros", "Bebidas y Suplementos", "Comestibles Especializados"
)

/**
 * Crear / editar producto del inventario (CRUD del vendedor).
 * Además de nombre, precio, categoría e imagen (Cloudinary), registra el stock y su umbral mínimo
 * (para la alerta CRÍTICO) y la ficha técnica que ve el paciente: concentración, % de THC,
 * beneficios, modo de uso y certificaciones. Con THC ≥ 1 % el servidor marca el producto como
 * de fórmula médica obligatoria. Todo se guarda en el backend; si falla, se informa el error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCrudScreen(
    productId: Int? = null,
    onBack: () -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PRODUCT_CATEGORIES.first()) }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    var minStock by remember { mutableStateOf("5") }
    var cbdConcentration by remember { mutableStateOf("") }
    var thcPercent by remember { mutableStateOf("0") }
    var benefits by remember { mutableStateOf("") }
    var usageMode by remember { mutableStateOf("") }
    var certifications by remember { mutableStateOf("") }
    var requiresPrescription by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Carga el producto a editar
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.getProduct(productId)?.let { p ->
                name = p.name
                price = p.price.toLong().toString()
                category = p.category
                description = p.description ?: ""
                imageUrl = p.imageUrl ?: ""
                stock = p.stock.toString()
                minStock = p.minStock.toString()
                cbdConcentration = p.cbdConcentration ?: ""
                thcPercent = p.thcPercent.toString()
                benefits = p.benefits ?: ""
                usageMode = p.usageMode ?: ""
                certifications = p.certifications ?: ""
                requiresPrescription = p.requiresPrescription
            }
        }
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            isUploading = true
            viewModel.uploadProductImage(context, it) { uploadedUrl ->
                isUploading = false
                if (uploadedUrl != null) {
                    imageUrl = uploadedUrl
                    Toast.makeText(context, context.getString(R.string.image_uploaded_toast), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.image_upload_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (productId == null) stringResource(id = R.string.add_product_title)
                        else stringResource(id = R.string.edit_product_title),
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen del producto (Cloudinary o recurso local)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val isWebUrl = imageUrl.startsWith("http")
                val imageResId = remember(imageUrl) {
                    if (imageUrl.isNotEmpty() && !isWebUrl) {
                        val nameWithoutExt = imageUrl.substringBefore(".")
                        val finalResName = if (nameWithoutExt.all { it.isDigit() }) "prod_$nameWithoutExt" else nameWithoutExt
                        context.resources.getIdentifier(finalResName, "drawable", context.packageName)
                    } else 0
                }
                when {
                    isUploading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    isWebUrl -> coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    imageResId != 0 -> androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.tap_to_upload_image), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(id = R.string.product_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(id = R.string.price)) },
                prefix = { Text("$ ") },
                suffix = { Text("COP") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.category_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    PRODUCT_CATEGORIES.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(id = R.string.description_label)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            // Inventario y umbral de seguridad
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.stock_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minStock,
                    onValueChange = { minStock = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(id = R.string.min_stock_input_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Ficha técnica
            Text(stringResource(id = R.string.technical_sheet_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = cbdConcentration, onValueChange = { cbdConcentration = it }, label = { Text(stringResource(id = R.string.concentration)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(
                value = thcPercent,
                onValueChange = { thcPercent = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text(stringResource(id = R.string.thc_percent_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text(stringResource(id = R.string.thc_rule_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(value = benefits, onValueChange = { benefits = it }, label = { Text(stringResource(id = R.string.clinical_benefits)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(value = usageMode, onValueChange = { usageMode = it }, label = { Text(stringResource(id = R.string.usage_mode)) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(value = certifications, onValueChange = { certifications = it }, label = { Text(stringResource(id = R.string.certification)) }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = requiresPrescription, onCheckedChange = { requiresPrescription = it })
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.requires_prescription_chip))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val priceValue = price.toDoubleOrNull() ?: 0.0
                    val thcValue = thcPercent.toDoubleOrNull()
                    if (priceValue <= 0.0) {
                        Toast.makeText(context, context.getString(R.string.toast_price_positive), Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (thcValue == null || thcValue < 0 || thcValue > 100) {
                        Toast.makeText(context, context.getString(R.string.toast_thc_range), Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val product = Product(
                        id = productId ?: 0,
                        name = name.trim(),
                        price = priceValue,
                        category = category,
                        description = description.trim(),
                        imageUrl = imageUrl.ifBlank { null },
                        stock = stock.toIntOrNull() ?: 0,
                        minStock = minStock.toIntOrNull() ?: 0,
                        cbdConcentration = cbdConcentration.trim().ifBlank { null },
                        thcPercent = thcValue,
                        benefits = benefits.trim().ifBlank { null },
                        usageMode = usageMode.trim().ifBlank { null },
                        certifications = certifications.trim().ifBlank { null },
                        requiresPrescription = requiresPrescription
                    )
                    isSaving = true
                    val onResult: (String?) -> Unit = { error ->
                        isSaving = false
                        if (error == null) {
                            Toast.makeText(context, context.getString(R.string.product_saved_toast), Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                        }
                    }
                    if (productId == null) viewModel.insert(product, onResult) else viewModel.update(product, onResult)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && price.isNotBlank() && !isSaving && !isUploading
            ) {
                Text(
                    if (productId == null) stringResource(id = R.string.save_product)
                    else stringResource(id = R.string.update_product),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
