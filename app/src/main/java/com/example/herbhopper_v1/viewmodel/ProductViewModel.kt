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
                Product(name = "Aceite Aura Blend 15%", description = "Aceite CBD de espectro completo 1500mg.", price = 64500.0, category = "Aceite", imageUrl = "url_aceite"),
                Product(name = "Flor Silver Haze Premium", description = "Flor curada de grado medicinal.", price = 45000.0, category = "Flor", imageUrl = "url_flor"),
                Product(name = "Cápsulas Night Cap", description = "Cápsulas para el descanso nocturno.", price = 32000.0, category = "Cápsulas", imageUrl = "url_capsulas"),
                Product(name = "Crema Relief Pro", description = "Crema tópica antiinflamatoria.", price = 28500.0, category = "Cremas", imageUrl = "url_crema"),
                Product(name = "Extracto de Menta", description = "Extracto botánico digestivo.", price = 24500.0, category = "Extractos", imageUrl = "url_extracto"),
                Product(name = "Spray Morning Mist", description = "Spray sublingual energizante.", price = 35000.0, category = "Extractos", imageUrl = "url_spray")
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
