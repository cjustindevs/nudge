package com.example.nudgev1.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // --- TASKS ---
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // --- USER STATS ---
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStats?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: UserStats)

    // --- CLASSES ---
    @Query("SELECT * FROM classes ORDER BY id ASC")
    fun getAllClasses(): Flow<List<ClassSchedule>>

    @Insert
    suspend fun insertClass(classSchedule: ClassSchedule)

    @Delete
    suspend fun deleteClass(classSchedule: ClassSchedule)
}
