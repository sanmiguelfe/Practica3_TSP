package com.mexiti.costogasolina.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gasoline_transactions")
data class GasolineTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val pricePerLiter: Double, // Precio por litro de gasolina
    val liters: Double,       // Litros comprados
    val tip: Double,          // Importe de la propina
    val date: String,         // Fecha de la transacción
    val kilometers: Double,   // Kilometraje del automóvil

    @ColumnInfo(defaultValue = "0")
    val includesTip: Boolean = false,

    // Imagen del comprobante almacenada como datos binarios.
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val receiptImage: ByteArray? = null
)