package com.example.minitask

import android.app.Application
import com.example.minitask.core.calendar.LunarDateCache
import com.example.minitask.core.di.AppContainer
import com.example.minitask.core.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class MiniTaskApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val appContainer: AppContainer by lazy { DefaultAppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            coroutineScope {
                val currentMonth = YearMonth.from(LocalDate.now())
                launch { LunarDateCache.prewarmMonth(currentMonth.minusMonths(1)) }
                launch { LunarDateCache.prewarmMonth(currentMonth) }
                launch { LunarDateCache.prewarmMonth(currentMonth.plusMonths(1)) }
                launch { prewarmDatabase() }
            }
        }
    }

    private suspend fun prewarmDatabase() = withContext(Dispatchers.IO) {
        val db = appContainer.appDatabase
        db.openHelper.writableDatabase.query("SELECT 1").close()
    }
}
