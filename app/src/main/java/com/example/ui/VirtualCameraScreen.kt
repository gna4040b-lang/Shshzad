package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PhysicalCameraDetail
import com.example.model.ResolutionOption
import com.example.model.SystemAuditResult
import com.example.ui.theme.Cyan400
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald400
import com.example.ui.theme.LiveRed
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.StandbyOrange
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualCameraScreen(
    viewModel: VirtualCameraViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.engineState.collectAsStateWithLifecycle()
    val audit by viewModel.auditResult.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onVideoPicked(uri)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isVirtualCamOn) Emerald400 else LiveRed)
                        )
                        Text(
                            text = "Virtual Camera Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Quick Status Pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isVirtualCamOn) Emerald400.copy(alpha = 0.15f) else StandbyOrange.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (state.isVirtualCamOn) Emerald400 else StandbyOrange
                        )
                    ) {
                        Text(
                            text = if (state.isVirtualCamOn) "CAM ON" else "STANDBY",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = if (state.isVirtualCamOn) Emerald400 else StandbyOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = Cyan400,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Cyan400
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Studio & Output", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Studio Tab") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Android OS Audit", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "OS Audit Tab") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Windows PC Bridge", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Computer, contentDescription = "Windows Bridge Tab") }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> StudioOutputTab(
                    viewModel = viewModel,
                    onPickVideo = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
                1 -> AndroidOsAuditTab(
                    auditResult = audit,
                    onRefresh = { viewModel.refreshAudit() }
                )
                2 -> WindowsBridgeTab(
                    state = state,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun StudioOutputTab(
    viewModel: VirtualCameraViewModel,
    onPickVideo: () -> Unit
) {
    val state by viewModel.engineState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Live frame refresh loop
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(state.targetFps, state.isPlaying, state.isVirtualCamOn) {
        val delayTime = (1000L / state.targetFps.coerceIn(1, 60))
        while (true) {
            currentBitmap = viewModel.getLatestFrame()
            delay(delayTime)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video Preview Monitor
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video_monitor_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isVirtualCamOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = if (state.isVirtualCamOn) Cyan400 else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MONITOR OUTPUT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "${state.resolution.label} • ${state.targetFps} FPS",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Cyan400
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(state.resolution.aspectRatio)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, Slate800, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = currentBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Virtual Camera Output Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text("Loading Virtual Stream...", color = Slate400, fontSize = 13.sp)
                        }

                        // Overlays
                        if (!state.isVirtualCamOn) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StandbyOrange)
                            ) {
                                Text(
                                    text = "STANDBY (CAMERA OFF)",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = StandbyOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Scrubber / Position Bar
                    if (state.selectedVideoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curSec = state.currentPositionMs / 1000
                            val durSec = state.durationMs / 1000
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", curSec / 60, curSec % 60),
                                fontSize = 11.sp,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                            Slider(
                                value = state.currentPositionMs.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toLong()) },
                                valueRange = 0f..state.durationMs.toFloat(),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .testTag("timeline_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = Cyan400,
                                    activeTrackColor = Cyan400,
                                    inactiveTrackColor = Slate700
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", durSec / 60, durSec % 60),
                                fontSize = 11.sp,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Master Stream Controls Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VIRTUAL CAMERA CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Row: Master ON/OFF Switch and Video Picker Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (state.isVirtualCamOn) Emerald400 else Slate400,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Virtual Camera Power",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (state.isVirtualCamOn) "Active video feed broadcast" else "Muted (Sends standby pattern)",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }
                        Switch(
                            checked = state.isVirtualCamOn,
                            onCheckedChange = { viewModel.toggleCameraPower() },
                            modifier = Modifier.testTag("power_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Emerald400,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Video Source Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickVideo,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_video_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan600),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Video File", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        if (state.selectedVideoUri != null) {
                            OutlinedButton(
                                onClick = { viewModel.clearVideo() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f))
                            ) {
                                Text("Reset Feed", fontSize = 12.sp)
                            }
                        }
                    }

                    if (state.selectedVideoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Source: ${state.videoName ?: "Selected Video"}",
                            fontSize = 12.sp,
                            color = Cyan400,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Source: High-Precision SMPTE Calibration Generator (Default)",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback Controls Row: Play/Pause, Loop Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.togglePlay() },
                                modifier = Modifier.testTag("play_pause_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state.isPlaying) Slate700 else Emerald400
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (state.isPlaying) "Pause" else "Play", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleLoop() },
                                modifier = Modifier.testTag("loop_toggle_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (state.isLooping) Cyan400 else Slate400
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (state.isLooping) Cyan400 else Slate700
                                )
                            ) {
                                Icon(Icons.Default.Loop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (state.isLooping) "Loop: ON" else "Loop: OFF", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resolution Chips
                    Text(text = "Resolution Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResolutionOption.entries.forEach { option ->
                            val isSelected = state.resolution == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setResolution(option) },
                                label = { Text(option.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Cyan600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate800,
                                    labelColor = Slate400
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // FPS Chips
                    Text(text = "Target Frame Rate (FPS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate400)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 24, 30, 60).forEach { fps ->
                            val isSelected = state.targetFps == fps
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFps(fps) },
                                label = { Text("$fps FPS", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald400,
                                    selectedLabelColor = Slate950,
                                    containerColor = Slate800,
                                    labelColor = Slate400
                                )
                            )
                        }
                    }
                }
            }
        }

        // Network Broadcast / IP Camera Server Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (state.isServerBroadcasting) Emerald400.copy(alpha = 0.6f) else Slate700
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isServerBroadcasting) Icons.Default.CastConnected else Icons.Default.Cast,
                                contentDescription = null,
                                tint = if (state.isServerBroadcasting) Emerald400 else Slate400
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Local Stream Server (MJPEG / IP Cam)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (state.isServerBroadcasting) "Streaming to PC, OBS & Browsers" else "Offline (Click Start to Broadcast)",
                                    fontSize = 11.sp,
                                    color = Slate400
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleBroadcastServer() },
                            modifier = Modifier.testTag("broadcast_server_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.isServerBroadcasting) LiveRed else Emerald400
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (state.isServerBroadcasting) "Stop Server" else "Start Server",
                                color = if (state.isServerBroadcasting) Color.White else Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.isServerBroadcasting) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Slate950,
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "NETWORK STREAM URL:", fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = state.serverUrl,
                                            fontSize = 13.sp,
                                            color = Cyan400,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Stream URL", state.serverUrl))
                                            Toast.makeText(context, "Stream URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL", tint = Cyan400)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Connected Clients: ${state.connectedClients}",
                                    fontSize = 12.sp,
                                    color = Emerald400,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Port: 8080",
                                    fontSize = 12.sp,
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidOsAuditTab(
    auditResult: SystemAuditResult?,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Limitation Statement Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, StandbyOrange)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StandbyOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Official Android OS Limitation Statement",
                            fontWeight = FontWeight.Bold,
                            color = StandbyOrange,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = auditResult?.limitationSummary ?: "Verifying system support...",
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Technical Architecture Analysis
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WHY STOCK ANDROID RESTRICTS SYSTEM VIRTUAL CAMERAS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    auditResult?.technicalDetails?.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "• ", color = Cyan400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = detail, color = Slate400, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // Legitimate Alternatives Working in this App
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Legitimate Working Alternatives Provided",
                            fontWeight = FontWeight.Bold,
                            color = Emerald400,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    auditResult?.legitimateAlternatives?.forEach { alt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "✓ ", color = Emerald400, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = alt, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // Live Hardware Inspection Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETECTED HARDWARE CAMERAS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Audit", tint = Cyan400)
                        }
                    }

                    val cameras = auditResult?.physicalCameras.orEmpty()
                    if (cameras.isEmpty()) {
                        Text(
                            text = "No physical camera sensors returned by CameraManager (or emulator sandbox without camera emulation).",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    } else {
                        cameras.forEach { cam ->
                            CameraItemCard(cam)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraItemCard(cam: PhysicalCameraDetail) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate950,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Camera ID: ${cam.id} (${cam.facing})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Cyan400
                )
                Text(
                    text = cam.maxResolution,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Slate400
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "HAL Support Level: ${cam.hardwareLevel}",
                fontSize = 11.sp,
                color = Slate400
            )
        }
    }
}

@Composable
private fun WindowsBridgeTab(
    state: com.example.engine.VirtualCameraState,
    context: Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Cyan400.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Computer, contentDescription = null, tint = Cyan400, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Windows Virtual Camera (Media Foundation)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The complete real Windows Virtual Camera solution is included in this repository under '/windows-virtual-cam/'. It implements a genuine Media Foundation Virtual Camera driver (MFCreateVirtualCamera) and Desktop Controller.",
                        fontSize = 13.sp,
                        color = Slate400,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Windows Features Checklist
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WINDOWS VIRTUAL CAMERA SPECIFICATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val features = listOf(
                        "Legitimate Media Foundation API: Built using MFCreateVirtualCamera and IMFMediaSource (SoftwareCameraSource).",
                        "Prerecorded Video Selection: Reads MP4/AVI/MKV video files via Media Foundation Source Reader.",
                        "Live WebCam Output: Appears as 'Virtual Camera MF Video Source' in Windows Settings, Teams, OBS, Zoom, and browsers.",
                        "Full Playback Controls: Play, Pause, Loop, Start/Stop driver, and ON/OFF Standby toggle.",
                        "Configurable Output: Supports 1080p, 720p, 480p at 15, 24, 30, 60 FPS in NV12 / RGB32 formats.",
                        "Dual Video Mode: Ingests local video files OR connects to this Android device's live stream URL!"
                    )

                    features.forEach { feat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "• ", color = Cyan400, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = feat, color = Color.White, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        // Build & Quick Start Instructions for Windows
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HOW TO BUILD & RUN ON WINDOWS PC",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val steps = listOf(
                        "1. Open folder 'windows-virtual-cam' on your Windows 10/11 PC.",
                        "2. Prerequisites: Visual Studio 2022 (with Desktop C++ & Windows SDK 10.0.22000+) and CMake.",
                        "3. Run build script: '.\\build_windows.bat' or '.\\build_windows.ps1'.",
                        "4. Run 'VirtualCameraController.exe' (as Administrator for initial driver registration).",
                        "5. Click 'Select Video', set Resolution/FPS, and click 'Start Virtual Camera'.",
                        "6. Open Windows Camera app, OBS Studio, or Microsoft Teams — select 'Virtual Camera MF Video Source'!"
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate800)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "BRIDGE ANDROID TO WINDOWS PC (OVER USB/ADB):", fontSize = 10.sp, color = Cyan400, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "1. Connect phone to PC with USB cable\n2. Run: adb forward tcp:8080 tcp:8080\n3. In Windows Virtual Camera Controller, choose Android Stream and enter http://localhost:8080/videostream",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
