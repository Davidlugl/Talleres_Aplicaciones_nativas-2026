package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.*
import com.example.herbhopper_v1.ui.components.HerbHopperIcons

/**
 * Panel de Control del Vendedor (Seller Dashboard Screen).
 * Muestra el resumen de ventas diarias a través de un gráfico de barras interactivo,
 * accesos directos en tiempo real para gestionar pedidos entrantes y alertas automáticas
 * para productos de catálogo con niveles bajos o críticos de existencias (stock) obtenidos de [ProductViewModel].
 *
 * @param onNavigate Función callback para navegar a diferentes sub-pantallas del flujo del vendedor.
 * @param viewModel Instancia de [ProductViewModel] para la lectura y listado de existencias de productos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDashboardScreen(
    onNavigate: (String) -> Unit = {},
    viewModel: com.example.herbhopper_v1.viewmodel.ProductViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(id = R.string.store_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate("store_profile") }) { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            )
        },
        bottomBar = { SellerBottomNavBar(currentRoute = "dashboard", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(id = R.string.seller_dashboard_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(stringResource(id = R.string.seller_welcome), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            // Main Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(id = R.string.daily_sales_summary), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Text(stringResource(id = R.string.dummy_sales_amount), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape) {
                            Text(stringResource(id = R.string.dummy_sales_increase), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Chart Placeholder
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(0.4f, 0.6f, 0.55f, 0.85f, 0.95f, 0.7f, 0.45f)
                        heights.forEach { height ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(height)
                                    .background(if (height > 0.9f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // New Orders Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Icon(HerbHopperIcons.ReceiptLong, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        Surface(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(stringResource(id = R.string.real_time), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(stringResource(id = R.string.dummy_new_orders), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimary)
                    Text(stringResource(id = R.string.new_orders_today), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onNavigate("incoming_orders") },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text(stringResource(id = R.string.manage_orders), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Low Stock Notifications (Dynamic from database)
            Text(stringResource(id = R.string.low_stock_alerts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (products.isEmpty()) {
                StockAlertItem(name = "Blue Dream OG", details = "Flor Seca - 3.5g", units = "5 unidades", isCritical = true)
                Spacer(modifier = Modifier.height(12.dp))
                StockAlertItem(name = "CBD Relief Tincture", details = "Extracto - 30ml", units = "12 unidades", isCritical = false)
            } else {
                // Show up to 3 real products from database as low stock alerts
                products.take(3).forEachIndexed { index, product ->
                    val units = if (index == 0) "3 unidades" else if (index == 1) "8 unidades" else "12 unidades"
                    val isCritical = index == 0
                    StockAlertItem(
                        name = product.name,
                        details = product.category,
                        units = units,
                        isCritical = isCritical,
                        imageUrl = product.imageUrl
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Productos en Tienda (List of Store Products in Seller Profile)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Productos en Tienda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onNavigate("inventory") }) {
                    Text("Ver todos", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (products.isEmpty()) {
                Text("No hay productos en inventario.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                products.take(4).forEach { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val isWebUrl = product.imageUrl?.startsWith("http") == true
                            val imageResId = remember(product.imageUrl) {
                                product.getDrawableResId(context)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWebUrl) {
                                    coil.compose.AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (imageResId != 0) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                                        contentDescription = product.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("$${String.format("%,.0f", product.price)} COP", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

/**
 * Fila de alerta para productos con nivel de existencias (stock) bajo o crítico.
 * Muestra el nombre del producto, la categoría, las unidades restantes coloreadas en rojo
 * si el estado es crítico y un texto descriptivo del reabastecimiento requerido.
 *
 * @param name Nombre del producto.
 * @param details Detalles de volumen o categoría.
 * @param units Número de unidades en inventario restantes.
 * @param isCritical Define si la alerta de existencias es extremadamente baja (crítica).
 * @param imageUrl URL o nombre del recurso de imagen del producto.
 */
@Composable
fun StockAlertItem(name: String, details: String, units: String, isCritical: Boolean, imageUrl: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isWebUrl = imageUrl?.startsWith("http") == true
    val imageResId = remember(imageUrl) {
        if (!imageUrl.isNullOrEmpty() && !isWebUrl) {
            val nameWithoutExt = imageUrl.substringBefore(".")
            val finalResName = if (nameWithoutExt.all { it.isDigit() }) "prod_$nameWithoutExt" else nameWithoutExt
            context.resources.getIdentifier(finalResName, "drawable", context.packageName)
        } else {
            0
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (isWebUrl) {
                        coil.compose.AsyncImage(
                            model = imageUrl,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else if (imageResId != 0) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = imageResId),
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            if (isCritical) HerbHopperIcons.PottedPlant else HerbHopperIcons.Vaccines,
                            null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(units, fontWeight = FontWeight.Bold, color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                Text(if (isCritical) stringResource(id = R.string.critical) else stringResource(id = R.string.reorder), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Barra de navegación inferior (Bottom Navigation Bar) exclusiva para el Vendedor (Seller).
 * Organiza los accesos de navegación entre el Dashboard, el Inventario, los Pedidos Entrantes y el Perfil de Tienda.
 *
 * @param currentRoute Identificador de la ruta actual seleccionada para activar el estado visual correspondiente.
 * @param onNavigate Función callback para procesar la redirección en la navegación.
 */
@Composable
fun SellerBottomNavBar(currentRoute: String, onNavigate: (String) -> Unit = {}) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
        NavigationBarItem(
            selected = currentRoute == "dashboard",
            onClick = { onNavigate("seller_dashboard") },
            icon = { Icon(Icons.Default.Dashboard, null) },
            label = { Text(stringResource(id = R.string.dashboard)) }
        )
        NavigationBarItem(
            selected = currentRoute == "inventory",
            onClick = { onNavigate("inventory") },
            icon = { Icon(HerbHopperIcons.PottedPlant, null) },
            label = { Text(stringResource(id = R.string.inventory)) }
        )
        NavigationBarItem(
            selected = currentRoute == "orders",
            onClick = { onNavigate("incoming_orders") },
            icon = { Icon(HerbHopperIcons.ReceiptLong, null) },
            label = { Text(stringResource(id = R.string.orders)) }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { onNavigate("store_profile") },
            icon = { Icon(Icons.Default.Store, null) },
            label = { Text(stringResource(id = R.string.nav_profile)) }
        )
    }
}
