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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                title = { Text("Business Analytics") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DateFilterType.values().forEach { filterType ->
                                DropdownMenuItem(
                                    text = { Text(filterType.name.lowercase().capitalize()) },
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
                ChartCard("Call Volume Trends") {
                    LineGraph(calls)
                }
            }

            item {
                ChartCard("Peak Hour Analysis (Heatmap)") {
                    Heatmap(calls)
                }
            }

            item {
                ChartCard("Cumulative Talk Time (Area)") {
                    AreaChart(calls)
                }
            }

            item {
                ChartCard("Sales Conversion Funnel") {
                    FunnelChart(calls)
                }
            }
        }
    }
}

@Composable
fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

// --- CHART COMPONENTS ---

@Composable
fun LineGraph(calls: List<CallData>) {
    // Dummy trend data based on call timestamps
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width * 0.2f, size.height * 0.5f)
            lineTo(size.width * 0.5f, size.height * 0.8f)
            lineTo(size.width * 0.8f, size.height * 0.2f)
            lineTo(size.width, size.height * 0.4f)
        }
        drawPath(path, color, style = Stroke(width = 4.dp.toPx()))
    }
}

@Composable
fun Heatmap(calls: List<CallData>) {
    // Represents 24 hours of the day
    Row(modifier = Modifier.fillMaxWidth().height(60.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(24) { hour ->
            // In a real app, calculate density: calls.count { it.hour == hour }
            val intensity = (0..10).random() / 10f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = intensity.coerceAtLeast(0.1f)))
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("12 AM", style = MaterialTheme.typography.bodySmall)
        Text("12 PM", style = MaterialTheme.typography.bodySmall)
        Text("11 PM", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AreaChart(calls: List<CallData>) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val fillPath = Path().apply {
            moveTo(0f, size.height)
            lineTo(size.width * 0.3f, size.height * 0.4f)
            lineTo(size.width * 0.6f, size.height * 0.3f)
            lineTo(size.width, size.height * 0.1f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(fillPath, secondaryColor.copy(alpha = 0.3f))
        drawPath(fillPath, secondaryColor, style = Stroke(width = 2.dp.toPx()))
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