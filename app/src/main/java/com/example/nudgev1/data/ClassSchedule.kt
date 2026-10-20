package com.example.nudgev1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectName: String,
    val room: String,
    val startTime: String,
    val endTime: String
)
