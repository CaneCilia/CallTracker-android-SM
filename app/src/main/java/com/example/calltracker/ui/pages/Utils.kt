package com.example.calltracker.ui.pages

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun filterCallLogs(logs: List<CallLogItem>, filter: String, customRange: Pair<Long, Long>?): List<CallLogItem> {
    val calendar = Calendar.getInstance()
    return when (filter) {
        "This Week" -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val startTime = calendar.timeInMillis
            logs.filter { it.dateTime >= startTime }
        }
        "This Month" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startTime = calendar.timeInMillis
            logs.filter { it.dateTime >= startTime }
        }
        "All Time" -> logs
        "Custom" -> {
            customRange?.let { (start, end) ->
                logs.filter { it.dateTime in start..end }
            } ?: logs
        }
        else -> logs
    }
}

fun loadCallLogs(context: Context): List<CallLogItem> {
    val list = mutableListOf<CallLogItem>()

    val cursor: Cursor? = context.contentResolver.query(
        CallLog.Calls.CONTENT_URI,
        null,
        null,
        null,
        "${CallLog.Calls.DATE} DESC"
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
        val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
        val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

        while (it.moveToNext()) {
            val type = when (it.getInt(typeIndex)) {
                CallLog.Calls.MISSED_TYPE -> "Missed"
                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                CallLog.Calls.REJECTED_TYPE -> "Rejected"
                else -> "Other"
            }

            list.add(
                CallLogItem(
                    it.getString(nameIndex),
                    it.getString(numberIndex),
                    type,
                    it.getLong(dateIndex),
                    it.getString(durationIndex)
                )
            )
        }
    }
    return list
}

fun calculateAnalytics(logs: List<CallLogItem>): AnalyticsData {
    val outgoingCalls = logs.filter { it.type == "Outgoing" }
    val answeredOutgoingCalls = outgoingCalls.filter { it.duration.toLong() > 0 }
    val connectivityRate = if (outgoingCalls.isNotEmpty()) {
        answeredOutgoingCalls.size.toFloat() / outgoingCalls.size
    } else {
        0f
    }

    val attendedCalls = logs.filter { it.type != "Missed" && it.type != "Rejected" }
    val totalTalkTime = attendedCalls.sumOf { it.duration.toLong() }
    val averageTalkTime = if (attendedCalls.isNotEmpty()) {
        totalTalkTime.toFloat() / attendedCalls.size
    } else {
        0f
    }

    val totalRingTime = logs.sumOf { it.dateTime - (it.dateTime - it.duration.toLong() * 1000) } / 1000
    val missedCount = logs.count { it.type == "Missed" }
    val rejectedCount = logs.count { it.type == "Rejected" }

    return AnalyticsData(
        totalCalls = logs.size,
        incomingCount = logs.count { it.type == "Incoming" },
        outgoingCount = outgoingCalls.size,
        missedCount = missedCount,
        rejectedCount = rejectedCount,
        uniqueReach = logs.distinctBy { it.number }.size,
        talkTime = totalTalkTime,
        ringTime = totalRingTime,
        connectivityRate = connectivityRate,
        averageTalkTime = averageTalkTime,
        connectedCalls = attendedCalls.size,
        unansweredCalls = logs.size - attendedCalls.size
    )
}

fun calculateNeverAttended(logs: List<CallLogItem>): List<NeverAttendedCall> {
    val missedCalls = logs.filter { it.type == "Missed" }
    val outgoingCalls = logs.filter { it.type == "Outgoing" }

    val neverAttended = mutableListOf<NeverAttendedCall>()

    for (missed in missedCalls) {
        val hasCalledBack = outgoingCalls.any { outgoing ->
            outgoing.number == missed.number && outgoing.dateTime > missed.dateTime
        }
        if (!hasCalledBack) {
            if (neverAttended.none { it.number == missed.number }) {
                missed.number?.let {
                    neverAttended.add(NeverAttendedCall(it, missed.dateTime))
                }
            }
        }
    }
    return neverAttended.sortedByDescending { it.lastMissedCallTime }
}


fun calculateStats(logs: List<CallLogItem>): CallStats {
    val missed = logs.count { it.type == "Missed" }
    val attended = logs.count { it.type != "Missed" }
    val days = logs.map { getDay(it.dateTime) }.distinct().size.coerceAtLeast(1)
    return CallStats(missed, attended, logs.size.toFloat() / days)
}

fun getDay(time: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))

fun formatDate(time: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))

fun uploadCallLogsToFirestore(
    logs: List<CallLogItem>,
    onSuccess: (Int) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val user = Firebase.auth.currentUser

    if (user == null) {
        onFailure(Exception("User not logged in"))
        return
    }

    if (logs.isEmpty()) {
        onFailure(Exception("No call logs to upload"))
        return
    }

    val db = Firebase.firestore
    val userDoc = db.collection("users").document(user.uid)

    val batch = db.batch()

    val logsToUpload = logs.take(500)
    logsToUpload.forEach { log ->
        val docRef = userDoc.collection("calldata").document()

        val data = hashMapOf(
            "name" to log.name,
            "number" to log.number,
            "type" to log.type,
            "dateTime" to log.dateTime,
            "duration" to log.duration,
            "leadStatus" to log.leadStatus.name,
            "tags" to log.tags,
            "uploadedAt" to FieldValue.serverTimestamp()
        )

        batch.set(docRef, data)
    }

    batch.commit()
        .addOnSuccessListener {
            Log.d("FIRESTORE", "Upload success")
            onSuccess(logsToUpload.size)
        }
        .addOnFailureListener {
            Log.e("FIRESTORE", "Upload failed", it)
            onFailure(it)
        }
}
