package com.example.minitask.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class AppCoroutineDispatchers(
    val io: CoroutineDispatcher = Dispatchers.IO,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val main: CoroutineDispatcher = Dispatchers.Main.immediate
)
