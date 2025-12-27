package com.example.inoconnect.ui.participant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.NetworkUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyNetworkViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // UI State for Suggested Users
    private val _suggestedUsers = MutableStateFlow<List<NetworkUser>>(emptyList())
    val suggestedUsers: StateFlow<List<NetworkUser>> = _suggestedUsers.asStateFlow()

    // UI State for Stats (Connections, Following)
    private val _networkStats = MutableStateFlow<Map<String, Int>>(
        mapOf("connections" to 0, "following" to 0)
    )
    val networkStats: StateFlow<Map<String, Int>> = _networkStats.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 1. Observe Suggested Users (Real-time)
            repository.getSuggestedUsersFlow().collect { users ->
                _suggestedUsers.value = users
            }
        }

        viewModelScope.launch {
            // 2. Observe Stats (Real-time)
            repository.getNetworkStatsFlow().collect { stats ->
                _networkStats.value = stats
            }
        }
    }

    fun connectWithUser(toUserId: String) {
        viewModelScope.launch {
            val success = repository.sendConnectionRequest(toUserId)
            if (success) {
                // Optimistic Update: Update the specific user's status in the list immediately
                _suggestedUsers.value = _suggestedUsers.value.map { networkUser ->
                    if (networkUser.user.userId == toUserId) {
                        networkUser.copy(connectionStatus = "pending_sent")
                    } else {
                        networkUser
                    }
                }
            }
        }
    }

    fun removeSuggestion(userId: String) {
        // Just hide it locally for this session
        _suggestedUsers.value = _suggestedUsers.value.filter { it.user.userId != userId }
    }



}