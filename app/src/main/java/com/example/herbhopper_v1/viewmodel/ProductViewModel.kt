package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.data.ProductDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val productDao: ProductDao = AppDatabase.getDatabase(application).productDao()
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()

    init {
        seedDatabase()
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            // Seeding directly to ensure data exists
            val initialProducts = listOf(
                Product(name = "Aceite Aura Blend 15%", description = "Aceite CBD de espectro completo 1500mg.", price = 64.50, category = "Aceite", imageUrl = "url_aceite"),
                Product(name = "Silver Haze Premium", description = "Flor curada de grado medicinal.", price = 45.00, category = "Flor", imageUrl = "url_flor"),
                Product(name = "Cápsulas Night Cap", description = "Cápsulas para el descanso nocturno.", price = 32.00, category = "Cápsulas", imageUrl = "url_capsulas"),
                Product(name = "Crema Relief Pro", description = "Crema tópica antiinflamatoria.", price = 28.50, category = "Cremas", imageUrl = "url_crema"),
                Product(name = "Extracto de Menta", description = "Extracto botánico digestivo.", price = 24.50, category = "Extractos", imageUrl = "url_extracto"),
                Product(name = "Morning Mist Spray", description = "Spray sublingual energizante.", price = 35.00, category = "Extractos", imageUrl = "url_spray")
            )
            initialProducts.forEach { productDao.insertProduct(it) }
        }
    }

    fun insert(product: Product) {
        viewModelScope.launch {
            productDao.insertProduct(product)
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
