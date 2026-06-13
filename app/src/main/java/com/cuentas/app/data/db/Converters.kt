package com.cuentas.app.data.db

import androidx.room.TypeConverter
import com.cuentas.app.data.model.InputType

class Converters {
    @TypeConverter
    fun fromInputType(value: InputType): String = value.name

    @TypeConverter
    fun toInputType(value: String): InputType = try {
        InputType.valueOf(value)
    } catch (e: Exception) {
        InputType.TEXT
    }
}
