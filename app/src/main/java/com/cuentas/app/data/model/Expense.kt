package com.cuentas.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InputType { VOICE, CAMERA, TEXT }

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val storeName: String = "",
    val category: String = "Otro",
    val imageUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val rawInput: String = "",
    val inputType: InputType = InputType.TEXT
)
