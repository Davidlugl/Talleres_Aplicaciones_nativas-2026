package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.OrderDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val orderDao: OrderDao = AppDatabase.getDatabase(application).orderDao()
    private val firestore = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }

    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrdersByUser(userId: String): Flow<List<Order>> = orderDao.getOrdersByUser(userId)

    fun placeOrder(order: Order) {
        viewModelScope.launch {
            // Guardar local
            orderDao.insertOrder(order)
            
            // Guardar en Firestore
            try {
                firestore?.collection("orders")?.document(order.orderId)?.set(order)?.await()
            } catch (e: Exception) {
                // Error de red
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            // Actualizar local
            orderDao.updateOrderStatus(orderId, newStatus)
            
            // Actualizar Firestore
            try {
                firestore?.collection("orders")?.document(orderId)?.update("status", newStatus)?.await()
            } catch (e: Exception) {
                // Error de red
            }
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return orderDao.getOrderById(orderId)
    }

    // Sincronización básica para el vendedor
    fun syncOrdersFromCloud() {
        viewModelScope.launch {
            try {
                firestore?.collection("orders")?.get()?.await()?.let { snapshot ->
                    for (doc in snapshot.documents) {
                        val order = Order(
                            orderId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "Usuario",
                            itemsJson = doc.getString("itemsJson") ?: "[]",
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            status = doc.getString("status") ?: "PENDING",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            address = doc.getString("address"),
                            paymentMethod = doc.getString("paymentMethod")
                        )
                        orderDao.insertOrder(order)
                    }
                }
            } catch (e: Exception) {
                // Error de red
            }
        }
    }
}
