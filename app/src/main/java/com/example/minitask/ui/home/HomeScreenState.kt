package com.example.minitask.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Stable
class HomeScreenState internal constructor(
    showMemoDialogState: MutableState<Boolean>,
    showAddTaskSheetState: MutableState<Boolean>,
    showStatsDialogState: MutableState<Boolean>
) {
    var showMemoDialog by showMemoDialogState
        private set

    var showAddTaskSheet by showAddTaskSheetState
        private set

    var showStatsDialog by showStatsDialogState
        private set

    fun openMemoDialog() {
        showMemoDialog = true
    }

    fun dismissMemoDialog() {
        showMemoDialog = false
    }

    fun openAddTaskSheet() {
        showAddTaskSheet = true
    }

    fun dismissAddTaskSheet() {
        showAddTaskSheet = false
    }

    fun openStatsDialog() {
        showStatsDialog = true
    }

    fun dismissStatsDialog() {
        showStatsDialog = false
    }
}

@Composable
fun rememberHomeScreenState(): HomeScreenState {
    val showMemoDialog = rememberSaveable { mutableStateOf(false) }
    val showAddTaskSheet = rememberSaveable { mutableStateOf(false) }
    val showStatsDialog = rememberSaveable { mutableStateOf(false) }

    return remember(
        showMemoDialog,
        showAddTaskSheet,
        showStatsDialog
    ) {
        HomeScreenState(
            showMemoDialogState = showMemoDialog,
            showAddTaskSheetState = showAddTaskSheet,
            showStatsDialogState = showStatsDialog
        )
    }
}
