package com.jejecomms.realtimechatfeature.ui.loginscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jejecomms.realtimechatfeature.data.repository.LoginRepository
import com.jejecomms.realtimechatfeature.utils.NetworkMonitor

/**
 * Factory for creating a [LoginViewModel] with a [LoginRepository] dependency.
 */
class LoginViewModelFactory(private val loginRepository: LoginRepository,
                            private val networkMonitor: NetworkMonitor) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(loginRepository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}