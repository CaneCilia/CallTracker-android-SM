package com.example.calltracker.data

// --- Data Models ---
data class CallData(
    val id: String = "",
    val timestamp: Long = 0L,
    val duration: Int = 0, // in seconds
    val answered: Boolean = false,
    val satisfactionScore: Int = 0, // e.g., 1-5
    val leadType: String = ""
)
