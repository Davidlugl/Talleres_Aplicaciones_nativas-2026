package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Store
import com.example.herbhopper_v1.data.StoreDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val storeDao: StoreDao = AppDatabase.getDatabase(application).storeDao()
    val allStores: Flow<List<Store>> = storeDao.getAllStores()

    init {
        seedStores()
    }

    private fun seedStores() {
        viewModelScope.launch {
            val initialStores = listOf(
                Store(name = "Botanical Precision", location = "Bogotá, Calle 100", licenseNumber = "LIC-9823", status = "VERIFIED"),
                Store(name = "Green Relief", location = "Medellín, El Poblado", licenseNumber = "LIC-4512", status = "PENDING"),
                Store(name = "Nature's Pharmacy", location = "Cali, Av. Sexta", licenseNumber = "LIC-7731", status = "VERIFIED")
            )
            initialStores.forEach { storeDao.insertStore(it) }
        }
    }

    fun insertStore(store: Store) {
        viewModelScope.launch {
            storeDao.insertStore(store)
        }
    }

    fun updateStore(store: Store) {
        viewModelScope.launch {
            storeDao.updateStore(store)
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch {
            storeDao.deleteStore(store)
        }
    }
}
