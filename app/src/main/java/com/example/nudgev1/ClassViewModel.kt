package com.example.nudgev1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nudgev1.data.AppDatabase
import com.example.nudgev1.data.ClassSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClassViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.classDao()

    val classes: StateFlow<List<ClassSchedule>> = dao.getAllClasses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addClass(classSchedule: ClassSchedule) {
        viewModelScope.launch {
            dao.insertClass(classSchedule)
        }
    }

    fun deleteClass(classSchedule: ClassSchedule) {
        viewModelScope.launch {
            dao.deleteClass(classSchedule)
        }
    }
}
