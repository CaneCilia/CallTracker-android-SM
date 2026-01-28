package com.example.calltracker.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class UserPerformance(
    val userId: String = "",
    val name: String = "Unknown User",
    val device: String = "Unknown Device",
    val totalCalls: Int = 0,
    val totalDuration: Long = 0,
    val connectedCalls: Int = 0,
    val interestedCount: Int = 0,
    val followUpCount: Int = 0
)

@Composable
fun TeamPerformancePage() {
    var teamData by remember { mutableStateOf<List<UserPerformance>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val db = Firebase.firestore

    fun loadData() {
        isRefreshing = true
        db.collection("users").get()
            .addOnSuccessListener { usersSnapshot ->
                val performanceList = mutableListOf<UserPerformance>()
                var processedUsers = 0
                val totalUsers = usersSnapshot.size()

                if (totalUsers == 0) {
                    isLoading = false
                    isRefreshing = false
                    return@addOnSuccessListener
                }

                usersSnapshot.documents.forEach { userDoc ->
                    val userId = userDoc.id
                    val name = userDoc.getString("username") ?: "Unknown"
                    val device = userDoc.getString("deviceName") ?: "Unknown"

                    db.collection("users").document(userId).collection("calldata").get()
                        .addOnSuccessListener { logsSnapshot ->
                            var calls = 0
                            var duration = 0L
                            var connected = 0
                            var interested = 0
                            var followUp = 0

                            logsSnapshot.documents.forEach { logDoc ->
                                calls++
                                val durStr = logDoc.get("duration")?.toString() ?: "0"
                                val dur = durStr.toLongOrNull() ?: 0L
                                duration += dur
                                if (dur > 0) connected++

                                val status = logDoc.getString("leadStatus")
                                if (status == LeadStatus.INTERESTED.name) interested++
                                if (status == LeadStatus.FOLLOW_UP.name) followUp++
                            }

                            performanceList.add(
                                UserPerformance(
                                    userId, name, device, 
                                    calls, duration, connected,
                                    interested, followUp
                                )
                            )
                            
                            processedUsers++
                            if (processedUsers == totalUsers) {
                                teamData = performanceList.sortedByDescending { it.totalCalls }
                                isLoading = false
                                isRefreshing = false
                            }
                        }
                        .addOnFailureListener {
                            processedUsers++
                            if (processedUsers == totalUsers) {
                                isLoading = false
                                isRefreshing = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
                isRefreshing = false
            }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Team Performance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.TrendingUp, contentDescription = "Refresh")
                    }
                }
            }

            if (teamData.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No team data found")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(teamData) { user ->
                        UserPerformanceCard(user)
                    }
                }
            }
        }
    }
}

@Composable
fun UserPerformanceCard(user: UserPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(user.device, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(Icons.Default.Call, user.totalCalls.toString(), "Total")
                MetricItem(Icons.Default.Timer, "${user.totalDuration / 60}m", "Talk Time")
                val successRate = if (user.totalCalls > 0) (user.connectedCalls * 100 / user.totalCalls) else 0
                MetricItem(null, "$successRate%", "Connected")
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.small).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Interested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.interestedCount.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Follow-up", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(user.followUpCount.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }
    }
}

@Composable
fun MetricItem(icon: ImageVector?, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
