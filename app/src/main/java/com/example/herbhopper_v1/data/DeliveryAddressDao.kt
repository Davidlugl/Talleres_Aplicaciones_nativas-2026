package com.example.herbhopper_v1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre [DeliveryAddress].
 * Proporciona consultas reactivas (Flow) y suspendidas para gestionar
 * las direcciones de entrega asociadas a un usuario.
 */
@Dao
interface DeliveryAddressDao {

    /** Observa en tiempo real todas las direcciones del usuario, ordenadas: por defecto primero. */
    @Query("SELECT * FROM delivery_addresses WHERE uid = :uid ORDER BY isDefault DESC, createdAt DESC")
    fun observeAddresses(uid: String): Flow<List<DeliveryAddress>>

    /** Inserta una nueva dirección. Si ya existe el mismo id, la reemplaza. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: DeliveryAddress)

    /** Actualiza los campos de una dirección existente. */
    @Update
    suspend fun updateAddress(address: DeliveryAddress)

    /** Elimina una dirección por su id. */
    @Query("DELETE FROM delivery_addresses WHERE id = :id")
    suspend fun deleteAddress(id: Int)

    /** Desmarca todas las direcciones del usuario como predeterminadas. */
    @Query("UPDATE delivery_addresses SET isDefault = 0 WHERE uid = :uid")
    suspend fun clearDefault(uid: String)

    /** Marca una dirección específica como predeterminada. */
    @Query("UPDATE delivery_addresses SET isDefault = 1 WHERE id = :id")
    suspend fun setDefault(id: Int)
}
