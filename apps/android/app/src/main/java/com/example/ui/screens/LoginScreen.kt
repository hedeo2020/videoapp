package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SecureDarkBackground
import com.example.ui.theme.SecureDarkSurface
import com.example.ui.theme.SecureMintAccent
import com.example.ui.theme.SecureTextGray
import com.example.ui.theme.SecureTextWhite
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.SecureStreamViewModel

@Composable
fun LoginScreen(
    viewModel: SecureStreamViewModel,
    modifier: Modifier = Modifier
) {
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SecureDarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branded Logo Header
            Icon(
                painter = painterResource(id = R.drawable.ic_secure_lock),
                contentDescription = "SecureStream Logo",
                tint = SecureMintAccent,
                modifier = Modifier
                    .size(96.dp)
                    .testTag("app_logo")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SECURESTREAM",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = SecureMintAccent,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Stories worth protecting.",
                style = MaterialTheme.typography.bodyLarge,
                color = SecureTextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SecureDarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        color = SecureTextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecureMintAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = SecureMintAccent,
                            unfocusedLabelColor = SecureTextGray,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = SecureTextGray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecureMintAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = SecureMintAccent,
                            unfocusedLabelColor = SecureTextGray,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message
                    AnimatedVisibility(
                        visible = authState is AuthState.Error,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        val message = (authState as? AuthState.Error)?.message ?: ""
                        Text(
                            text = message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .testTag("login_error_message")
                        )
                    }

                    // Login Button
                    Button(
                        onClick = { viewModel.login(email.trim(), password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_button"),
                        enabled = authState !is AuthState.Loading && email.isNotEmpty() && password.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecureMintAccent,
                            contentColor = SecureDarkBackground,
                            disabledContainerColor = SecureMintAccent.copy(alpha = 0.3f),
                            disabledContentColor = SecureDarkBackground.copy(alpha = 0.5f)
                        )
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = SecureDarkBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SecureStream is a secure, viewer-only video client. Admin configurations cannot be performed from this app.",
                style = MaterialTheme.typography.bodySmall,
                color = SecureTextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
