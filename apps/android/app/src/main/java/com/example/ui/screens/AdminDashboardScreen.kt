package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.viewmodel.AdminPanelData
import com.example.ui.viewmodel.AdminUploadState
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
    val tabs = listOf(
        "Overview",
        "Catalog",
        "Videos",
        "Video Editor",
        "File Manager",
        "Storage",
        "Series",
        "Uploads",
        "Processing",
        "Collections",
        "Users",
        "Device Sessions",
        "Messages",
        "Notifications",
        "API Tokens",
        "Playback sessions",
        "Watermark Trace",
        "Backup & Restore",
        "Activity",
        "Trash",
        "Audit logs",
        "Security",
        "Settings"
    )

    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
        while (true) {
            delay(5000)
            viewModel.loadAdminDashboard(silent = true)
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
                    IconButton(onClick = { viewModel.loadAdminDashboard(silent = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = SecureMintAccent)
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out",
                            tint = SecureMintAccent
                        )
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
                    when (val tab = tabs[selectedTab]) {
                        "Overview" -> AdminOverviewTab(current.systemStatus, current.users, current.conversations, current.deviceSessions)
                        "Users" -> AdminUsersTab(viewModel, current.users)
                        "Messages" -> AdminMessagesTab(viewModel, current.conversations)
                        "Device Sessions" -> AdminDevicesTab(viewModel, current.deviceSessions)
                        else -> GenericAdminPanel(viewModel, tab, current.panels[tab])
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
                AdminMetricCard("Server", system?.host?.hostname ?: "-", "current host", Icons.Filled.Memory, Modifier.weight(1f))
            }
        }
        item {
            UsageCard("CPU", system?.cpu?.usedPercent ?: 0, "${system?.cpu?.cores ?: "-"} cores", "current load")
        }
        item {
            UsageCard("Memory", system?.memory?.usedPercent ?: 0, formatBytes(system?.memory?.usedBytes), "of ${formatBytes(system?.memory?.totalBytes)}")
        }
        item {
            UsageCard("Storage", system?.storage?.usedPercent ?: 0, formatBytes(system?.storage?.usedBytes), "of ${formatBytes(system?.storage?.totalBytes)}")
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
private fun AdminUsersTab(viewModel: SecureStreamViewModel, users: List<AdminUserDto>) {
    val context = LocalContext.current
    val ok: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val fail: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
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
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminActionButton(
                        label = if (user.status == "ACTIVE") "Suspend" else "Activate",
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.adminSetUserStatus(user.id, if (user.status == "ACTIVE") "SUSPENDED" else "ACTIVE", ok, fail)
                    }
                    AdminDangerButton(label = "Delete", modifier = Modifier.weight(1f)) {
                        viewModel.adminDeleteUser(user.id, ok, fail)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMessagesTab(viewModel: SecureStreamViewModel, conversations: List<AdminConversationDto>) {
    val context = LocalContext.current
    val ok: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val fail: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(conversations) { conversation ->
            var reply by remember(conversation.id) { mutableStateOf("") }
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
                Spacer(Modifier.height(10.dp))
                AdminTextField(value = reply, onValueChange = { reply = it }, label = "Reply")
                Spacer(Modifier.height(8.dp))
                AdminActionButton(label = "Send reply", enabled = reply.isNotBlank()) {
                    viewModel.adminReplyToConversation(conversation.id, reply, { message ->
                        reply = ""
                        ok(message)
                    }, fail)
                }
            }
        }
    }
}

@Composable
private fun AdminDevicesTab(viewModel: SecureStreamViewModel, devices: List<AdminDeviceSessionDto>) {
    val context = LocalContext.current
    val ok: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val fail: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
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
                Spacer(Modifier.height(10.dp))
                AdminDangerButton(label = "Revoke session") {
                    viewModel.adminRevokeDeviceSession(session.id, ok, fail)
                }
            }
        }
    }
}

@Composable
private fun GenericAdminPanel(viewModel: SecureStreamViewModel, title: String, panel: AdminPanelData?) {
    if (panel == null) {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            item {
                AdminCard {
                    Text(title, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("This panel is ready for mobile UI wiring, but no readable endpoint data was returned.", color = SecureTextGray)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AdminCard {
                Text(panel.title, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(panel.subtitle, color = SecureTextGray)
                if (!panel.mobileNote.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(panel.mobileNote, color = SecureMintAccent, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                DetailLine("Rows", panel.rows.size.toString())
                if (panel.details.isNotEmpty()) {
                    DetailLine("Details", panel.details.size.toString())
                }
            }
        }

        item {
            AdminPanelActions(viewModel, title, panel)
        }

        if (panel.details.isNotEmpty()) {
            item {
                AdminCard {
                    Text("Details", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    panel.details.entries.take(16).forEach { (key, value) ->
                        DetailLine(key, compactValue(value))
                    }
                }
            }
        }

        if (panel.rows.isEmpty() && panel.details.isEmpty()) {
            item {
                AdminCard {
                    Text("No records yet", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("When this panel has data, it will appear here.", color = SecureTextGray)
                }
            }
        } else {
            items(panel.rows.take(80)) { row ->
                GenericRecordCard(row)
            }
        }
    }
}

@Composable
private fun GenericRecordCard(row: Map<String, Any?>) {
    val title = firstString(row, "title", "name", "email", "action", "id") ?: "Record"
    val subtitle = firstString(row, "status", "role", "state", "kind", "createdAt", "updatedAt") ?: ""
    AdminCard {
        Text(title, color = SecureTextWhite, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = SecureMintAccent, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(8.dp))
        row.entries
            .filter { (key, value) -> value != null && key !in setOf("id", "title", "name", "email", "action", "status", "role", "state", "kind") }
            .take(5)
            .forEach { (key, value) -> DetailLine(key, compactValue(value)) }
    }
}

@Composable
private fun AdminPanelActions(viewModel: SecureStreamViewModel, title: String, panel: AdminPanelData) {
    val context = LocalContext.current
    val uploadState by viewModel.adminUploadState.collectAsState()
    val ok: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    val fail: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }

    when (title) {
        "Notifications" -> {
            var notificationTitle by remember { mutableStateOf("") }
            var notificationBody by remember { mutableStateOf("") }
            AdminCard {
                Text("Send notification", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdminTextField(notificationTitle, { notificationTitle = it }, "Title")
                Spacer(Modifier.height(8.dp))
                AdminTextField(notificationBody, { notificationBody = it }, "Message")
                Spacer(Modifier.height(10.dp))
                AdminActionButton(label = "Send to all active viewers", enabled = notificationTitle.isNotBlank() && notificationBody.isNotBlank()) {
                    viewModel.adminSendNotification(notificationTitle, notificationBody, { message ->
                        notificationTitle = ""
                        notificationBody = ""
                        ok(message)
                    }, fail)
                }
            }
        }
        "Backup & Restore" -> {
            AdminCard {
                Text("Backup actions", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminActionButton("Create backup", Modifier.weight(1f)) {
                        viewModel.adminCreateBackup(ok, fail)
                    }
                    AdminActionButton("Run schedule", Modifier.weight(1f)) {
                        viewModel.adminRunScheduledBackupNow(ok, fail)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Restore still requires file selection, so keep restore on the web panel for safety.", color = SecureTextGray, style = MaterialTheme.typography.bodySmall)
            }
        }
        "Settings" -> {
            val maintenanceMap = panel.details["maintenance"] as? Map<*, *>
            val backupMap = panel.details["backupSchedule"] as? Map<*, *>
            val deleteOriginal = panel.details["deleteOriginalAfterPreview"] as? Boolean ?: false
            val maintenance = maintenanceMap?.get("enabled") as? Boolean ?: false
            val backupEnabled = backupMap?.get("enabled") as? Boolean ?: false
            val backupDrive = backupMap?.get("uploadToDrive") as? Boolean ?: false
            var maintenanceMessage by remember { mutableStateOf((maintenanceMap?.get("message") as? String).orEmpty()) }
            AdminCard {
                Text("Settings actions", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdminActionButton(if (deleteOriginal) "Keep originals" else "Delete originals after preview") {
                    viewModel.adminUpdateSettings(deleteOriginalAfterPreview = !deleteOriginal, onSuccess = ok, onError = fail)
                }
                Spacer(Modifier.height(8.dp))
                AdminTextField(maintenanceMessage, { maintenanceMessage = it }, "Maintenance message")
                Spacer(Modifier.height(8.dp))
                AdminActionButton(if (maintenance) "Disable maintenance" else "Enable maintenance") {
                    viewModel.adminUpdateSettings(maintenanceMode = !maintenance, maintenanceMessage = maintenanceMessage, onSuccess = ok, onError = fail)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminActionButton(if (backupEnabled) "Pause backups" else "Enable backups", Modifier.weight(1f)) {
                        viewModel.adminUpdateSettings(backupScheduleEnabled = !backupEnabled, onSuccess = ok, onError = fail)
                    }
                    AdminActionButton(if (backupDrive) "Drive off" else "Drive on", Modifier.weight(1f)) {
                        viewModel.adminUpdateSettings(backupScheduleDrive = !backupDrive, onSuccess = ok, onError = fail)
                    }
                }
            }
        }
        "Security" -> {
            AdminCard {
                Text("Alert actions", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                AdminActionButton("Send test Discord/platform alert") {
                    viewModel.adminTestAlert(ok, fail)
                }
            }
        }
        "Uploads" -> {
            var uploadTitle by remember { mutableStateOf("") }
            var uploadSynopsis by remember { mutableStateOf("") }
            var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
            val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                selectedVideoUri = uri
                if (uploadTitle.isBlank()) {
                    uploadTitle = uri?.lastPathSegment?.substringAfterLast("/")?.substringBeforeLast(".") ?: "Uploaded video"
                }
            }
            AdminCard {
                Text("Mobile upload", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Pick a video from this phone and upload it as a draft. Publish it after preview conversion is ready.", color = SecureTextGray)
                Spacer(Modifier.height(10.dp))
                AdminActionButton(label = if (selectedVideoUri == null) "Choose video" else "Change video") {
                    picker.launch("video/*")
                }
                selectedVideoUri?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Selected: ${it.lastPathSegment ?: "video"}", color = SecureMintAccent, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(8.dp))
                AdminTextField(uploadTitle, { uploadTitle = it }, "Title")
                Spacer(Modifier.height(8.dp))
                AdminTextField(uploadSynopsis, { uploadSynopsis = it }, "Storyline / notes")
                Spacer(Modifier.height(10.dp))
                if (uploadState.running || uploadState.progress > 0) {
                    UploadProgress(uploadState)
                    Spacer(Modifier.height(10.dp))
                }
                AdminActionButton(
                    label = if (uploadState.running) uploadState.phase.ifBlank { "Uploading..." } else "Upload as draft",
                    enabled = !uploadState.running && selectedVideoUri != null && uploadTitle.isNotBlank()
                ) {
                    val uri = selectedVideoUri
                    if (uri != null) {
                        viewModel.adminUploadVideo(uri, uploadTitle, uploadSynopsis, { message ->
                            selectedVideoUri = null
                            uploadTitle = ""
                            uploadSynopsis = ""
                            ok(message)
                        }, fail)
                    }
                }
            }
        }
        "Video Editor" -> {
            val assets = recordList(panel.details["assets"])
            var selectedAssetId by remember { mutableStateOf(firstString(assets.firstOrNull().orEmpty(), "id").orEmpty()) }
            var startSeconds by remember { mutableStateOf("0") }
            var endSeconds by remember { mutableStateOf("") }
            var outputTitle by remember { mutableStateOf("") }
            AdminCard {
                Text("Mobile editor", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Create a trimmed draft copy from an uploaded video. The original file is not changed.", color = SecureTextGray)
                Spacer(Modifier.height(10.dp))
                Text("Choose source", color = SecureTextWhite, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                assets.take(6).forEach { asset ->
                    val assetId = firstString(asset, "id").orEmpty()
                    val label = firstString(asset, "title", "sourceFileName", "previewFileName") ?: assetId
                    AdminActionButton(
                        label = if (assetId == selectedAssetId) "✓ $label" else label,
                        enabled = assetId.isNotBlank()
                    ) {
                        selectedAssetId = assetId
                        if (outputTitle.isBlank()) outputTitle = "$label clip"
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (assets.isEmpty()) {
                    Text("No uploaded video assets yet. Upload a video first.", color = SecureTextGray)
                }
                Spacer(Modifier.height(8.dp))
                AdminTextField(selectedAssetId, { selectedAssetId = it }, "Asset ID")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) { AdminTextField(startSeconds, { startSeconds = it }, "Start sec") }
                    Box(Modifier.weight(1f)) { AdminTextField(endSeconds, { endSeconds = it }, "End sec") }
                }
                Spacer(Modifier.height(8.dp))
                AdminTextField(outputTitle, { outputTitle = it }, "Edited draft title")
                Spacer(Modifier.height(10.dp))
                AdminActionButton(
                    label = "Create trimmed draft",
                    enabled = selectedAssetId.isNotBlank() && endSeconds.toDoubleOrNull() != null
                ) {
                    viewModel.adminCreateTrimJob(
                        assetId = selectedAssetId,
                        startSeconds = startSeconds.toDoubleOrNull() ?: 0.0,
                        endSeconds = endSeconds.toDoubleOrNull() ?: 0.0,
                        title = outputTitle,
                        onSuccess = ok,
                        onError = fail
                    )
                }
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
private fun AdminTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SecureMintAccent,
            unfocusedBorderColor = SecureTextGray,
            focusedLabelColor = SecureMintAccent,
            unfocusedLabelColor = SecureTextGray,
            focusedTextColor = SecureTextWhite,
            unfocusedTextColor = SecureTextWhite,
            cursorColor = SecureMintAccent
        )
    )
}

@Composable
private fun AdminActionButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = SecureMintAccent, contentColor = SecureDarkBackground)
    ) {
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminDangerButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
    ) {
        Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun UploadProgress(state: AdminUploadState) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(state.phase.ifBlank { "Working" }, color = SecureTextWhite, fontWeight = FontWeight.Bold)
            Text("${state.progress.coerceIn(0, 100)}%", color = SecureMintAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { state.progress.coerceIn(0, 100) / 100f },
            color = SecureMintAccent,
            trackColor = SecureDarkBackground,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun UsageCard(title: String, percent: Int, value: String, caption: String) {
    AdminCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Storage, contentDescription = null, tint = SecureMintAccent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    Text("$value $caption", color = SecureTextGray, style = MaterialTheme.typography.labelMedium)
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

private fun firstString(row: Map<String, Any?>, vararg keys: String): String? {
    for (key in keys) {
        val value = row[key]
        if (value is String && value.isNotBlank()) return value
        if (value != null && value !is Map<*, *> && value !is List<*>) return value.toString()
    }
    return null
}

private fun recordList(value: Any?): List<Map<String, Any?>> {
    return (value as? List<*>)?.mapNotNull { item ->
        @Suppress("UNCHECKED_CAST")
        item as? Map<String, Any?>
    } ?: emptyList()
}

private fun compactValue(value: Any?): String {
    return when (value) {
        null -> "-"
        is String -> prettyDate(value).takeIf { value.contains("T") && value.endsWith("Z") } ?: value
        is Number, is Boolean -> value.toString()
        is List<*> -> "${value.size} item${if (value.size == 1) "" else "s"}"
        is Map<*, *> -> "${value.size} field${if (value.size == 1) "" else "s"}"
        else -> value.toString()
    }
}
