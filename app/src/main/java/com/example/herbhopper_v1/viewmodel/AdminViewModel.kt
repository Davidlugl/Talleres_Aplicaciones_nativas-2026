package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.Store
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.StoreDto
import com.example.herbhopper_v1.data.network.apiErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Gobernanza de la red de sucursales. Las sucursales viven en la base de datos central
 * (/api/stores), así que todos los administradores y dispositivos ven la misma lista.
 */
class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val api = ApiService.instance

    private val _allStores = MutableStateFlow<List<Store>>(emptyList())
    val allStores: StateFlow<List<Store>> = _allStores

    /** Porcentaje de integridad de la base de datos central (null mientras carga). */
    private val _integrityPercent = MutableStateFlow<Int?>(null)
    val integrityPercent: StateFlow<Int?> = _integrityPercent

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _allStores.value = api.getStores().map { it.toStore() }
                _integrityPercent.value = api.getSystemHealth().integrityPercent
            } catch (e: Exception) {
                _message.value = e.apiErrorMessage()
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun insertStore(store: Store) = runAndRefresh {
        api.createStore(StoreDto(name = store.name, location = store.location, licenseNumber = store.licenseNumber))
    }

    fun updateStore(store: Store) = runAndRefresh {
        api.updateStore(store.id, StoreDto(store.id, store.name, store.location, store.licenseNumber, store.status))
    }

    fun deleteStore(store: Store) = runAndRefresh { api.deleteStore(store.id) }

    private fun runAndRefresh(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
                refresh()
            } catch (e: Exception) {
                _message.value = e.apiErrorMessage()
            }
        }
    }

    private fun StoreDto.toStore() = Store(id = id, name = name, location = location, licenseNumber = licenseNumber, status = status)
}
