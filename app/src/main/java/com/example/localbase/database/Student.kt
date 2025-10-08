package com.example.localbase.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_table")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val date: String? = null,
    val time: String? = null,
    val grades: FloatArray,
    val imagePath: String? = null
)

