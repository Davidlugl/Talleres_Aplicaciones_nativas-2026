package com.example.herbhopper_v1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val category: String = "General",
    val imageUrl: String? = null
) {
    fun getDrawableResId(context: android.content.Context): Int {
        val url = imageUrl ?: return 0
        if (url.startsWith("http")) return 0
        val nameWithoutExt = url.substringBefore(".")
        val finalResName = if (nameWithoutExt.all { it.isDigit() }) "prod_$nameWithoutExt" else nameWithoutExt
        return context.resources.getIdentifier(finalResName, "drawable", context.packageName)
    }
}
