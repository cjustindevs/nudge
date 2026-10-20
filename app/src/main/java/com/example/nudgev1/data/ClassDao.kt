package com.example.nudgev1.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<ClassSchedule>>

    @Insert
    suspend fun insertClass(classSchedule: ClassSchedule)

    @Delete
    suspend fun deleteClass(classSchedule: ClassSchedule)
}
