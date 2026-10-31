package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Producto del catálogo. Espejo local (Room) de la tabla products del backend.
 * Los campos opcionales son anulables porque Gson no aplica los valores por defecto de Kotlin.
 */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String?,
    val price: Double,
    val category: String = "General",
    val imageUrl: String? = null,
    /** Unidades disponibles en inventario. */
    val stock: Int = 0,
    /** Umbral mínimo de seguridad: por debajo o igual se marca como CRÍTICO. */
    val minStock: Int = 5,
    /** Concentración de compuestos activos, p. ej. "CBD 1000 mg / 30 ml (3,3 %)". */
    val cbdConcentration: String? = null,
    /** Porcentaje de THC verificado (Decreto 811/2021: ≥ 1 % es psicoactivo). */
    val thcPercent: Double = 0.0,
    val benefits: String? = null,
    val usageMode: String? = null,
    val certifications: String? = null,
    /** Si es true, el pedido exige una fórmula médica. */
    val requiresPrescription: Boolean = false
) {
    val isCriticalStock: Boolean get() = stock <= minStock

    fun getDrawableResId(context: android.content.Context): Int {
        val url = imageUrl ?: return 0
        if (url.startsWith("http")) return 0
        val nameWithoutExt = url.substringBefore(".")
        val finalResName = if (nameWithoutExt.all { it.isDigit() }) "prod_$nameWithoutExt" else nameWithoutExt
        return context.resources.getIdentifier(finalResName, "drawable", context.packageName)
    }
}
