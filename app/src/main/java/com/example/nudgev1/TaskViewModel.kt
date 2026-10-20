package com.example.nudgev1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nudgev1.data.AppDatabase
import com.example.nudgev1.data.Task
import com.example.nudgev1.data.UserStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.taskDao()

    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userStats: StateFlow<UserStats?> = dao.getUserStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStats(id = 1, currentStreak = 1, longestStreak = 1, totalTasksCompleted = 0)
        )

    fun addTask(task: Task) {
        viewModelScope.launch {
            dao.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newlyCompleted = !task.isCompleted
            dao.updateTask(task.copy(isCompleted = newlyCompleted))

            val currentStats = userStats.value ?: UserStats(id = 1, currentStreak = 1, longestStreak = 1, totalTasksCompleted = 0)
            if (newlyCompleted) {
                val newTotal = currentStats.totalTasksCompleted + 1
                val newStreak = currentStats.currentStreak + 1
                val newLongest = maxOf(newStreak, currentStats.longestStreak)
                dao.insertOrUpdateStats(currentStats.copy(currentStreak = newStreak, longestStreak = newLongest, totalTasksCompleted = newTotal))
            }
        }
    }

    fun addStudyTime(task: Task, minutes: Long) {
        viewModelScope.launch {
            val updatedDuration = task.studyDuration + minutes
            dao.updateTask(task.copy(studyDuration = updatedDuration))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }
}
