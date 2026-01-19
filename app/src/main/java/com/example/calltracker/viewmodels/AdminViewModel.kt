package com.example.calltracker.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.calltracker.data.CallData
import com.example.calltracker.ui.pages.DateFilterType
import java.util.*

// --- ViewModel ---
class AdminViewModel : ViewModel() {
    // This would be fetched from Firebase in a real app
    private val allCalls = listOf(
        CallData("1", System.currentTimeMillis() - 86400000 * 1, 120, true, 5, "New Lead"),
        CallData("2", System.currentTimeMillis() - 86400000 * 1, 0, false, 0, "New Lead"),
        CallData("3", System.currentTimeMillis() - 86400000 * 2, 300, true, 4, "Follow-up"),
        CallData("4", System.currentTimeMillis() - 86400000 * 3, 180, true, 5, "New Lead"),
        CallData("5", System.currentTimeMillis() - 86400000 * 8, 0, false, 0, "Never Attended"),
        CallData("6", System.currentTimeMillis() - 86400000 * 9, 240, true, 3, "Follow-up"),
        CallData("7", System.currentTimeMillis() - 3600000 * 2, 150, true, 4, "New Lead"), // 2 hours ago
        CallData("8", System.currentTimeMillis() - 3600000 * 3, 0, false, 0, "New Lead"), // 3 hours ago
        CallData("9", System.currentTimeMillis() - 3600000 * 4, 400, true, 5, "Follow-up"), // 4 hours ago
        CallData("10", System.currentTimeMillis() - 3600000 * 5, 200, true, 3, "New Lead"), // 5 hours ago
    )

    private val _filteredCalls = mutableStateOf(allCalls)
    val filteredCalls: State<List<CallData>> = _filteredCalls

    fun filterCalls(filterType: DateFilterType) {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        val startOfPeriod = when (filterType) {
            DateFilterType.TODAY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 0)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.timeInMillis
            }
            DateFilterType.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.timeInMillis
            }

            DateFilterType.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
        }
        _filteredCalls.value = allCalls.filter { it.timestamp >= startOfPeriod && it.timestamp <= now }
    }
}
