package com.example.calltracker.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("Analysis Dashboard") },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DateFilterType.values().forEach { filterType ->
                                DropdownMenuItem(
                                    text = { Text(filterType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
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
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(calls = calls, filterType = selectedFilter)
            }
        }
    }
}

@Composable
fun SummaryCard(calls: List<CallData>, filterType: DateFilterType) {
    val missedCalls = calls.count { !it.answered }
    val attendedCalls = calls.count { it.answered }

    val numberOfDays = when (filterType) {
        DateFilterType.TODAY -> 1
        DateFilterType.THIS_WEEK -> 7
        DateFilterType.THIS_MONTH -> Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        DateFilterType.CUSTOM -> {
            if (calls.isNotEmpty()) {
                val minTime = calls.minOf { it.timestamp }
                val maxTime = calls.maxOf { it.timestamp }
                val diff = maxTime - minTime
                val days = (diff / (1000 * 60 * 60 * 24)).toInt()
                if (days > 0) days else 1
            } else {
                1
            }
        }
    }
    val avgPerDay = if (numberOfDays > 0) calls.size.toFloat() / numberOfDays else 0f


    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = filterType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                SummaryItem(icon = Icons.Default.TrendingDown, value = missedCalls.toString(), label = "Missed")
                SummaryItem(icon = Icons.Default.Call, value = attendedCalls.toString(), label = "Attended")
                SummaryItem(icon = Icons.Default.TrendingUp, value = String.format("%.1f", avgPerDay), label = "Avg/Day")
            }
        }
    }
}

@Composable
fun SummaryItem(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)
    }
}
