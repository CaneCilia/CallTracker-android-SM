package com.example.calltracker.ui.pages

import android.Manifest
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserPage() {
    val navController = rememberNavController()

    val context = LocalContext.current
    var callLogs by remember { mutableStateOf<List<CallLogItem>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            callLogs = loadCallLogs(context)
            Log.d("CALL_LOGS", "Loaded: ${callLogs.size}")
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
    }

    LaunchedEffect(Unit) {
        FirebaseApp.initializeApp(context)

        if (Firebase.auth.currentUser == null) {
            Firebase.auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("AUTH", "Anonymous login success")
                }
                .addOnFailureListener {
                    Log.e("AUTH", "Login failed", it)
                }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                val items = listOf(
                    BottomNavItem.Home,                    BottomNavItem.Analytics,                    BottomNavItem.Settings
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = navController.currentBackStackEntry?.destination?.route == screen.route,
                        onClick = { navController.navigate(screen.route) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreenContent(
                    callLogs = callLogs,
                    hasPermission = hasPermission,
                    isLoading = isLoading,
                    padding = padding,
                    isSyncing = isSyncing,
                    onSyncClick = {
                        isSyncing = true
                        uploadCallLogsToFirestore(
                            logs = callLogs,
                            onSuccess = { count ->
                                coroutineScope.launch {
                                    Toast.makeText(context, "$count call logs updated", Toast.LENGTH_SHORT).show()
                                    isSyncing = false
                                }
                            },
                            onFailure = { exception ->
                                coroutineScope.launch {
                                    Toast.makeText(context, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    isSyncing = false
                                }
                            }
                        )
                    }
                )
            }
            composable("analytics") { AnalyticsScreen(callLogs) }
            composable("settings") { SettingsScreen() }
        }
    }
}