package com.example.calltracker.ui.pages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.calltracker.data.CallData
import com.example.calltracker.viewmodels.AdminViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilterType {
    TODAY, THIS_WEEK, THIS_MONTH
}

@Composable
fun AdminPage(viewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val calls = viewModel.filteredCalls.value
    var selectedFilter by remember { mutableStateOf(DateFilterType.TODAY) }

    LaunchedEffect(selectedFilter) {
        viewModel.filterCalls(selectedFilter)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(selectedFilter) { newFilter ->
                selectedFilter = newFilter
            }
        }

        item {
            KpiSection(calls)
        }

        item {
            AnalyticsPager(calls)
        }

        item {
            CallDataTable(calls)
        }
    }
}

@Composable
fun Header(selectedFilter: DateFilterType, onFilterSelected: (DateFilterType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Analysis Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = selectedFilter.name.replace('_', ' '))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DateFilterType.values().forEach { filterType ->
                    DropdownMenuItem(
                        text = { Text(filterType.name.replace('_', ' ')) },
                        onClick = {
                            onFilterSelected(filterType)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun KpiSection(calls: List<CallData>) {
    val totalCalls = calls.size
    val missedCalls = calls.count { !it.answered }
    val connectedCalls = totalCalls - missedCalls
    val totalDuration = calls.sumOf { it.duration }

    val kpiData = listOf(
        "Total Calls" to totalCalls.toString(),
        "Connected" to connectedCalls.toString(),
        "Missed Calls" to missedCalls.toString(),
        "Total Duration" to "${totalDuration / 60}m ${totalDuration % 60}s"
    )

    Column {
        Text("Key Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            kpiData.forEach {
                KpiItem(title = it.first, value = it.second, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun KpiItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalyticsPager(calls: List<CallData>) {
    val chartTitles = listOf(
        "Volume & Device",
        "Inbound vs. Outbound",
        "Call Outcome",
        "Response Efficiency",
        "Staffing Demand",
        "Performance Growth",
        "Regional Sourcing",
        "Call Length",
        "Agent Competency",
        "SLA Monitoring"
    )
    val pagerState = rememberPagerState(pageCount = { chartTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Column {
        Text("Charts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))

        if (calls.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No data available to display charts.",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            chartTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) { page ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> VolumeDeviceTrendsChart(calls)
                    1 -> InboundOutboundSplitChart(calls)
                    2 -> CallOutcomeBreakdownChart(calls)
                    3 -> ResponseEfficiencyChart(calls)
                    4 -> StaffingDemandChart(calls)
                    5 -> PerformanceGrowthChart(calls)
                    6 -> RegionalLeadSourcingChart(calls)
                    7 -> CallLengthDensityChart(calls)
                    8 -> AgentCompetencyProfileChart(calls)
                    9 -> SlaTargetMonitoringChart(calls)
                }
            }
        }
    }
}

@Composable
fun VolumeDeviceTrendsChart(calls: List<CallData>) {
    Text("Volume & Device Trends — Stacked Area Chart (Placeholder)")
}

@Composable
fun InboundOutboundSplitChart(calls: List<CallData>) {
    Text("Inbound vs. Outbound Split — Side-by-Side Bar Plot (Placeholder)")
}

@Composable
fun CallOutcomeBreakdownChart(calls: List<CallData>) {
    val answered = calls.count { it.answered }.toFloat()
    val missed = calls.count { !it.answered }.toFloat()

    val entries = listOf(
        PieEntry(answered, "Answered"),
        PieEntry(missed, "Missed")
    )
    AnalyticsPieChart(entries = entries, colors = listOf(ColorTemplate.rgb("#4CAF50"), ColorTemplate.rgb("#F44336")))
}

@Composable
fun ResponseEfficiencyChart(calls: List<CallData>) {
    Text("Response Efficiency Correlation — Scatter Plot (Placeholder)")
}

@Composable
fun StaffingDemandChart(calls: List<CallData>) {
    Text("Staffing Demand — Activity Heatmap (Placeholder)")
}

@Composable
fun PerformanceGrowthChart(calls: List<CallData>) {
    Text("Long-Term Performance Growth — Line Chart (Placeholder)")
}

@Composable
fun RegionalLeadSourcingChart(calls: List<CallData>) {
    Text("Regional Lead Sourcing — Choropleth (Map) Chart (Placeholder)")
}

@Composable
fun CallLengthDensityChart(calls: List<CallData>) {
    val buckets = mapOf(
        "<1 min" to calls.count { it.duration < 60 }.toFloat(),
        "1-5 mins" to calls.count { it.duration in 60..300 }.toFloat(),
        "5-10 mins" to calls.count { it.duration in 301..600 }.toFloat(),
        ">10 mins" to calls.count { it.duration > 600 }.toFloat()
    )

    val entries = buckets.entries.mapIndexed { index, entry ->
        BarEntry(index.toFloat(), entry.value)
    }
    AnalyticsBarChart(entries = entries, colors = ColorTemplate.JOYFUL_COLORS.toList())
}

@Composable
fun AgentCompetencyProfileChart(calls: List<CallData>) {
    Text("Agent Competency Profile — Radar (Spider) Chart (Placeholder)")
}

@Composable
fun SlaTargetMonitoringChart(calls: List<CallData>) {
    Text("SLA Target Monitoring — Bullet Graph (Placeholder)")
}

@Composable
private fun AnalyticsPieChart(entries: List<PieEntry>, colors: List<Int>) {
    val dataSet = PieDataSet(entries, "").apply {
        this.colors = colors
        setDrawValues(false) // Hide values on slices
    }
    val pieData = PieData(dataSet)
    val labelColor = MaterialTheme.colorScheme.onSurface
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                data = pieData
                description.isEnabled = false
                legend.isWordWrapEnabled = true
                isDrawHoleEnabled = true
                holeRadius = 45f
                transparentCircleRadius = 50f
                setEntryLabelColor(labelColor.toArgb())
                invalidate()
            }
        },
        modifier = Modifier
            .height(180.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun AnalyticsBarChart(entries: List<BarEntry>, colors: List<Int>) {
    val dataSet = BarDataSet(entries, "").apply {
        this.colors = colors
        setDrawValues(false)
    }
    val barData = BarData(dataSet)
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                data = barData
                description.isEnabled = false
                legend.isEnabled = false
                // Customize axes
                xAxis.setDrawGridLines(false)
                xAxis.setDrawAxisLine(false)
                axisLeft.setDrawGridLines(false)
                axisRight.isEnabled = false
                invalidate()
            }
        },
        modifier = Modifier
            .height(180.dp)
            .fillMaxWidth()
    )
}

@Composable
fun CallDataTable(calls: List<CallData>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            Text(
                text = "Call Log",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                TableCell("Time", weight = 3f, isHeader = true)
                TableCell("Duration", weight = 2f, isHeader = true)
                TableCell("Status", weight = 2f, isHeader = true)
                TableCell("Lead Type", weight = 3f, isHeader = true)
            }
            Divider()

            if (calls.isNotEmpty()) {
                calls.forEach { call ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val date = remember(call.timestamp) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = call.timestamp
                            "${cal.get(Calendar.HOUR_OF_DAY)}:${String.format("%02d", cal.get(Calendar.MINUTE))}"
                        }
                        TableCell(date, weight = 3f)
                        TableCell("${call.duration}s", weight = 2f)
                        TableCell(if (call.answered) "Connected" else "Missed", weight = 2f)
                        TableCell(call.leadType, weight = 3f)
                    }
                    Divider()
                }
            } else {
                Text("No calls to display.", modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun RowScope.TableCell(text: String, weight: Float, isHeader: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(12.dp),
        fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
        style = MaterialTheme.typography.bodyMedium,
        color = if(isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
