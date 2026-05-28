package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.viewmodel.ProductViewModel

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.CloudUpload

/**
 * Pantalla de Creación y Edición del Inventario (Inventory CRUD Screen).
 * Ofrece un formulario interactivo para registrar un nuevo producto o actualizar
 * los detalles de un producto existente. Realiza la comprobación del producto cargándolo
 * desde la lista de [ProductViewModel] y actualiza los campos Nombre, Precio, Categoría y Descripción,
 * guardando los cambios de forma reactiva en la base de datos local.
 *
 * @param productId Identificador del producto a editar; si es null, la pantalla se comporta en modo de inserción/creación.
 * @param onBack Función callback para retornar a la pantalla de inventario.
 * @param viewModel Instancia de [ProductViewModel] para la persistencia e inserción/actualización de productos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCrudScreen(
    productId: Int? = null,
    onBack: () -> Unit,
    viewModel: ProductViewModel = viewModel()
) {
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    val existingProduct = products.find { it.id == productId }

    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(existingProduct?.name ?: "") }
    var price by remember { mutableStateOf(existingProduct?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(existingProduct?.category ?: "General") }
    var description by remember { mutableStateOf(existingProduct?.description ?: "") }
    var imageUrl by remember { mutableStateOf(existingProduct?.imageUrl ?: "") }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            isUploading = true
            viewModel.uploadProductImage(context, it) { uploadedUrl ->
                isUploading = false
                if (uploadedUrl != null) {
                    imageUrl = uploadedUrl
                    android.widget.Toast.makeText(context, "¡Imagen subida a Cloudinary exitosamente!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Error al subir la imagen", android.widget.Toast.LENGTH_SHORT).show()
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
                .verticalScroll(rememberScrollState())
        ) {
            // Visor y Cargador de Imagen (Cloudinary / Local)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                val isWebUrl = imageUrl.startsWith("http")
                val imageResId = remember(imageUrl) {
                    if (!imageUrl.isNullOrEmpty() && !isWebUrl) {
                        val nameWithoutExt = imageUrl.substringBefore(".")
                        val finalResName = if (nameWithoutExt.all { it.isDigit() }) "prod_$nameWithoutExt" else nameWithoutExt
                        context.resources.getIdentifier(finalResName, "drawable", context.packageName)
                    } else {
                        0
                    }
                }

                if (isUploading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (isWebUrl) {
                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Toca para cargar imagen a Cloudinary",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(id = R.string.price)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(id = R.string.category_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(id = R.string.description_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    val product = Product(
                        id = productId ?: 0,
                        name = name,
                        price = price.toDoubleOrNull() ?: 0.0,
                        category = category,
                        description = description,
                        imageUrl = imageUrl
                    )
                    if (productId == null) {
                        viewModel.insert(product)
                    } else {
                        viewModel.update(product)
                    }
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = name.isNotBlank() && price.isNotBlank()
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
