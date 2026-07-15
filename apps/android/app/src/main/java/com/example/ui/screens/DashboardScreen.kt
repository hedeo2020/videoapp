package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.api.MessageDto
import com.example.data.api.NotificationDto
import com.example.ui.theme.*
import com.example.ui.viewmodel.SecureStreamViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SecureStreamViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOnline by viewModel.isOnline.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.dashboardLoading.collectAsState()
    val errorMsg by viewModel.dashboardError.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    // Load initial data on mount and poll GET /account/dashboard every 5 seconds
    LaunchedEffect(isOnline) {
        if (isOnline) {
            viewModel.loadDashboardSummary()
            viewModel.loadNotifications()
            while (true) {
                kotlinx.coroutines.delay(5000)
                viewModel.loadDashboardSummary()
            }
        }
    }

    // Poll GET /messages every 2500ms while the chat tab (selectedTabIndex == 1) is visible
    LaunchedEffect(selectedTabIndex, isOnline) {
        if (isOnline) {
            if (selectedTabIndex == 1) {
                viewModel.loadMessages()
                while (true) {
                    kotlinx.coroutines.delay(2500)
                    viewModel.loadMessages()
                }
            } else {
                viewModel.loadMessages()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecureTextWhite
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SecureMintAccent
                        )
                    }
                },
                actions = {
                    if (isOnline) {
                        IconButton(
                            onClick = {
                                viewModel.loadDashboardSummary()
                                viewModel.loadMessages()
                                viewModel.loadNotifications()
                                Toast.makeText(context, "Dashboard Refreshed", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = SecureMintAccent
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SecureDarkBackground,
                    titleContentColor = SecureTextWhite
                )
            )
        },
        containerColor = SecureDarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isOnline) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OFFLINE MODE - Dashboard unavailable",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Tabs Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SecureDarkSurface,
                contentColor = SecureMintAccent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = SecureMintAccent
                    )
                }
            ) {
                // Tab 1: Account
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Account") },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Account"
                        )
                    },
                    selectedContentColor = SecureMintAccent,
                    unselectedContentColor = SecureTextGray
                )

                // Tab 2: Messages (Chat)
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        selectedTabIndex = 1
                        viewModel.loadMessages()
                    },
                    text = { Text("Messages") },
                    icon = {
                        BadgedBox(
                            badge = {
                                val unreadCount = summary?.unreadMessages ?: 0
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = SecureMintAccent,
                                        contentColor = SecureDarkBackground
                                    ) {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = "Messages"
                            )
                        }
                    },
                    selectedContentColor = SecureMintAccent,
                    unselectedContentColor = SecureTextGray
                )

                // Tab 3: Notifications
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {
                        selectedTabIndex = 2
                        viewModel.loadNotifications()
                    },
                    text = { Text("Alerts") },
                    icon = {
                        BadgedBox(
                            badge = {
                                val unreadCount = summary?.unreadNotifications ?: 0
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = SecureMintAccent,
                                        contentColor = SecureDarkBackground
                                    ) {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    },
                    selectedContentColor = SecureMintAccent,
                    unselectedContentColor = SecureTextGray
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(SecureDarkBackground)
            ) {
                if (isOnline) {
                    when (selectedTabIndex) {
                        0 -> AccountTab(viewModel = viewModel, isLoading = isLoading, errorMsg = errorMsg)
                        1 -> MessagesTab(
                            viewModel = viewModel,
                            messages = messages,
                            currentUserId = summary?.user?.id
                        )
                        2 -> NotificationsTab(viewModel = viewModel, notifications = notifications)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Offline",
                            tint = SecureTextGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Dashboard Offline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecureTextWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please reconnect to the internet to view your account, messages, and notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureTextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountTab(
    viewModel: SecureStreamViewModel,
    isLoading: Boolean,
    errorMsg: String?
) {
    val context = LocalContext.current
    val currentEmail = viewModel.authState.collectAsState().value.let {
        if (it is com.example.ui.viewmodel.AuthState.Success) it.email else ""
    }
    val currentDisplayName = viewModel.authState.collectAsState().value.let {
        if (it is com.example.ui.viewmodel.AuthState.Success) it.displayName else ""
    }

    var displayNameInput by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Details Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SecureDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Current Account Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecureMintAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Display Name:", color = SecureTextGray, style = MaterialTheme.typography.bodyMedium)
                        Text(currentDisplayName, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Email Address:", color = SecureTextGray, style = MaterialTheme.typography.bodyMedium)
                        Text(currentEmail, color = SecureTextWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Edit Profile Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SecureDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Update Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecureMintAccent
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Display Name input
                    OutlinedTextField(
                        value = displayNameInput,
                        onValueChange = { displayNameInput = it },
                        label = { Text("Display Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecureMintAccent,
                            unfocusedBorderColor = SecureDarkGray,
                            focusedLabelColor = SecureMintAccent,
                            unfocusedLabelColor = SecureTextGray,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_display_name_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password input
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("New Password (Optional)") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecureMintAccent,
                            unfocusedBorderColor = SecureDarkGray,
                            focusedLabelColor = SecureMintAccent,
                            unfocusedLabelColor = SecureTextGray,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_password_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password input
                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecureMintAccent,
                            unfocusedBorderColor = SecureDarkGray,
                            focusedLabelColor = SecureMintAccent,
                            unfocusedLabelColor = SecureTextGray,
                            focusedTextColor = SecureTextWhite,
                            unfocusedTextColor = SecureTextWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_confirm_password_input")
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (!errorMsg.isNullOrBlank()) {
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (displayNameInput.isBlank()) {
                                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (passwordInput.isNotEmpty() && passwordInput != confirmPasswordInput) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.updateAccount(
                                displayName = displayNameInput,
                                password = passwordInput.ifEmpty { null },
                                onSuccess = {
                                    passwordInput = ""
                                    confirmPasswordInput = ""
                                    Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_LONG).show()
                                },
                                onError = { msg ->
                                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecureMintAccent,
                            contentColor = SecureDarkBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_profile_button"),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SecureDarkBackground)
                        } else {
                            Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessagesTab(
    viewModel: SecureStreamViewModel,
    messages: List<MessageDto>,
    currentUserId: String?
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val effectiveUserId = currentUserId ?: viewModel.getUserId()

    // Scroll to bottom when messages load/change
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Support Chat with Admin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SecureMintAccent
            )
            IconButton(
                onClick = { viewModel.loadMessages() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh messages",
                    tint = SecureMintAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Messages scrolling view
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet. Send a message to start chatting with an Admin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureTextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(messages) { msg ->
                    // Determine sender alignment
                    val isMyMsg = msg.sender?.id == effectiveUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMyMsg) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMyMsg) 16.dp else 2.dp,
                                        bottomEnd = if (isMyMsg) 2.dp else 16.dp
                                    )
                                )
                                .background(if (isMyMsg) SecureMintAccent else SecureDarkSurface)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.body,
                                    color = if (isMyMsg) SecureDarkBackground else SecureTextWhite,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!msg.createdAt.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatTime(msg.createdAt),
                                        color = if (isMyMsg) SecureDarkBackground.copy(alpha = 0.6f) else SecureTextGray,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sending Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecureMintAccent,
                    unfocusedBorderColor = SecureDarkGray,
                    focusedTextColor = SecureTextWhite,
                    unfocusedTextColor = SecureTextWhite,
                    focusedPlaceholderColor = SecureTextGray,
                    unfocusedPlaceholderColor = SecureTextGray
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText.trim())
                        messageText = ""
                        keyboardController?.hide()
                    }
                },
                containerColor = SecureMintAccent,
                contentColor = SecureDarkBackground,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_message_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationsTab(
    viewModel: SecureStreamViewModel,
    notifications: List<NotificationDto>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SecureMintAccent
            )
            if (notifications.any { it.readAt == null }) {
                TextButton(
                    onClick = { viewModel.markAllNotificationsAsRead() },
                    colors = ButtonDefaults.textButtonColors(contentColor = SecureMintAccent),
                    modifier = Modifier.testTag("mark_all_read_button")
                ) {
                    Text("Mark all as read", fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You have no notifications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecureTextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(notifications) { item ->
                    val isUnread = item.readAt == null
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isUnread) {
                                    viewModel.markNotificationAsRead(item.id)
                                }
                            }
                            .testTag("notification_item_${item.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isUnread) SecureDarkSurface else SecureDarkSurface.copy(alpha = 0.85f)
                        ),
                        border = if (isUnread) androidx.compose.foundation.BorderStroke(1.dp, SecureMintAccent.copy(alpha = 0.5f)) else null,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Status Dot
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp, end = 12.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (!isUnread) Color.Transparent else SecureMintAccent)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (!isUnread) FontWeight.Normal else FontWeight.Bold,
                                    color = SecureTextWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!isUnread) SecureTextGray else SecureTextWhite.copy(alpha = 0.9f)
                                )
                                if (!item.createdAt.isBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatTime(item.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SecureTextGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Format notification and chat times using Asia/Manila timezone
private fun formatTime(isoString: String): String {
    if (isoString.isBlank()) return ""
    return try {
        val instant = java.time.Instant.parse(isoString)
        val manilaZone = java.time.ZoneId.of("Asia/Manila")
        val zonedDateTime = instant.atZone(manilaZone)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        try {
            val manilaZone = java.time.ZoneId.of("Asia/Manila")
            val zonedDateTime = java.time.ZonedDateTime.parse(isoString).withZoneSameInstant(manilaZone)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            zonedDateTime.format(formatter)
        } catch (ex: Exception) {
            // Slicing fallback as last resort
            if (isoString.contains("T")) {
                val parts = isoString.split("T")
                val date = parts[0]
                val time = parts[1].take(5)
                "$date $time"
            } else {
                isoString
            }
        }
    }
}
