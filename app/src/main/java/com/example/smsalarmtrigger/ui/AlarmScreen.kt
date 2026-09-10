package com.example.smsalarmtrigger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsalarmtrigger.R
import com.example.smsalarmtrigger.data.AlarmSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    settings: AlarmSettings,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onKeywordChanged: (String) -> Unit,
    onSenderFilterChanged: (String) -> Unit,
    onTestAlarm: (Int) -> Unit,
    onStopAlarm: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestDisableBatteryOptimization: () -> Unit,
    onToggleHideIcon: (Boolean) -> Unit
) {
    var keywordInput by remember(settings.triggerKeyword) { mutableStateOf(settings.triggerKeyword) }
    var senderInput by remember(settings.senderFilter) { mutableStateOf(settings.senderFilter) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    if (settings.isAlarmActive) Color(0xFFDC2626)
                                    else if (settings.isEnabled) Color(0xFF16A34A)
                                    else Color(0xFF9CA3AF),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. ACTIVE ALARM BANNER (Visible when ringing)
            AnimatedVisibility(visible = settings.isAlarmActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(pulseScale),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFBA1A1A))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.active_alarm_warning),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Loud alarm is looping on STREAM_ALARM with maximum volume & vibration.",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = onStopAlarm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFBA1A1A)
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.stop_alarm_button),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. MAIN ACTIVATION TOGGLE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isEnabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (settings.isEnabled) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (settings.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (settings.isEnabled) stringResource(R.string.service_status_enabled)
                                else stringResource(R.string.service_status_disabled),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = if (settings.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (settings.isEnabled) "Actively scanning incoming SMS in background"
                                else "Background SMS monitoring paused",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Switch(
                        checked = settings.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // 3. TRIGGER CONFIGURATION CARD
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Trigger Configuration",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    // Keyword field
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = {
                            keywordInput = it
                            onKeywordChanged(it)
                        },
                        label = { Text(stringResource(R.string.trigger_keyword_label)) },
                        placeholder = { Text(stringResource(R.string.trigger_keyword_placeholder)) },
                        supportingText = { Text(stringResource(R.string.trigger_keyword_helper)) },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sender filter field
                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = {
                            senderInput = it
                            onSenderFilterChanged(it)
                        },
                        label = { Text(stringResource(R.string.sender_filter_label)) },
                        placeholder = { Text(stringResource(R.string.sender_filter_placeholder)) },
                        supportingText = { Text(stringResource(R.string.sender_filter_helper)) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3b. APP VISIBILITY CARD
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.hide_icon_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.hide_icon_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = settings.isIconHidden,
                            onCheckedChange = onToggleHideIcon
                        )
                    }
                    if (settings.isIconHidden) {
                        Text(
                            text = stringResource(R.string.secret_code_hint),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = stringResource(R.string.hide_icon_disclaimer),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // 4. TEST ALARM ACTION BUTTON
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Immediate Test",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Verify that STREAM_ALARM max volume, speaker playback, and vibration function properly on your hardware without sending an actual SMS.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )
                    Text(
                        text = "Test a specific numbered tone (e.g. what EMERGENCY2 would play):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3).forEach { index ->
                            FilledTonalButton(
                                onClick = { onTestAlarm(index) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Audio $index", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onTestAlarm(-1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.test_alarm_button), fontWeight = FontWeight.SemiBold)
                        }

                        if (settings.isAlarmActive) {
                            FilledTonalButton(
                                onClick = onStopAlarm,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.stop_alarm_button))
                            }
                        }
                    }
                }
            }

            // 5. PERMISSIONS & BATTERY OPTIMIZATION GUIDE CARD
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.permissions_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }

                    Text(
                        text = "Android 8 through Android 15 impose strict background execution limits. To guarantee the SMS BroadcastReceiver fires immediately even when your phone is in deep sleep (Doze mode):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // SMS Permission Item
                    PermissionStatusRow(
                        title = stringResource(R.string.sms_permission_title),
                        description = stringResource(R.string.sms_permission_desc),
                        icon = Icons.Default.Sms,
                        isGranted = hasSmsPermission,
                        buttonText = stringResource(R.string.grant_permission),
                        onAction = onRequestSmsPermission
                    )

                    // Notification Permission Item
                    PermissionStatusRow(
                        title = stringResource(R.string.notification_permission_title),
                        description = stringResource(R.string.notification_permission_desc),
                        icon = Icons.Default.NotificationsActive,
                        isGranted = hasNotificationPermission,
                        buttonText = stringResource(R.string.grant_permission),
                        onAction = onRequestNotificationPermission
                    )

                    // Battery Optimization Item
                    PermissionStatusRow(
                        title = stringResource(R.string.battery_opt_title),
                        description = stringResource(R.string.battery_opt_desc),
                        icon = Icons.Default.BatteryAlert,
                        isGranted = isBatteryOptimizationIgnored,
                        buttonText = stringResource(R.string.disable_battery_opt),
                        onAction = onRequestDisableBatteryOptimization
                    )
                }
            }

            // 6. LAST TRIGGER HISTORY / AUDIT CARD
            if (settings.lastTriggeredTime > 0L) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Last Trigger Audit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                            .format(Date(settings.lastTriggeredTime))
                        Text(text = "Triggered At: $formattedDate", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (settings.lastTriggeredSender.isNotBlank()) {
                            // Sender/phone number intentionally not shown on this screen.
                        }
                        if (settings.lastTriggeredBody.isNotBlank()) {
                            Text(
                                text = "Message: \"${settings.lastTriggeredBody.take(120)}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isGranted) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                RoundedCornerShape(12.dp)
            )
            .background(
                if (isGranted) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) Color(0xFF16A34A) else Color(0xFFDC2626),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (isGranted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.DarkGray,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (!isGranted) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = "Active",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
