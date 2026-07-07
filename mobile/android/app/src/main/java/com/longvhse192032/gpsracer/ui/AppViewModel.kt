package com.longvhse192032.gpsracer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longvhse192032.gpsracer.data.ApiClient
import com.longvhse192032.gpsracer.data.AuthResponse
import com.longvhse192032.gpsracer.data.SessionStore
import com.longvhse192032.gpsracer.data.TrackingResult
import com.longvhse192032.gpsracer.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val user: UserProfile? = null,
    val token: String? = null,
    val latestResult: TrackingResult? = null,
    val isLoadingSession: Boolean = true,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionStore = SessionStore(application)
    private val _latestResult = MutableStateFlow<TrackingResult?>(null)
    private val _appState = MutableStateFlow(AppUiState(isLoadingSession = true))
    val appState: StateFlow<AppUiState> = _appState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.sessionFlow.collect { (user, token) ->
                if (token != null) ApiClient.setAuthToken(token)
                _appState.value = AppUiState(
                    user = user,
                    token = token,
                    latestResult = _latestResult.value,
                    isLoadingSession = false,
                )
            }
        }
    }

    fun setLatestResult(result: TrackingResult?) {
        _latestResult.value = result
        _appState.value = _appState.value.copy(latestResult = result)
    }

    suspend fun onAuthSuccess(response: AuthResponse) {
        sessionStore.saveSession(response.user, response.accessToken)
    }

    suspend fun saveUser(user: UserProfile) {
        sessionStore.saveUser(user)
        _appState.value = _appState.value.copy(user = user)
    }

    suspend fun clearAuth() {
        sessionStore.clear()
        _latestResult.value = null
        _appState.value = AppUiState(isLoadingSession = false)
    }
}
