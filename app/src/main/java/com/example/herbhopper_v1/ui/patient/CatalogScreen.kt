package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.ui.components.PatientBottomNavBar
import com.example.herbhopper_v1.viewmodel.ProductViewModel

/**
 * Pantalla principal del Catálogo de Productos para pacientes.
 * Permite explorar y buscar formulaciones botánicas y productos clínicos por categorías.
 * Ofrece integración de búsqueda en tiempo real, filtrado por categorías mediante chips
 * horizontales, visualización de productos destacados, navegación al carrito de compras,
 * al menú lateral de opciones y barra de navegación inferior nativa del paciente.
 *
 * @param productViewModel ViewModel [ProductViewModel] para la gestión del catálogo de productos y filtros.
 * @param onProductClick Función callback al seleccionar un producto para ver sus detalles.
 * @param onAddToCart Función callback para agregar directamente un producto al carrito de compras.
 * @param onMenuClick Función callback al abrir el menú lateral o menú de navegación.
 * @param onCartClick Función callback para navegar a la pantalla del carrito de compras.
 * @param onHomeClick Función callback para la navegación al inicio (Home) de la barra inferior.
 * @param onOrdersClick Función callback para la navegación al historial de órdenes de la barra inferior.
 * @param onScriptsClick Función callback para la navegación a recetas/validaciones de la barra inferior.
 * @param onProfileClick Función callback para la navegación al perfil de la barra inferior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    productViewModel: ProductViewModel,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onMenuClick: () -> Unit,
    onCartClick: () -> Unit,
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // Solución al Issue 6: Obtener el estado de productos filtrados y categoría seleccionada
    // directamente del ViewModel para evitar recálculos en la capa UI.
    val filteredProducts by productViewModel.filteredProducts.collectAsState()
    val selectedCategory by productViewModel.selectedCategory.collectAsState()
    val searchQuery by productViewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) }
                },
                actions = {
                    IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingBasket, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            PatientBottomNavBar(
                currentRoute = "home",
                onHomeClick = onHomeClick,
                onOrdersClick = onOrdersClick,
                onScriptsClick = onScriptsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        // Solución al Layout Native: Usar LazyVerticalGrid nativo con GridItemSpan para headers y cards
        // evitando el hack manual de chunked(2) y optimizando el renderizado de la grilla.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header content (Sección que ocupa todo el ancho de la pantalla)
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { productViewModel.setSearchQuery(it) },
                        placeholder = { Text(stringResource(id = R.string.search_formulations)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Categories (usando stringResource() para cumplir con la política estricta de localización)
                    val categories = listOf(
                        "Todos" to R.string.all,
                        "Líquidos y Aceites" to R.string.oil,
                        "Comestibles" to R.string.capsules,
                        "Tópicos" to R.string.creams,
                        "Bienestar y Cuidado" to R.string.extracts,
                        "Concentrados y Otros" to R.string.flower,
                        "Bebidas y Suplementos" to R.string.category_bebidas,
                        "Comestibles Especializados" to R.string.category_especializados
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(categories) { pair ->
                            val categoryKey = pair.first
                            val labelRes = pair.second
                            FilterChip(
                                selected = selectedCategory == categoryKey,
                                onClick = { productViewModel.setCategory(categoryKey) },
                                label = { Text(stringResource(id = labelRes)) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Featured Card - Only show when "Todos" is selected
            if (selectedCategory == "Todos") {
                item(span = { GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)))
                                Column(modifier = Modifier.padding(24.dp).align(Alignment.BottomStart)) {
                                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                                        Text(stringResource(id = R.string.staff_pick), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Text(stringResource(id = R.string.aura_blend_special), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // Grid Header
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text(stringResource(id = R.string.catalog), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(stringResource(id = R.string.catalog_subtitle), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Products items / empty state
            if (filteredProducts.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.no_products_available), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(
                    items = filteredProducts,
                    key = { it.id }
                ) { product ->
                    ProductCard(
                        product = product,
                        modifier = Modifier.padding(
                            start = if (filteredProducts.indexOf(product) % 2 == 0) 24.dp else 0.dp,
                            end = if (filteredProducts.indexOf(product) % 2 != 0) 24.dp else 0.dp
                        ),
                        onClick = { onProductClick(product) },
                        onAddClick = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual que muestra la información de un producto dentro del catálogo.
 * Renderiza la imagen del producto, su nombre, precio formateado y un botón de acceso rápido para agregarlo al carrito.
 *
 * @param product El objeto de datos [Product] con la información del artículo a mostrar.
 * @param modifier Modificador de Compose para personalizar el diseño externo de la tarjeta.
 * @param onClick Función callback que se ejecuta al pulsar sobre el cuerpo de la tarjeta (para ver detalles).
 * @param onAddClick Función callback que se ejecuta al pulsar sobre el botón de agregar al carrito.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val imageResId = remember(product.imageUrl) {
                if (!product.imageUrl.isNullOrEmpty()) {
                    context.resources.getIdentifier(product.imageUrl, "drawable", context.packageName)
                } else {
                    0
                }
            }

            Box(modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                if (imageResId != 0) {
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
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(product.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            Text("$${String.format("%,.0f", product.price)} COP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp).align(Alignment.End).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
