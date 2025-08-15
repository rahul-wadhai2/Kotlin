package com.jejecomms.realtimechatfeature.ui.loginscreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor.isOnline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication-related operations.
 */
class LoginViewModel(
    private val authRepository: LoginRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        _currentUser.value = firebaseAuth.currentUser
    }

    init {
        authRepository.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.removeAuthStateListener(authStateListener)
    }

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            // Check network connectivity first.
            if (!networkMonitor.isOnline().first()) {
                _uiState.value = LoginUiState
                    .Error(context.getString(R.string.no_internet))
                return@launch
            }

            try {
                authRepository.login(email, password)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signUp(context: Context, email: String, password: String, userName: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            // Check network connectivity first.
            if (!networkMonitor.isOnline().first()) {
                _uiState.value = LoginUiState
                    .Error(context.getString(R.string.no_internet))
                return@launch
            }

            try {
                authRepository.signUp(email, password)
                val user = authRepository.currentUser
                if (user != null) {
                    // Save user data to Firestore after successful sign-up
                    authRepository.saveUserDataToFirestore(
                        uid = user.uid,
                        name = userName,
                        email = user.email ?: "",
                        password = password
                    )
                }
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                authRepository.resetPassword(email)
                _uiState.value = LoginUiState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    /**
     * Reset the UI state to [LoginUiState.Idle].
     */
    fun loginFailed() {
        _uiState.value = LoginUiState.Idle
        _currentUser.value = null
    }

    /**
     * Login success state.
     */
    fun loginSuccess() {
        _uiState.value = LoginUiState.Idle
    }
}