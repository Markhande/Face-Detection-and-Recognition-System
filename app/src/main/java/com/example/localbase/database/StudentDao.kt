package com.example.localbase.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface StudentDao {

    @Insert
    suspend fun insert(student: Student)

    @Query("SELECT * FROM student_table ORDER BY id ASC")
    fun getAllStudents(): LiveData<List<Student>>

    @Query("DELETE FROM student_table")
    suspend fun deleteAll()

    // Delete a student by ID
    @Query("DELETE FROM student_table WHERE id = :studentId")
    suspend fun deleteById(studentId: Int)

    @Update
    suspend fun update(student: Student)
}