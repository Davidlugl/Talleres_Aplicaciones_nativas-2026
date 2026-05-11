package com.example.herbhopper_v1.ui.admin.crud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCrudScreen(viewModel: ProductViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Aceite") }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val products by viewModel.allProducts.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(id = R.string.product_crud_title)) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Form
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.product_name)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(id = R.string.price)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(id = R.string.category_hint)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(id = R.string.description_label)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        val product = Product(
                            id = editingProduct?.id ?: 0,
                            name = name,
                            price = price.toDoubleOrNull() ?: 0.0,
                            description = description,
                            category = category
                        )
                        if (editingProduct == null) {
                            viewModel.insert(product)
                        } else {
                            viewModel.update(product)
                        }
                        name = ""
                        price = ""
                        description = ""
                        category = "Aceite"
                        editingProduct = null
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editingProduct == null) stringResource(id = R.string.add_btn) else stringResource(id = R.string.update_btn))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // List
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(products) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                Text("${product.category} • $${String.format("%,.0f", product.price)} COP", style = MaterialTheme.typography.bodyMedium)
                            }
                            Row {
                                IconButton(onClick = {
                                    editingProduct = product
                                    name = product.name
                                    price = product.price.toString()
                                    description = product.description
                                    category = product.category
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit), tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.delete(product) }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
