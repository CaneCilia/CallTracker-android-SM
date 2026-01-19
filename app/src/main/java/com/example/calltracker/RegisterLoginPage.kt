package com.example.calltracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterLoginPage(
    onNavigateToLogin: () -> Unit = {},  // Callback to navigate to Login Page
    onGoogleClick: () -> Unit = {}       // Google login callback (optional)
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var deviceName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))
            Text("Welcome", fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Register your Login", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = deviceName,
                onValueChange = { deviceName = it },
                label = { Text("Device Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("User Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(image, contentDescription = null)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button

                    if (deviceName.isBlank() || username.isBlank() || email.isBlank() || password.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Please fill all fields") }
                        return@Button
                    }

                    if (password.length < 6) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Password must be at least 6 characters") }
                        return@Button
                    }

                    isLoading = true

                    val auth = Firebase.auth
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                val firebaseUser = auth.currentUser
                                val userId = firebaseUser?.uid
                                if (userId != null) {
                                    // Firestore reference
                                    val firestore = Firebase.firestore
                                    val user = hashMapOf(
                                        "deviceName" to deviceName,
                                        "username" to username,
                                        "email" to email
                                    )
                                    firestore.collection("users").document(userId)
                                        .set(user)
                                        .addOnSuccessListener {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Registration successful!")
                                            }
                                            onNavigateToLogin()
                                        }
                                        .addOnFailureListener { error ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Failed to save user data: ${error.message}")
                                            }
                                        }
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Registration failed: ${task.exception?.localizedMessage}")
                                }
                            }
                        }

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Registering..." else "Register")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onGoogleClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue with Google")
            }
        }
    }
}
