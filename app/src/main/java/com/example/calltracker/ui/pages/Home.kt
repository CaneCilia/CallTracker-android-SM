package com.example.calltracker.ui.pages

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallMissedOutgoing
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreenContent(
    callLogs: List<CallLogItem>,
    hasPermission: Boolean,
    isLoading: Boolean,
    padding: PaddingValues,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    // Local state to manage the logs with their fetched statuses
    var displayedLogs by remember(callLogs) { mutableStateOf(callLogs) }
    var isFetching by remember { mutableStateOf(false) }

    // Fetch lead statuses from Firestore when logs are loaded
    LaunchedEffect(callLogs) {
        if (callLogs.isNotEmpty()) {
            isFetching = true
            fetchLeadStatuses(callLogs) {
                // Create a new list reference to trigger recomposition
                displayedLogs = callLogs.toList()
                isFetching = false
            }
        }
    }

    val stats = calculateStats(displayedLogs)
    
    when {
        isLoading -> LoadingView()
        !hasPermission -> PermissionView(padding)
        callLogs.isEmpty() -> EmptyView(padding)
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Home", style = MaterialTheme.typography.headlineSmall)
                        if (isFetching) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                    SyncButton(onClick = onSyncClick, isSyncing = isSyncing)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    item { StatsSection(stats) }

                    items(displayedLogs) { call ->
                        CallLogCard(
                            log = call,
                            onUpdate = {
                                displayedLogs = displayedLogs.toList()
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun fetchLeadStatuses(logs: List<CallLogItem>, onComplete: () -> Unit) {
    val user = Firebase.auth.currentUser ?: return
    val db = Firebase.firestore
    
    db.collection("users")
        .document(user.uid)
        .collection("phoneStatus")
        .get()
        .addOnSuccessListener { querySnapshot ->
            val statusMap = querySnapshot.documents.associate { 
                it.id to it.getString("status") 
            }
            
            logs.forEach { log ->
                val statusName = statusMap[log.number]
                if (statusName != null) {
                    try {
                        log.leadStatus = LeadStatus.valueOf(statusName)
                    } catch (e: Exception) {
                        Log.e("FIRESTORE", "Invalid status: $statusName")
                    }
                }
            }
            onComplete()
        }
        .addOnFailureListener {
            onComplete()
        }
}

@Composable
fun StatsSection(stats: CallStats) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "This Week",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item { StatMetric("Missed", stats.missed.toString(), Icons.Default.CallMissedOutgoing) }
                item { StatMetric("Attended", stats.attended.toString(), Icons.Default.Phone) }
                item { StatMetric("Avg/Day", String.format("%.1f", stats.avgPerDay), Icons.Default.TrendingUp) }
            }
        }
    }
}

@Composable
fun StatMetric(title: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CallLogCard(log: CallLogItem, onUpdate: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDialog) {
        AddTagDialog(
            onDismiss = { showDialog = false },
            onAddTag = { tag ->
                log.tags.add(tag)
                updateFirestoreCallLog(log)
                onUpdate()
                showDialog = false
            }
        )
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CallTypeAvatar(log.type)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.name ?: log.number ?: "Unknown Caller",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    DurationBadge(log.duration)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatDate(log.dateTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    log.tags.forEach { tag ->
                        TagChip(tag = tag)
                    }
                    IconButton(onClick = { showDialog = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Add Tag")
                    }
                    IconButton(
                        onClick = { openWhatsApp(context, log.number) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "WhatsApp")
                    }
                }

                LeadStatusTracker(log) { newStatus ->
                    log.leadStatus = newStatus
                    updateFirestoreCallLog(log)
                    // Update global lead status for this phone number
                    updateGlobalPhoneNumberStatus(log.number, newStatus)
                    onUpdate()
                }
            }
        }
    }
}

private fun updateFirestoreCallLog(log: CallLogItem) {
    val user = Firebase.auth.currentUser ?: return
    val db = Firebase.firestore
    
    db.collection("users")
        .document(user.uid)
        .collection("calldata")
        .whereEqualTo("number", log.number)
        .whereEqualTo("dateTime", log.dateTime)
        .get()
        .addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                document.reference.update(
                    "leadStatus", log.leadStatus.name,
                    "tags", log.tags
                ).addOnSuccessListener {
                    Log.d("FIRESTORE", "Successfully updated lead status/tags")
                }.addOnFailureListener { e ->
                    Log.e("FIRESTORE", "Error updating document", e)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e("FIRESTORE", "Error finding document", e)
        }
}

private fun updateGlobalPhoneNumberStatus(number: String?, status: LeadStatus) {
    if (number.isNullOrBlank()) return
    val user = Firebase.auth.currentUser ?: return
    val db = Firebase.firestore
    
    val data = hashMapOf(
        "status" to status.name,
        "updatedAt" to com.google.firebase.Timestamp.now()
    )
    
    db.collection("users")
        .document(user.uid)
        .collection("phoneStatus")
        .document(number)
        .set(data)
        .addOnSuccessListener {
            Log.d("FIRESTORE", "Global phone status updated for $number")
        }
}

@Composable
fun TagChip(tag: String) {
    Surface(
        modifier = Modifier.padding(end = 4.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTagDialog(onDismiss: () -> Unit, onAddTag: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a Tag") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Tag") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onAddTag(text)
                    }
                }
            ) {
                Text("Add")
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
fun CallTypeAvatar(type: String) {
    val (bgColor, icon) = when (type) {
        "Missed" -> Pair(Color(0xFFFEF2F2), Icons.Default.CallMissed)
        "Incoming" -> Pair(Color(0xFFF0FDF4), Icons.Default.CallReceived)
        "Outgoing" -> Pair(Color(0xFFEEF2FF), Icons.Default.CallMade)
        "Rejected" -> Pair(Color(0xFFFFEBEE), Icons.Default.CallEnd)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, Icons.Default.Phone)
    }

    Surface(
        color = bgColor,
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DurationBadge(duration: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
    ) {
        Text(
            text = "$duration s",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun PermissionView(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Grant Call Log Permission",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Allow access to view your call history",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EmptyView(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.PhoneMissed,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No call logs found",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading call logs...")
        }
    }
}

@Composable
fun LeadStatusTracker(log: CallLogItem, onStatusChange: (LeadStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(log.leadStatus.status)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            LeadStatus.values().forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.status) },
                    onClick = {
                        onStatusChange(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SyncButton(onClick: () -> Unit, isSyncing: Boolean) {
    IconButton(onClick = onClick) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Icon(Icons.Default.Sync, contentDescription = "Sync")
        }
    }
}


fun openWhatsApp(context: Context, number: String?) {
    if (number.isNullOrBlank()) return
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$number")
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
    }
}
