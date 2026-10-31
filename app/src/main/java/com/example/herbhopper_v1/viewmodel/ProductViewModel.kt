package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.data.ProductDao
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.ReorderRequest
import com.example.herbhopper_v1.data.network.apiErrorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Catálogo e inventario. El backend es la fuente de verdad; Room guarda una copia
 * para mostrar el catálogo sin conexión.
 */
class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productDao: ProductDao = AppDatabase.getDatabase(application).productDao()
    private val api = ApiService.instance

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    /** Productos con stock en nivel crítico (para el dashboard del vendedor). */
    val criticalProducts: StateFlow<List<Product>> = allProducts.map { products ->
        products.filter { it.isCriticalStock }.sortedBy { it.stock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        val categoryFiltered = if (category == "Todos") products else products.filter { it.category == category }
        if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (it.description ?: "").contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    init {
        refreshProductsFromBackend()
    }

    fun refreshProductsFromBackend() {
        viewModelScope.launch {
            try {
                productDao.replaceAll(api.getProducts())
            } catch (e: Exception) {
                // Sin conexión: se sigue mostrando la copia local
                e.printStackTrace()
            }
        }
    }

    suspend fun getProduct(id: Int): Product? = productDao.getProductById(id)

    fun uploadProductImage(context: android.content.Context, uri: android.net.Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw java.io.IOException("No se pudo abrir el stream")
                val tempFile = java.io.File.createTempFile("upload_", ".jpg", context.cacheDir)
                tempFile.outputStream().use { output -> inputStream.use { it.copyTo(output) } }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val body = okhttp3.MultipartBody.Part.createFormData("image", tempFile.name, requestFile)

                val response = api.uploadImage(body)
                tempFile.delete()
                onResult(response.url)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    /** Crea el producto en el servidor. [onResult] recibe null si todo salió bien o el mensaje de error. */
    fun insert(product: Product, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                productDao.insertProduct(api.createProduct(product))
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    fun update(product: Product, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                productDao.insertProduct(api.updateProduct(product.id, product))
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    fun delete(product: Product, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.deleteProduct(product.id)
                productDao.deleteProduct(product)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    /** Botón "Reordenar": envía la solicitud de reabastecimiento al administrador. */
    fun requestRestock(product: Product, quantity: Int, onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(api.reorderProduct(product.id, ReorderRequest(quantity)).message)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }
}
