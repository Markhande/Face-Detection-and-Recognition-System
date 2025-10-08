package com.example.localbase.converters

import androidx.room.TypeConverter

class Converters {

    // Convert FloatArray to a comma-separated string
    @TypeConverter
    fun fromFloatArray(floatArray: FloatArray): String {
        return floatArray.joinToString(",")  // Join array elements with commas
    }

    // Convert a comma-separated string back to a FloatArray
    @TypeConverter
    fun toFloatArray(data: String): FloatArray {
        return data.split(",").map { it.toFloat() }.toFloatArray()
    }
}