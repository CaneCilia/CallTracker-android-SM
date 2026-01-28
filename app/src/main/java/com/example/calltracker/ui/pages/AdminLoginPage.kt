package com.example.calltracker.ui.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun AdminLoginPage(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Admin Login",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val inputUser = username.trim()
                val inputPass = password.trim()

                if (inputUser.isNotEmpty() && inputPass.isNotEmpty()) {
                    isLoading = true
                    val db = Firebase.firestore

                    // Query the entire admincredits collection for matching credentials
                    // This will check every document (admin, adminone, etc.)
                    db.collection("admincredits")
                        .whereEqualTo("username", inputUser)
                        .whereEqualTo("password", inputPass)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            isLoading = false
                            if (!querySnapshot.isEmpty) {
                                // Successfully authenticated if any document matches
                                Log.d("AdminLogin", "Login successful for: $inputUser")
                                navController.navigate("admin")
                            } else {
                                Log.d("AdminLogin", "No matching document found in admincredits")
                                Toast.makeText(context, "Invalid Admin Credentials", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Log.e("AdminLogin", "Firestore Error: ${e.message}")
                            Toast.makeText(context, "Login Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Please fill all fields.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Verifying..." else "Login")
        }
    }
}
