package com.jejecomms.realtimechatfeature.ui.loginscreen

import android.view.Gravity
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jejecomms.realtimechatfeature.R
import com.jejecomms.realtimechatfeature.ui.theme.LightGreen
import com.jejecomms.realtimechatfeature.utils.ToastUtils.showCustomToast
import kotlinx.coroutines.delay

/**
 * Composable function for the login screen.
 */
@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
) {
    /**
     * State variables for the login form.
     */
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    /**
     * State for validation errors.
     */
    var isUserNameError by remember { mutableStateOf(false) }
    var userNameErrorMessage by remember { mutableStateOf("") }
    var isEmailError by remember { mutableStateOf(false) }
    var emailErrorMessage by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf("") }

    val uiState by loginViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        when (uiState) {
            LoginUiState.Success -> {
                loginViewModel.loginSuccess()
                onLoginSuccess()
            }
            LoginUiState.PasswordResetSent -> {
                showCustomToast(
                    context = context,
                    message = context.getString(R.string.password_reset_sent),
                    gravity = Gravity.CENTER,
                    duration = Toast.LENGTH_LONG
                )
            }
            is LoginUiState.Error -> {
                showCustomToast(
                    context = context,
                    message = (uiState as LoginUiState.Error).message,
                    gravity = Gravity.CENTER,
                    duration = Toast.LENGTH_LONG
                )
                loginViewModel.loginFailed()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val bounceAnimation by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .size(116.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(100.dp * bounceAnimation),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LightGreen)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "RealTimeChat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                    isUserNameError = false
                    userNameErrorMessage = ""
                },
                label = { Text("User Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading,
                isError = isUserNameError,
                supportingText = {
                    if (isUserNameError) {
                        Text(text = userNameErrorMessage)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    isEmailError = false
                    emailErrorMessage = ""
                },
                label = { Text("Email ID") },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading,
                isError = isEmailError,
                supportingText = {
                    if (isEmailError) {
                        Text(text = emailErrorMessage)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    isPasswordError = false
                    passwordErrorMessage = ""
                },
                label = { Text("Password") },
                visualTransformation = if (isPasswordVisible) VisualTransformation
                    .None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading,
                isError = isPasswordError,
                supportingText = {
                    if (isPasswordError) {
                        Text(text = passwordErrorMessage)
                    }
                },
                trailingIcon = {
                    val image = if (isPasswordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff
                    val description = if (isPasswordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    // Reset all errors first
                    isEmailError = false
                    isPasswordError = false
                    // Validate fields
                    val isEmailValid = email.isNotBlank() && isValidEmail(email)
                    val isPasswordValid = password.isNotBlank()

                    if (isEmailValid && isPasswordValid) {
                        loginViewModel.login(context, email, password)
                    } else {
                        if (!isEmailValid) {
                            isEmailError = true
                            emailErrorMessage = if (email.isBlank()) "Email is required" else "Invalid email format"
                        }
                        if (!isPasswordValid) {
                            isPasswordError = true
                            passwordErrorMessage = "Password is required"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading
            ) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // Reset all errors first
                    isUserNameError = false
                    isEmailError = false
                    isPasswordError = false
                    // Validate fields
                    val isUserNameValid = userName.isNotBlank()
                    val isEmailValid = email.isNotBlank() && isValidEmail(email)
                    val isPasswordValid = password.isNotBlank()

                    if (isUserNameValid && isEmailValid && isPasswordValid) {
                        loginViewModel.signUp(context, email, password, userName)
                    } else {
                        if (!isUserNameValid) {
                            isUserNameError = true
                            userNameErrorMessage = "User Name is required"
                        }
                        if (!isEmailValid) {
                            isEmailError = true
                            emailErrorMessage = if (email.isBlank()) "Email is required" else "Invalid email format"
                        }
                        if (!isPasswordValid) {
                            isPasswordError = true
                            passwordErrorMessage = "Password is required"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading
            ) {
                Text("Sign Up")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = {
                    // Reset email error
                    isEmailError = false
                    if (email.isNotBlank()) {
                        if (isValidEmail(email)) {
                            loginViewModel.resetPassword(email)
                        } else {
                            isEmailError = true
                            emailErrorMessage = "Invalid email format"
                        }
                    } else {
                        isEmailError = true
                        emailErrorMessage = "Email is required to reset password"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState != LoginUiState.Loading
            ) {
                Text("Forgot Password?")
            }
        }

        if (uiState == LoginUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
        }
    }
}

/**
 * A simple email validation function.
 */
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}





