package com.example.herbhopper_v1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY timestamp DESC")
    fun getOrdersByUser(userId: String): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): Order?

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    fun observeOrder(orderId: String): Flow<Order?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<Order>)

    @Query("DELETE FROM orders")
    suspend fun deleteAll()

    /** Reemplaza la caché local por lo que devuelve el servidor. */
    @Transaction
    suspend fun replaceAll(orders: List<Order>) {
        deleteAll()
        insertOrders(orders)
    }
}
