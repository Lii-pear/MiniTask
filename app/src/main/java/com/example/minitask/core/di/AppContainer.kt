package com.example.minitask.core.di

import android.content.Context
import com.example.minitask.MiniTaskApp
import com.example.minitask.core.dispatchers.AppCoroutineDispatchers
import com.example.minitask.data.local.AppDatabase
import com.example.minitask.data.repository.OfflineTaskRepository
import com.example.minitask.data.repository.TaskRepository

interface AppContainer {
    val dispatchers: AppCoroutineDispatchers
    val appDatabase: AppDatabase
    val taskRepository: TaskRepository
}

class DefaultAppContainer(
    private val context: Context
) : AppContainer {
    override val dispatchers: AppCoroutineDispatchers by lazy { AppCoroutineDispatchers() }

    override val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(context)
    }

    override val taskRepository: TaskRepository by lazy {
        OfflineTaskRepository(
            taskDao = appDatabase.taskDao(),
            dispatchers = dispatchers
        )
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as MiniTaskApp).appContainer
