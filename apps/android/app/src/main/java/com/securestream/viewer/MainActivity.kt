package com.securestream.viewer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { SecureStreamApp() } }
  fun enterProtectedPlayback() { window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
  fun leaveProtectedPlayback() { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
}

@Composable fun SecureStreamApp(){ MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF83F7C5),background=Color(0xFF090D0E),surface=Color(0xFF111819))){ Surface(Modifier.fillMaxSize()){ HomeScreen() } } }
@Composable fun HomeScreen(){ val rails=listOf("Continue Watching","Featured Tonight","Recently Added","Original Series"); Column(Modifier.fillMaxSize().background(Color(0xFF090D0E)).padding(20.dp)){ Spacer(Modifier.height(28.dp)); Text("SECURESTREAM",color=Color(0xFF83F7C5),style=MaterialTheme.typography.titleMedium); Spacer(Modifier.height(26.dp)); Text("Stories worth protecting.",style=MaterialTheme.typography.headlineLarge); Text("Your catalog is configured by your administrator.",color=Color.Gray); Spacer(Modifier.height(30.dp)); rails.forEach{ rail->Text(rail,style=MaterialTheme.typography.titleLarge); Spacer(Modifier.height(12.dp)); LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp)){items((1..5).toList()){Card(Modifier.width(150.dp).height(92.dp)){Box(Modifier.fillMaxSize().background(Color(0xFF203A33)).padding(12.dp)){Text("Title $it")}}}};Spacer(Modifier.height(24.dp))} } }
