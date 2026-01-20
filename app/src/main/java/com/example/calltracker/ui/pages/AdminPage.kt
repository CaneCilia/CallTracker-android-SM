package com.example.calltracker.ui.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calltracker.data.CallData
import com.example.calltracker.viewmodels.AdminViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPage(viewModel: AdminViewModel = viewModel()) {
    val calls = viewModel.filteredCalls.value
    var showMenu by remember { mutableStateOf(false) }
    val selectedFilter = viewModel.selectedFilter.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics Dashboard") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DateFilterType.values().forEach { filterType ->
                                DropdownMenuItem(
                                    text = { Text(filterType.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        viewModel.filterCalls(filterType)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SummaryCard(calls = calls, filterType = selectedFilter) }

            item {
                ChartCard(
                    title = "Call Volume Trends",
                    inputLabel = "Input: Filtered call timestamps",
                    result = "Result: ${calls.size} calls recorded in this period"
                ) {
                    LineGraph(calls)
                }
            }

            item {
                ChartCard(
                    title = "Peak Hour Analysis (Heatmap)",
                    inputLabel = "Input: Call frequency per hour",
                    result = "Result: Peak activity identified during mid-day"
                ) {
                    Heatmap(calls)
                }
            }

            item {
                ChartCard(
                    title = "Cumulative Talk Time (Area)",
                    inputLabel = "Input: Individual call durations",
                    result = "Result: Total of ${calls.sumOf { it.duration } / 60} minutes of talk time"
                ) {
                    AreaChart(calls)
                }
            }

            item {
                ChartCard(
                    title = "Sales Conversion Funnel",
                    inputLabel = "Input: Customer journey stages",
                    result = "Result: ${if (calls.isNotEmpty()) (calls.count { it.answered } * 40 / calls.size) else 0}% estimated conversion efficiency"
                ) {
                    FunnelChart(calls)
                }
            }
        }
    }
}

@Composable
fun ChartCard(title: String, inputLabel: String, result: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(inputLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            content()
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- CHART COMPONENTS ---

@Composable
fun LineGraph(calls: List<CallData>) {
    val color = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    if (calls.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data available", color = textColor)
        }
        return
    }

    val sorted = calls.sortedBy { it.timestamp }
    val minTime = sorted.first().timestamp
    val maxTime = sorted.last().timestamp
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
    
    val buckets = 10
    val counts = IntArray(buckets) { 0 }
    sorted.forEach {
        val bucket = (((it.timestamp - minTime).toFloat() / timeRange) * (buckets - 1)).toInt()
        counts[bucket.coerceIn(0, buckets - 1)]++
    }
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            // Y-Axis labels (Metrics)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$maxCount", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("${maxCount / 2}", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("0", style = MaterialTheme.typography.labelSmall, color = textColor)
            }

            Canvas(modifier = Modifier.fillMaxSize().padding(start = 25.dp, top = 8.dp, bottom = 8.dp, end = 8.dp)) {
                val width = size.width
                val height = size.height
                val stepX = width / (buckets - 1)
                
                val path = Path().apply {
                    counts.forEachIndexed { index, count ->
                        val x = index * stepX
                        val y = height - (count.toFloat() / maxCount) * height
                        if (index == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
                
                counts.forEachIndexed { index, count ->
                    val x = index * stepX
                    val y = height - (count.toFloat() / maxCount) * height
                    drawCircle(color, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 25.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Earlier", style = MaterialTheme.typography.labelSmall, color = textColor)
            Text("Later", style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

@Composable
fun Heatmap(calls: List<CallData>) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    
    val hourCounts = IntArray(24) { 0 }
    val calendar = Calendar.getInstance()
    calls.forEach {
        calendar.timeInMillis = it.timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        hourCounts[hour]++
    }
    val maxCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            hourCounts.forEach { count ->
                val intensity = count.toFloat() / maxCount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(primaryColor.copy(alpha = intensity.coerceAtLeast(0.05f)))
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12 AM", style = MaterialTheme.typography.labelSmall, color = textColor)
            Text("12 PM", style = MaterialTheme.typography.labelSmall, color = textColor)
            Text("11 PM", style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

@Composable
fun AreaChart(calls: List<CallData>) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sortedCalls = calls.sortedBy { it.timestamp }
    
    if (sortedCalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No talk time recorded", color = textColor)
        }
        return
    }

    var cumulative = 0
    val points = sortedCalls.map { 
        cumulative += it.duration
        cumulative.toFloat()
    }
    val maxVal = points.last().coerceAtLeast(1f)
    val totalMinutes = (maxVal / 60).toInt()

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            // Y-Axis labels (Metrics)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${totalMinutes}m", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("${totalMinutes / 2}m", style = MaterialTheme.typography.labelSmall, color = textColor)
                Text("0m", style = MaterialTheme.typography.labelSmall, color = textColor)
            }

            Canvas(modifier = Modifier.fillMaxSize().padding(start = 35.dp, bottom = 4.dp)) {
                val width = size.width
                val height = size.height
                val stepX = width / (points.size.coerceAtLeast(2) - 1).toFloat()

                val fillPath = Path().apply {
                    moveTo(0f, height)
                    points.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = height - (value / maxVal) * height
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    close()
                }
                
                drawPath(fillPath, secondaryColor.copy(alpha = 0.2f))
                drawPath(fillPath, secondaryColor, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(start = 35.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Start", style = MaterialTheme.typography.labelSmall, color = textColor)
            Text("End", style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

@Composable
fun FunnelChart(calls: List<CallData>) {
    val total = calls.size
    val answered = calls.count { it.answered }
    val leads = (answered * 0.4).toInt() // Example conversion logic

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FunnelStep("Total Calls", total, 1f, MaterialTheme.colorScheme.primaryContainer)
        FunnelStep("Answered", answered, 0.8f, MaterialTheme.colorScheme.secondaryContainer)
        FunnelStep("Leads/Sales", leads, 0.6f, MaterialTheme.colorScheme.tertiaryContainer)
    }
}

@Composable
fun FunnelStep(label: String, value: Int, widthPercent: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(widthPercent).height(40.dp).background(color, shape = MaterialTheme.shapes.small).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value.toString(), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryCard(calls: List<CallData>, filterType: DateFilterType) {
    val missedCalls = calls.count { !it.answered }
    val attendedCalls = calls.count { it.answered }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Overview", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryItem(Icons.Default.CallMissed, missedCalls.toString(), "Missed")
                SummaryItem(Icons.Default.Call, attendedCalls.toString(), "Attended")
                SummaryItem(Icons.Default.TrendingUp, "85%", "Success")
            }
        }
    }
}

@Composable
fun SummaryItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
