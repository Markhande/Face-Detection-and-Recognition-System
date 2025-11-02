package com.example.localbase.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_table")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val date: String?,
    val time: String?,
    val grades: FloatArray,
    val imagePath: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Student

        if (id != other.id) return false
        if (name != other.name) return false
        if (date != other.date) return false
        if (time != other.time) return false
        if (!grades.contentEquals(other.grades)) return false
        if (imagePath != other.imagePath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (time?.hashCode() ?: 0)
        result = 31 * result + grades.contentHashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        return result
    }
}

