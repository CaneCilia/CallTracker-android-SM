package com.example.calltracker.ui.pages

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(callLogs: List<CallLogItem>) {
    var selectedFilter by remember { mutableStateOf("This Week") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customDateRange by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var showFilter by remember { mutableStateOf(false) }

    val filteredLogs = remember(selectedFilter, customDateRange, callLogs) {
        filterCallLogs(callLogs, selectedFilter, customDateRange)
    }

    val analyticsData = calculateAnalytics(filteredLogs)
    val neverAttendedCalls = calculateNeverAttended(filteredLogs)

    if (showDatePicker) {
        CustomDateRangePicker(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                customDateRange = Pair(start, end)
                selectedFilter = "Custom"
                showDatePicker = false
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Analytics", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = { showFilter = !showFilter }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }

        if (showFilter) {
            DateFilter(
                selectedFilter = selectedFilter,
                onFilterSelected = { filter ->
                    if (filter == "Custom") {
                        showDatePicker = true
                    } else {
                        selectedFilter = filter
                        customDateRange = null
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CoreActivityMetrics(analytics = analyticsData, neverAttendedCallsCount = neverAttendedCalls.size)
            }
            item {
                CallTypeBreakdown(analytics = analyticsData)
            }
            item {
                HourlyActivityChart(callLogs = filteredLogs)
            }
            item {
                WeeklyActivityChart(callLogs = filteredLogs)
            }
            item {
                LeadStatusDistributionChart(callLogs = filteredLogs)
            }
            item {
                DurationDistributionChart(callLogs = filteredLogs)
            }
            item {
                NeverAttendedList(neverAttendedCalls = neverAttendedCalls)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateFilter(selectedFilter: String, onFilterSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    val filters = listOf("This Week", "This Month", "All Time", "Custom")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(Icons.Default.DateRange, contentDescription = "Date Range")
        Spacer(modifier = Modifier.width(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePicker(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            DateRangePicker(state = dateRangePickerState, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(
                onClick = {
                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                        dateRangePickerState.selectedEndDateMillis?.let { end ->
                            onDateRangeSelected(start, end)
                        }
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
fun CoreActivityMetrics(analytics: AnalyticsData, neverAttendedCallsCount: Int) {
    val talkTimeFormatted = formatDuration(analytics.talkTime)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                title = "Total Calls",
                value = analytics.totalCalls.toString(),
                icon = Icons.Default.Call,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Total Duration",
                value = talkTimeFormatted,
                icon = Icons.Default.AccessTime,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                title = "Connected Calls",
                value = analytics.connectedCalls.toString(),
                icon = Icons.Default.PhoneInTalk,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Unique Clients",
                value = analytics.uniqueReach.toString(),
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            KpiCard(
                title = "Unanswered Calls",
                value = analytics.unansweredCalls.toString(),
                icon = Icons.Default.PhoneMissed,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Never Attended",
                value = neverAttendedCallsCount.toString(),
                icon = Icons.Default.PhoneMissed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KpiCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (title == "Total Duration" || title == "Unanswered Calls" || title == "Never Attended") Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
fun CallTypeBreakdown(analytics: AnalyticsData) {
    val pieChartData = listOf(
        PieEntry(analytics.incomingCount.toFloat(), "Incoming"),
        PieEntry(analytics.outgoingCount.toFloat(), "Outgoing"),
        PieEntry(analytics.missedCount.toFloat(), "Missed"),
        PieEntry(analytics.rejectedCount.toFloat(), "Rejected")
    )

    val pieDataSet = PieDataSet(pieChartData, "").apply {
        colors = listOf(
            Color(0xFF66BB6A).toArgb(), // Green
            Color(0xFF42A5F5).toArgb(), // Blue
            Color(0xFFEF5350).toArgb(),  // Red
            Color(0xFF78909C).toArgb() // Grey
        )
        valueTextColor = Color.Black.toArgb()
        valueTextSize = 12f
    }

    val pieData = PieData(pieDataSet)

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Call Type Breakdown", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(factory = { context ->
                PieChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )
                    data = pieData
                    description.isEnabled = false
                    setUsePercentValues(true)
                    isDrawHoleEnabled = true
                    holeRadius = 58f
                    transparentCircleRadius = 61f
                    setDrawCenterText(true)
                    centerText = "Call Types"
                    setCenterTextSize(16f)
                    legend.isEnabled = false
                    animateY(1400)
                }
            }, modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun HourlyActivityChart(callLogs: List<CallLogItem>) {
    val hourlyData = callLogs.groupBy {
        SimpleDateFormat("HH", Locale.getDefault()).format(Date(it.dateTime)).toInt()
    }.mapValues { it.value.size }

    val barEntries = (0..23).map {
        BarEntry(it.toFloat(), hourlyData[it]?.toFloat() ?: 0f)
    }

    val barDataSet = BarDataSet(barEntries, "Calls").apply {
        color = MaterialTheme.colorScheme.primary.toArgb()
        valueTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
        valueTextSize = 10f
    }

    val barData = BarData(barDataSet)

    val labels = (0..23).map { String.format("%02d", it) }

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Hourly Activity", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(factory = { context ->
                BarChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )
                    data = barData
                    description.isEnabled = false
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    xAxis.labelCount = 12
                    axisRight.isEnabled = false
                    animateY(1400)
                }
            }, modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun WeeklyActivityChart(callLogs: List<CallLogItem>) {
    val weeklyData = callLogs.groupBy {
        val calendar = Calendar.getInstance()
        calendar.time = Date(it.dateTime)
        calendar.get(Calendar.DAY_OF_WEEK)
    }.mapValues { it.value.size }

    val barEntries = (1..7).map {
        BarEntry(it.toFloat() -1, weeklyData[it]?.toFloat() ?: 0f)
    }

    val barDataSet = BarDataSet(barEntries, "Calls").apply {
        color = MaterialTheme.colorScheme.primary.toArgb()
        valueTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
        valueTextSize = 10f
    }

    val barData = BarData(barDataSet)

    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Weekly Activity", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(factory = { context ->
                BarChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )
                    data = barData
                    description.isEnabled = false
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    xAxis.labelCount = 7
                    axisRight.isEnabled = false
                    animateY(1400)
                }
            }, modifier = Modifier.height(200.dp))
        }
    }
}

@Composable
fun LeadStatusDistributionChart(callLogs: List<CallLogItem>) {
    val leadStatusData = callLogs.groupBy { it.leadStatus }.mapValues { it.value.size }

    val pieEntries = LeadStatus.values().map {
        PieEntry(leadStatusData[it]?.toFloat() ?: 0f, it.status)
    }

    val pieDataSet = PieDataSet(pieEntries, "").apply {
        colors = listOf(
            Color(0xFF66BB6A).toArgb(),
            Color(0xFFEF5350).toArgb(),
            Color(0xFFFFA726).toArgb(),
            Color(0xFF78909C).toArgb()
        )
        valueTextColor = Color.Black.toArgb()
        valueTextSize = 12f
    }

    val pieData = PieData(pieDataSet)

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Lead Status Distribution", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(factory = { context ->
                PieChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )
                    data = pieData
                    description.isEnabled = false
                    setUsePercentValues(true)
                    isDrawHoleEnabled = true
                    holeRadius = 58f
                    transparentCircleRadius = 61f
                    setDrawCenterText(true)
                    centerText = "Lead Status"
                    setCenterTextSize(16f)
                    legend.isEnabled = false
                    animateY(1400)
                }
            }, modifier = Modifier.height(200.dp))
        }
    }
}


@Composable
fun DurationDistributionChart(callLogs: List<CallLogItem>) {
    val durationCategories = callLogs.map { it.duration.toLong() }.groupBy {
        when {
            it < 30 -> "<30s"
            it in 30..180 -> "1-3m"
            else -> ">3m"
        }
    }.mapValues { it.value.size }

    val barEntries = listOf(
        BarEntry(0f, durationCategories["<30s"]?.toFloat() ?: 0f),
        BarEntry(1f, durationCategories["1-3m"]?.toFloat() ?: 0f),
        BarEntry(2f, durationCategories[">3m"]?.toFloat() ?: 0f)
    )

    val barDataSet = BarDataSet(barEntries, "Call Durations").apply {
        colors = listOf(
            Color(0xFF42A5F5).toArgb(),
            Color(0xFF66BB6A).toArgb(),
            Color(0xFFFFA726).toArgb()
        )
        valueTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
        valueTextSize = 10f
    }

    val barData = BarData(barDataSet)

    val labels = listOf("<30s", "1-3m", ">3m")

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Duration Distribution", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(factory = { context ->
                BarChart(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        500
                    )
                    data = barData
                    description.isEnabled = false
                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(false)
                    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                    axisRight.isEnabled = false
                    animateY(1400)
                }
            }, modifier = Modifier.height(200.dp))
        }
    }
}


@Composable
fun NeverAttendedList(neverAttendedCalls: List<NeverAttendedCall>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Never Attended", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            if (neverAttendedCalls.isEmpty()) {
                Text("No one is waiting for your call back.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column {
                    neverAttendedCalls.forEach { call ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhoneMissed, contentDescription = "Missed call")
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(call.number, fontWeight = FontWeight.Bold)
                                Text(formatTimestamp(call.lastMissedCallTime), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds < 0) return "0s"
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val remainingSeconds = seconds % 60

    val parts = mutableListOf<String>()
    if (hours > 0) {
        parts.add("${hours}h")
    }
    if (minutes > 0) {
        parts.add("${minutes}m")
    }
    if (remainingSeconds > 0 || parts.isEmpty()) {
        parts.add("${remainingSeconds}s")
    }
    return parts.joinToString(" ")
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
