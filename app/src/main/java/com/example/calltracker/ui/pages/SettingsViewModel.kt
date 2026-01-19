package com.example.calltracker.ui.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserData(val username: String = "", val deviceName: String = "")

class SettingsViewModel : ViewModel() {

    private val _userData = MutableStateFlow(UserData())
    val userData: StateFlow<UserData> = _userData

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document != null) {
                            val username = document.getString("username") ?: ""
                            val deviceName = document.getString("deviceName") ?: ""
                            _userData.value = UserData(username, deviceName)
                        }
                    }
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}