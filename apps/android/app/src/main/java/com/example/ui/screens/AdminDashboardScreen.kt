package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.api.AdminConversationDto
import com.example.data.api.AdminDeviceSessionDto
import com.example.data.api.AdminSystemStatusDto
import com.example.data.api.AdminUserDto
import com.example.ui.theme.SecureDarkBackground
import com.example.ui.theme.SecureDarkSurface
import com.example.ui.theme.SecureMintAccent
import com.example.ui.theme.SecureTextGray
import com.example.ui.theme.SecureTextWhite
import com.example.ui.viewmodel.AdminDashboardState
import com.example.ui.viewmodel.SecureStreamViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: SecureStreamViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.adminDashboardState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Users", "Messages", "Devices")

    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
        while (true) {
            delay(15000)
            viewModel.loadAdminDashboard()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                        Text("Mobile control center", color = SecureTextGray, style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SecureMintAccent)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAdminDashboard() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = SecureMintAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecureDarkBackground)
            )
        },
        containerColor = SecureDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = SecureDarkBackground,
                contentColor = SecureMintAccent,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (val current = state) {
                is AdminDashboardState.Idle,
                is AdminDashboardState.Loading -> LoadingAdminState()
                is AdminDashboardState.Error -> ErrorAdminState(current.message)
                is AdminDashboardState.Success -> {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(current.systemStatus, current.users, current.conversations, current.deviceSessions)
                        1 -> AdminUsersTab(current.users)
                        2 -> AdminMessagesTab(current.conversations)
                        3 -> AdminDevicesTab(current.deviceSessions)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingAdminState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SecureMintAccent)
    }
}

@Composable
private fun ErrorAdminState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        AdminCard {
            Text("Admin dashboard unavailable", color = SecureTextWhite, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = SecureTextGray)
        }
    }
}

@Composable
private fun AdminOverviewTab(
    system: AdminSystemStatusDto?,
    users: List<AdminUserDto>,
    conversations: List<AdminConversationDto>,
    devices: List<AdminDeviceSessionDto>
) {
    val unread = conversations.sumOf { it.unreadCount ?: 0 }
    val activeUsers = users.count { it.status == "ACTIVE" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Users", "$activeUsers/${users.size}", "active / total", Icons.Filled.People, Modifier.weight(1f))
                AdminMetricCard("Unread", "$unread", "messages", Icons.Filled.SupportAgent, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                AdminMetricCard("Devices", "${devices.size}", "active sessions", Icons.Filled.Devices, Modifier.weight(1f))
                AdminMetricCard("CPU", "${system?.cpu?.cores ?: "-"}", "cores", Icons.Filled.Memory, Modifier.weight(1f))
            }
        }
        item {
            UsageCard("Memory", system?.memory?.usedPercent ?: 0, formatBytes(system?.memory?.usedBytes), formatBytes(system?.memory?.totalBytes))
        }
        item {
            UsageCard("Storage", system?.storage?.usedPercent ?: 0, formatBytes(system?.storage?.usedBytes), formatBytes(system?.storage?.totalBytes))
        }
        item {
            AdminCard {
                Text("Server", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                DetailLine("Host", system?.host?.hostname ?: "Unknown")
                DetailLine("Platform", listOfNotNull(system?.host?.platform, system?.host?.arch).joinToString(" ").ifBlank { "Unknown" })
                DetailLine("Network interfaces", "${system?.network?.interfaceCount ?: 0}")
                DetailLine("Updated", prettyDate(system?.checkedAt))
            }
        }
    }
}

@Composable
private fun AdminUsersTab(users: List<AdminUserDto>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(users) { user ->
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(user.status == "ACTIVE")
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName ?: user.email, color = SecureTextWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(user.email, color = SecureTextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(user.role ?: "USER", color = SecureMintAccent, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
                DetailLine("Status", user.status ?: "Unknown")
                DetailLine("Restricted", if (user.accessRestricted == true) "Yes" else "No")
            }
        }
    }
}

@Composable
private fun AdminMessagesTab(conversations: List<AdminConversationDto>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(conversations) { conversation ->
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(conversation.user?.displayName ?: conversation.user?.email ?: "Viewer", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                        Text(conversation.lastMessage?.body ?: "No messages yet", color = SecureTextGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    val unread = conversation.unreadCount ?: 0
                    if (unread > 0) {
                        Text("$unread new", color = SecureDarkBackground, modifier = Modifier.background(SecureMintAccent, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                DetailLine("Updated", prettyDate(conversation.updatedAt))
            }
        }
    }
}

@Composable
private fun AdminDevicesTab(devices: List<AdminDeviceSessionDto>) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(devices) { session ->
            AdminCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = SecureMintAccent)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.user?.email ?: "Unknown user", color = SecureTextWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(session.deviceId ?: "Unknown device", color = SecureTextGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(8.dp))
                DetailLine("Last used", prettyDate(session.lastUsedAt))
                DetailLine("Expires", prettyDate(session.expiresAt))
            }
        }
    }
}

@Composable
private fun AdminMetricCard(title: String, value: String, caption: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    AdminCard(modifier = modifier) {
        Icon(icon, contentDescription = null, tint = SecureMintAccent)
        Spacer(Modifier.height(10.dp))
        Text(value, color = SecureTextWhite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(title, color = SecureTextWhite, fontWeight = FontWeight.Bold)
        Text(caption, color = SecureTextGray, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UsageCard(title: String, percent: Int, used: String, total: String) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Storage, contentDescription = null, tint = SecureMintAccent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Text("$used used of $total", color = SecureTextGray, style = MaterialTheme.typography.labelMedium)
            }
            Text("$percent%", color = SecureMintAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (percent.coerceIn(0, 100) / 100f) },
            color = SecureMintAccent,
            trackColor = SecureDarkBackground,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AdminCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SecureDarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SecureTextGray, modifier = Modifier.weight(1f))
        Text(value, color = SecureTextWhite, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .height(12.dp)
            .width(12.dp)
            .background(if (active) SecureMintAccent else Color.Red, RoundedCornerShape(999.dp))
    )
}

private fun formatBytes(value: Long?): String {
    val bytes = value ?: return "-"
    val gb = bytes / 1024.0 / 1024.0 / 1024.0
    return if (gb >= 1) String.format("%.1f GB", gb) else String.format("%.0f MB", bytes / 1024.0 / 1024.0)
}

private fun prettyDate(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return value.replace("T", " ").replace(Regex("\\.\\d+Z$"), " UTC").replace("Z", " UTC")
}
