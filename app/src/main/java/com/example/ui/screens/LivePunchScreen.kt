package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Worker
import com.example.data.model.WorkerAttendanceItem
import com.example.ui.components.WorkerAvatar
import com.example.ui.theme.StatusPresent
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LivePunchScreen(
    items: List<WorkerAttendanceItem>,
    onPunchClock: (Worker) -> Unit,
    modifier: Modifier = Modifier
) {
    // Live ticking clock state
    var currentTimeString by remember { mutableStateOf("") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = LocalTime.now().format(timeFormatter)
            delay(1000)
        }
    }

    var selectedWorkerId by remember { mutableStateOf<Long?>(null) }
    var punchSearchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Workers, 1: Active On Shift, 2: Completed

    val currentlyOnShift = items.filter {
        it.attendance?.checkInTime != null && it.attendance?.checkOutTime == null
    }
    val shiftCompleted = items.filter {
        it.attendance?.checkInTime != null && it.attendance?.checkOutTime != null
    }
    val notClockedIn = items.filter {
        it.attendance?.checkInTime == null
    }

    val filteredList = when (selectedTab) {
        1 -> currentlyOnShift
        2 -> shiftCompleted
        else -> items
    }.filter {
        punchSearchQuery.isBlank() ||
            it.worker.fullName.contains(punchSearchQuery, ignoreCase = true) ||
            it.worker.empCode.contains(punchSearchQuery, ignoreCase = true)
    }

    val selectedWorkerItem = items.firstOrNull { it.worker.id == selectedWorkerId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("live_punch_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Digital Clock Hero Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                tint = StatusPresent,
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "ATTENDANCE TERMINAL • LIVE KIOSK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = currentTimeString.ifEmpty { "12:00:00 PM" },
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LiveStatBadge(
                                label = "On Shift",
                                count = currentlyOnShift.size,
                                color = StatusPresent
                            )
                            LiveStatBadge(
                                label = "Completed",
                                count = shiftCompleted.size,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                            LiveStatBadge(
                                label = "Pending",
                                count = notClockedIn.size,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Punch Action Box (if worker selected)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quick Punch Desk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedWorkerItem != null) {
                        val isClockedIn = selectedWorkerItem.attendance?.checkInTime != null
                        val isCompleted = selectedWorkerItem.attendance?.checkOutTime != null

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WorkerAvatar(
                                name = selectedWorkerItem.worker.fullName,
                                colorHex = selectedWorkerItem.worker.avatarColorHex,
                                size = 50.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedWorkerItem.worker.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedWorkerItem.worker.empCode} • ${selectedWorkerItem.worker.role}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isClockedIn && !isCompleted) {
                                    Text(
                                        text = "Clocked in at ${selectedWorkerItem.attendance?.checkInTime}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StatusPresent
                                    )
                                } else if (isCompleted) {
                                    Text(
                                        text = "Shift completed (${selectedWorkerItem.attendance?.checkInTime} - ${selectedWorkerItem.attendance?.checkOutTime})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                onPunchClock(selectedWorkerItem.worker)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("punch_action_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isClockedIn) StatusPresent else MaterialTheme.colorScheme.primary
                            ),
                            enabled = !isCompleted
                        ) {
                            Icon(
                                imageVector = if (!isClockedIn) Icons.Default.Login else Icons.Default.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !isClockedIn -> "PUNCH IN NOW"
                                    !isCompleted -> "PUNCH OUT & END SHIFT"
                                    else -> "SHIFT ALREADY COMPLETED"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Tap any worker from the list below to punch in or punch out instantly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Tabs & Search Filter
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All (${items.size})", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("On Floor (${currentlyOnShift.size})", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Done (${shiftCompleted.size})", fontSize = 13.sp) }
                    )
                }

                OutlinedTextField(
                    value = punchSearchQuery,
                    onValueChange = { punchSearchQuery = it },
                    placeholder = { Text("Search worker to punch...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("kiosk_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }

        // Worker List
        items(filteredList, key = { it.worker.id }) { item ->
            val isSelected = item.worker.id == selectedWorkerId
            val isClockedIn = item.attendance?.checkInTime != null
            val isClockedOut = item.attendance?.checkOutTime != null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedWorkerId = item.worker.id }
                    .testTag("kiosk_worker_${item.worker.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkerAvatar(
                        name = item.worker.fullName,
                        colorHex = item.worker.avatarColorHex,
                        size = 42.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.worker.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.worker.empCode} • ${item.worker.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Punch Status
                    when {
                        isClockedIn && !isClockedOut -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StatusPresent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(StatusPresent)
                                    )
                                    Text(
                                        text = "In ${item.attendance?.checkInTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusPresent
                                    )
                                }
                            }
                        }
                        isClockedOut -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Completed (${item.attendance?.hoursWorked}h)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Tap to Clock In",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
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
private fun LiveStatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
