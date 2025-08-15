package com.jejecomms.realtimechatfeature.ui.loginscreen

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    object PasswordResetSent : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}