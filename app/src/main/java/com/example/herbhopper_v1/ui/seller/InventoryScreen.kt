package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.herbhopper_v1.data.Product
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.components.HerbHopperIcons

/**
 * Pantalla de Inventario General del Vendedor (Inventory Screen).
 * Muestra una lista de todos los productos de catálogo, permitiendo al vendedor
 * agregar nuevos artículos, editar la información de los existentes o eliminarlos
 * de la base de datos de manera reactiva mediante el uso de [ProductViewModel].
 *
 * @param onBack Función callback para volver a la pantalla anterior.
 * @param onNavigate Función callback para navegar a otras secciones del vendedor.
 * @param onAddProduct Función callback para ir al panel de creación de nuevo producto.
 * @param onEditProduct Función callback para ir al panel de edición del producto mediante su identificador.
 * @param viewModel Instancia de [ProductViewModel] para la consulta y eliminación de artículos del catálogo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onAddProduct: () -> Unit = {},
    onEditProduct: (Int) -> Unit = {},
    viewModel: com.example.herbhopper_v1.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.inventory), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onAddProduct) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.add_btn))
                    }
                }
            )
        },
        bottomBar = { 
            SellerBottomNavBar(
                currentRoute = "inventory",
                onNavigate = onNavigate
            ) 
        }
    ) { padding ->
        val inventoryItems by viewModel.allProducts.collectAsState(initial = emptyList())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(inventoryItems) { item ->
                InventoryCard(item, onEditProduct, onDelete = { viewModel.delete(item) })
            }
        }
    }
}

/**
 * Tarjeta individual de producto para el inventario del vendedor.
 * Carga de forma segura y dinámica la imagen asociada al producto desde recursos locales,
 * y muestra el nombre, categoría, precio formateado y accesos rápidos para editar o eliminar.
 *
 * @param item El objeto [Product] a representar.
 * @param onEdit Función callback para despachar la edición del producto.
 * @param onDelete Función callback para despachar la eliminación del producto.
 */
@Composable
fun InventoryCard(item: Product, onEdit: (Int) -> Unit, onDelete: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageResId = remember(item.imageUrl) {
        if (!item.imageUrl.isNullOrEmpty()) {
            context.resources.getIdentifier(item.imageUrl, "drawable", context.packageName)
        } else {
            0
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$${String.format("%,.0f", item.price)} COP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row {
                IconButton(onClick = { onEdit(item.id) }) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
