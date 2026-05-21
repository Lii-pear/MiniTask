package com.example.minitask.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.minitask.core.dispatchers.AppCoroutineDispatchers
import com.example.minitask.data.repository.TaskRepository

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val dispatchers: AppCoroutineDispatchers
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return TaskViewModel(
            repository = repository,
            dispatchers = dispatchers
        ) as T
    }
}
