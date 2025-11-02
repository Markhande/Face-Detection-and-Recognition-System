package com.example.localbase.database

import androidx.lifecycle.LiveData

class StudentRepository(private val studentDao: StudentDao) {

    val allStudents: LiveData<List<Student>> = studentDao.getAllStudents()

    suspend fun insert(student: Student) {
        studentDao.insert(student)
    }

    suspend fun deleteAll() {
        studentDao.deleteAllAndResetId()
    }

    suspend fun deleteById(id:Int){
        studentDao.deleteById(id)
    }

    suspend fun update(student: Student){
        studentDao.update(student)
    }
}
