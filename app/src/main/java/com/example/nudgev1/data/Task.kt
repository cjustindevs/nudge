package com.example.nudgev1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val subject: String,
    val category: String, // "Assignment", "Exam", "Project", "Personal"
    val deadline: String,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val studyDuration: Long = 0L
)
