package com.example.inoconnect.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.NetworkUser
import com.example.inoconnect.data.User
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

    // --- NEW: Lists for Connections & Following ---
    private val _connectionList = MutableStateFlow<List<User>>(emptyList())
    val connectionList: StateFlow<List<User>> = _connectionList.asStateFlow()

    private val _followingList = MutableStateFlow<List<User>>(emptyList())
    val followingList: StateFlow<List<User>> = _followingList.asStateFlow()

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

    // --- NEW: Load Full User Lists for Sheets ---
    fun loadConnections() {
        viewModelScope.launch {
            val uid = repository.currentUserId ?: return@launch
            val currentUser = repository.getUserById(uid)
            val ids = currentUser?.connectionIds ?: emptyList()
            if (ids.isNotEmpty()) {
                _connectionList.value = repository.getUsersByIds(ids)
            } else {
                _connectionList.value = emptyList()
            }
        }
    }

    fun loadFollowing() {
        viewModelScope.launch {
            val uid = repository.currentUserId ?: return@launch
            val currentUser = repository.getUserById(uid)
            val ids = currentUser?.followingIds ?: emptyList()
            if (ids.isNotEmpty()) {
                _followingList.value = repository.getUsersByIds(ids)
            } else {
                _followingList.value = emptyList()
            }
        }
    }

    fun connectWithUser(toUserId: String) {
        viewModelScope.launch {
            val success = repository.sendConnectionRequest(toUserId)
            if (success) {
                // Optimistic Update
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
        _suggestedUsers.value = _suggestedUsers.value.filter { it.user.userId != userId }
    }
}