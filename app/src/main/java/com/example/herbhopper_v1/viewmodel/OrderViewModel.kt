package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.OrderDao
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.CreateOrderRequest
import com.example.herbhopper_v1.data.network.OrderStatusRequest
import com.example.herbhopper_v1.data.network.SellerStatsResponse
import com.example.herbhopper_v1.data.network.apiErrorMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Pedidos. El backend decide qué pedidos ve cada rol (el paciente solo los suyos,
 * vendedor y administrador todos) y Room guarda la última copia para uso sin conexión.
 */
class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val orderDao: OrderDao = AppDatabase.getDatabase(application).orderDao()
    private val api = ApiService.instance

    /** Pedidos visibles para el usuario actual (ya filtrados por el servidor). */
    val orders: Flow<List<Order>> = orderDao.getAllOrders()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _sellerStats = MutableStateFlow<SellerStatsResponse?>(null)
    val sellerStats: StateFlow<SellerStatsResponse?> = _sellerStats

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                orderDao.replaceAll(api.getOrders())
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.apiErrorMessage()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Borra la caché local (al cerrar sesión, para que otro usuario no vea estos pedidos). */
    fun clearLocal() {
        viewModelScope.launch { orderDao.deleteAll() }
    }

    fun observeOrder(orderId: String): Flow<Order?> = orderDao.observeOrder(orderId)

    fun refreshOrder(orderId: String) {
        viewModelScope.launch {
            try {
                orderDao.insertOrder(api.getOrder(orderId))
            } catch (e: Exception) {
                _error.value = e.apiErrorMessage()
            }
        }
    }

    /** Registra el pedido en el servidor (valida precios, stock y fórmula) antes de pagar. */
    suspend fun createOrder(request: CreateOrderRequest): Result<Order> = try {
        val order = api.createOrder(request)
        orderDao.insertOrder(order)
        Result.success(order)
    } catch (e: Exception) {
        Result.failure(Exception(e.apiErrorMessage()))
    }

    /** Anula un pedido propio no pagado (p. ej. si el usuario abandona el pago). */
    fun cancelMyOrder(orderId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.cancelMyOrder(orderId)
                refreshOrder(orderId)
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    /** Vendedor: ACEPTAR, CANCELAR o MARCAR COMO ENTREGADO. [onResult] recibe null si salió bien. */
    fun updateOrderStatus(orderId: String, newStatus: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                api.updateOrderStatus(orderId, OrderStatusRequest(newStatus))
                orderDao.insertOrder(api.getOrder(orderId))
                onResult(null)
            } catch (e: Exception) {
                onResult(e.apiErrorMessage())
            }
        }
    }

    /** Ventas del día (solo pagos aprobados) y pedidos nuevos, para el dashboard del vendedor. */
    fun loadSellerStats() {
        viewModelScope.launch {
            try {
                _sellerStats.value = api.getSellerStats()
            } catch (e: Exception) {
                _error.value = e.apiErrorMessage()
            }
        }
    }

    suspend fun getPaymentStatus(orderId: String): String? = try {
        api.getPaymentStatus(orderId).paymentStatus
    } catch (e: Exception) {
        null
    }
}
