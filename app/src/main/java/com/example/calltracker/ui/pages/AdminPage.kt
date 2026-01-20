package com.example.calltracker.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calltracker.data.CallData
import com.example.calltracker.viewmodels.AdminViewModel

enum class DateFilterType {
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

@Composable
fun AdminPage(viewModel: AdminViewModel = viewModel()) {
    val calls = viewModel.filteredCalls.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SummaryView(calls = calls)
        }
    }
}

@Composable
fun SummaryView(calls: List<CallData>) {
    Column {
        Text("Summary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Total Reach",
                value = calls.distinctBy { it.id }.size.toString(),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Connectivity Rate",
                value = if (calls.isNotEmpty()) {
                    "${String.format("%.1f", (calls.count { it.answered }.toFloat() / calls.size.toFloat()) * 100)}%"
                } else {
                    "0%"
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Avg. Handling Time",
                value = if (calls.isNotEmpty()) {
                    "${String.format("%.2f", calls.sumOf { it.duration }.toFloat() / calls.size)}s"
                } else {
                    "0s"
                },
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Lead Conversion %",
                value = if (calls.isNotEmpty()) {
                    "${String.format("%.1f", (calls.count { it.satisfactionScore > 3 }.toFloat() / calls.size.toFloat()) * 100)}%"
                } else {
                    "0%"
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
