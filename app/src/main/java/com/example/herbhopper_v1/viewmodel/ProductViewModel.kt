package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.data.ProductDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productDao: ProductDao = AppDatabase.getDatabase(application).productDao()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    // Solución al Issue 6: Mover la lógica de filtrado al ViewModel para ser testeable
    // y evitar recálculos costosos en cada frame de la UI.
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredProducts: StateFlow<List<Product>> = combine(
        allProducts,
        _selectedCategory,
        _searchQuery
    ) { products, category, query ->
        val categoryFiltered = if (category == "Todos") {
            products
        } else {
            products.filter { it.category == category }
        }
        if (query.isBlank()) {
            categoryFiltered
        } else {
            categoryFiltered.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.description.contains(query, ignoreCase = true) 
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
        seedDatabase()
        refreshProductsFromBackend()
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            val existing = productDao.getAllProducts().first()
            val isCatalogPresent = existing.any { it.name == "Aceite de CBD Full Spectrum" }
            if (!isCatalogPresent) {
                productDao.deleteAllProducts()
                val initialProducts = listOf(
                    // Líquidos y Aceites (Tincturas y Gotas)
                    Product(name = "Aceite de CBD Full Spectrum", description = "El formato más común para administrar gotas sublinguales.", price = 65000.0, category = "Líquidos y Aceites", imageUrl = "prod_1"),
                    Product(name = "Nano-emulsiones de agua", description = "Líquidos solubles en agua que se pueden mezclar con cualquier bebida.", price = 75000.0, category = "Líquidos y Aceites", imageUrl = "prod_2"),
                    Product(name = "Jarabe de CBD/THC", description = "Utilizado a menudo para ayudar a conciliar el sueño (estilo \"lean\" medicinal).", price = 85000.0, category = "Líquidos y Aceites", imageUrl = "prod_3"),
                    Product(name = "Sprays Sublinguales", description = "Rocíos que se aplican debajo de la lengua para una absorción rápida.", price = 58000.0, category = "Líquidos y Aceites", imageUrl = "prod_4"),

                    // Comestibles (Dulces y Alimentos)
                    Product(name = "Gomitas (Gummies)", description = "De sabores frutales, generalmente con dosis exactas de 10mg o 25mg.", price = 42000.0, category = "Comestibles", imageUrl = "prod_5"),
                    Product(name = "Chocolates Artesanales", description = "Tabletas de chocolate negro o con leche con infusiones de cannabinoides.", price = 48000.0, category = "Comestibles", imageUrl = "prod_6"),
                    Product(name = "Cápsulas de Gel (Softgels)", description = "Similares a las de aceite de pescado, ideales para quienes no quieren sentir el sabor de la planta.", price = 55000.0, category = "Comestibles", imageUrl = "prod_7"),
                    Product(name = "Miel de Cannabis", description = "Miel natural infusionada, usada como endulzante medicinal.", price = 62000.0, category = "Comestibles", imageUrl = "prod_8"),
                    Product(name = "Mentas Medicinales", description = "Pequeñas pastillas para microdosis discretas durante el día.", price = 35000.0, category = "Comestibles", imageUrl = "prod_9"),

                    // Tópicos (Uso Externo)
                    Product(name = "Bálsamos para Dolor Muscular", description = "Cremas con efecto frío/calor para atletas o personas con artritis.", price = 59000.0, category = "Tópicos", imageUrl = "prod_10"),
                    Product(name = "Parches Transdérmicos", description = "Se pegan en la piel y liberan el medicamento de forma constante durante 12-24 horas.", price = 68000.0, category = "Tópicos", imageUrl = "prod_11"),
                    Product(name = "Aceites para Masaje", description = "Enriquecidos con CBD para relajación profunda sin efectos psicoactivos.", price = 72000.0, category = "Tópicos", imageUrl = "prod_12"),
                    Product(name = "Lubricantes Medicinales", description = "Diseñados para reducir el dolor o aumentar la sensibilidad.", price = 80000.0, category = "Tópicos", imageUrl = "prod_13"),

                    // Bienestar y Cuidado Personal
                    Product(name = "Sales de Baño (Epsom)", description = "Para baños de inmersión relajantes y desinflamatorios.", price = 45000.0, category = "Bienestar y Cuidado", imageUrl = "prod_14"),
                    Product(name = "Bombas de Baño", description = "Al contacto con el agua liberan aceites esenciales y cannabinoides.", price = 38000.0, category = "Bienestar y Cuidado", imageUrl = "prod_15"),
                    Product(name = "Mascarillas Faciales", description = "Utilizadas en dermatología medicinal para reducir la inflamación cutánea.", price = 32000.0, category = "Bienestar y Cuidado", imageUrl = "prod_16"),

                    // Formatos Concentrados y Otros
                    Product(name = "Destilado en Jeringas", description = "Un aceite muy puro que se puede comer directamente o usar en recetas.", price = 110000.0, category = "Concentrados y Otros", imageUrl = "prod_17"),
                    Product(name = "Inhaladores", description = "Similares a los del asma, entregan una dosis precisa de vapor sin combustión.", price = 95000.0, category = "Concentrados y Otros", imageUrl = "prod_18"),
                    Product(name = "Supositorios", description = "Utilizados para pacientes con problemas gastrointestinales graves o dolores pélvicos crónicos.", price = 88000.0, category = "Concentrados y Otros", imageUrl = "prod_19"),
                    Product(name = "Polvos Hidrosolubles", description = "Sobres de polvo que se disuelven en agua, ideales para llevar fuera de casa.", price = 60000.0, category = "Concentrados y Otros", imageUrl = "prod_20"),

                    // Bebidas y Suplementos Líquidos
                    Product(name = "Cerveza sin alcohol con CBD", description = "Bebidas refrescantes que buscan el efecto relajante sin la resaca del alcohol.", price = 25000.0, category = "Bebidas y Suplementos", imageUrl = "prod_21"),
                    Product(name = "Kombucha infusionada", description = "Mezcla de probióticos y cannabis para la salud digestiva.", price = 22000.0, category = "Bebidas y Suplementos", imageUrl = "prod_22"),
                    Product(name = "Café en grano con CBD", description = "Diseñado para obtener la alerta del café pero reduciendo la ansiedad o \"temblor\" de la cafeína.", price = 45000.0, category = "Bebidas y Suplementos", imageUrl = "prod_23"),
                    Product(name = "Té de hierbas (Tisanas)", description = "Bolsitas de té con flores de cáñamo y otras plantas medicinales como manzanilla o valeriana.", price = 30000.0, category = "Bebidas y Suplementos", imageUrl = "prod_24"),
                    Product(name = "Shots de energía", description = "Pequeñas dosis líquidas que combinan CBD con vitamina B12 y cafeína.", price = 18000.0, category = "Bebidas y Suplementos", imageUrl = "prod_25"),

                    // Comestibles Especializados
                    Product(name = "Mantequilla (Cannabutter)", description = "Base lista para cocinar o untar, utilizada en dietas medicinales específicas.", price = 78000.0, category = "Comestibles Especializados", imageUrl = "prod_26"),
                    Product(name = "Caramelos macizos", description = "Ideales para una absorción lenta a través de la mucosa bucal.", price = 28000.0, category = "Comestibles Especializados", imageUrl = "prod_27"),
                    Product(name = "Aceite de Oliva infusionado", description = "Para uso directo en ensaladas o platos fríos como suplemento nutricional.", price = 85000.0, category = "Comestibles Especializados", imageUrl = "prod_28"),
                    Product(name = "Harina de Cáñamo con CBD", description = "Utilizada en repostería funcional para pacientes que requieren consumo constante.", price = 40000.0, category = "Comestibles Especializados", imageUrl = "prod_29"),
                    Product(name = "Chicles medicinales", description = "Permiten una liberación rápida de los compuestos mientras se mastica.", price = 32000.0, category = "Comestibles Especializados", imageUrl = "prod_30")
                )
                initialProducts.forEach { productDao.insertProduct(it) }
            }
        }
    }

    fun refreshProductsFromBackend() {
        viewModelScope.launch {
            try {
                val networkProducts = com.example.herbhopper_v1.data.network.ApiService.instance.getProducts()
                if (networkProducts.isNotEmpty()) {
                    networkProducts.forEach { networkProd ->
                        productDao.insertProduct(networkProd)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun uploadProductImage(context: android.content.Context, uri: android.net.Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri) ?: throw java.io.IOException("No se pudo abrir el stream")
                val tempFile = java.io.File.createTempFile("upload_", ".jpg", context.cacheDir)
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                
                val mediaType = "image/*".toMediaTypeOrNull()
                val requestFile = tempFile.asRequestBody(mediaType)
                val body = okhttp3.MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                
                val response = com.example.herbhopper_v1.data.network.ApiService.instance.uploadImage(body)
                tempFile.delete()
                onResult(response.url)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun insert(product: Product) {
        viewModelScope.launch {
            try {
                val createdProduct = com.example.herbhopper_v1.data.network.ApiService.instance.createProduct(product)
                productDao.insertProduct(createdProduct)
            } catch (e: Exception) {
                e.printStackTrace()
                productDao.insertProduct(product)
            }
        }
    }

    fun update(product: Product) {
        viewModelScope.launch {
            productDao.updateProduct(product)
        }
    }

    fun delete(product: Product) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }
}
