package com.securestream.viewer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val SESSION_PREFS = "securestream_session"
private const val SESSION_EMAIL = "email"
private const val SESSION_LOGGED_IN = "logged_in"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableAppFullscreen()
    setContent { SecureStreamApp() }
  }
  override fun onResume() {
    super.onResume()
    enableAppFullscreen()
  }
  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) enableAppFullscreen()
  }

  fun enterProtectedPlayback() { window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
  fun leaveProtectedPlayback() { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }

  private fun enableAppFullscreen() {
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.setDecorFitsSystemWindows(false)
      window.insetsController?.let { controller ->
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    } else {
      @Suppress("DEPRECATION")
      window.decorView.systemUiVisibility =
        View.SYSTEM_UI_FLAG_FULLSCREEN or
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
  }
}

@Composable
fun SecureStreamApp() {
  val context = LocalContext.current
  var accountEmail by remember { mutableStateOf(loadSavedAccount(context)) }

  MaterialTheme(
    colorScheme = darkColorScheme(
      primary = Color(0xFF22DCC8),
      background = Color(0xFF000000),
      surface = Color(0xFF111819),
      onBackground = Color.White,
      onSurface = Color.White
    )
  ) {
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
      if (accountEmail == null) {
        LoginScreen(
          onLogin = { email ->
            saveAccount(context, email)
            accountEmail = email
          }
        )
      } else {
        HomeScreen(
          accountEmail = accountEmail.orEmpty(),
          onLogout = {
            clearAccount(context)
            accountEmail = null
          }
        )
      }
    }
  }
}

@Composable
fun LoginScreen(onLogin: (String) -> Unit) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var error by remember { mutableStateOf("") }

  Box(
    Modifier
      .fillMaxSize()
      .background(Color.Black)
      .padding(28.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      Image(
        painter = painterResource(id = R.drawable.ic_securestream_lock),
        contentDescription = "SecureStream app icon",
        modifier = Modifier.size(82.dp)
      )
      Spacer(Modifier.height(18.dp))
      Text(
        "SECURESTREAM",
        color = Color(0xFF22DCC8),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center
      )
      Text(
        "Stories worth protecting.",
        color = Color(0xFF9AA3A3),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
      )
      Spacer(Modifier.height(34.dp))
      OutlinedTextField(
        value = email,
        onValueChange = { email = it; error = "" },
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = password,
        onValueChange = { password = it; error = "" },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth()
      )
      if (error.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Text(error, color = Color(0xFFFF8A8A), style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(18.dp))
      Button(
        onClick = {
          if (!email.contains("@") || password.isBlank()) {
            error = "Enter your email and password."
          } else {
            onLogin(email.trim())
          }
        },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Sign in")
      }
    }
  }
}

@Composable
fun HomeScreen(accountEmail: String, onLogout: () -> Unit) {
  val rails = listOf("Continue Watching", "Featured Tonight", "Recently Added", "Original Series")
  Column(
    Modifier
      .fillMaxSize()
      .background(Color(0xFF090D0E))
      .padding(20.dp)
  ) {
    Spacer(Modifier.height(28.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        Text("SECURESTREAM", color = Color(0xFF22DCC8), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Text(accountEmail, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
      }
      TextButton(onClick = onLogout) { Text("Logout") }
    }
    Spacer(Modifier.height(26.dp))
    Text("Stories worth protecting.", style = MaterialTheme.typography.headlineLarge)
    Text("Your catalog is configured by your administrator.", color = Color.Gray)
    Spacer(Modifier.height(30.dp))
    rails.forEach { rail ->
      Text(rail, style = MaterialTheme.typography.titleLarge)
      Spacer(Modifier.height(12.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items((1..5).toList()) {
          Card(Modifier.width(150.dp).height(92.dp)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF203A33)).padding(12.dp)) {
              Text("Title $it")
            }
          }
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

private fun loadSavedAccount(context: Context): String? {
  val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
  return if (prefs.getBoolean(SESSION_LOGGED_IN, false)) prefs.getString(SESSION_EMAIL, null) else null
}

private fun saveAccount(context: Context, email: String) {
  context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
    .edit()
    .putBoolean(SESSION_LOGGED_IN, true)
    .putString(SESSION_EMAIL, email)
    .apply()
}

private fun clearAccount(context: Context) {
  context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
    .edit()
    .clear()
    .apply()
}
