package com.example.calltracker.ui.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class LeadStatus(val status: String) {
    INTERESTED("Interested"),
    WRONG_NUMBER("Wrong Number"),
    FOLLOW_UP("Follow-up"),
    NONE("None")
}

data class CallLogItem(
    val name: String?,
    val number: String?,
    val type: String,
    val dateTime: Long,
    val duration: String,
    var leadStatus: LeadStatus = LeadStatus.NONE,
    val tags: MutableList<String> = mutableListOf()
)

data class CallStats(
    val missed: Int,
    val attended: Int,
    val avgPerDay: Float
)

data class AnalyticsData(
    val totalCalls: Int,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
    val rejectedCount: Int,
    val uniqueReach: Int,
    val talkTime: Long,
    val ringTime: Long,
    val connectivityRate: Float,
    val averageTalkTime: Float,
    val connectedCalls: Int,
    val unansweredCalls: Int
)

data class NeverAttendedCall(
    val number: String,
    val lastMissedCallTime: Long
)

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Analytics : BottomNavItem("analytics", "Analytics", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}