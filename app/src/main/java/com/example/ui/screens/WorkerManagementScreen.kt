package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shift
import com.example.data.model.Worker
import com.example.ui.components.AddWorkerDialog
import com.example.ui.components.ShiftManagementDialog
import com.example.ui.components.WorkerAvatar

@Composable
fun WorkerManagementScreen(
    workers: List<Worker>,
    shifts: List<Shift> = emptyList(),
    onAddWorker: (
        empCode: String,
        fullName: String,
        role: String,
        department: String,
        phone: String,
        shiftName: String,
        hourlyRate: Double,
        avatarColorHex: String
    ) -> Unit,
    onUpdateWorker: (Worker) -> Unit,
    onDeleteWorker: (Worker) -> Unit,
    onAddShift: (
        name: String,
        code: String,
        startTime: String,
        endTime: String,
        status: String,
        breakMinutes: Int,
        colorHex: String,
        description: String?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateShift: (Shift) -> Unit = {},
    onUpdateShiftStatus: (shiftId: Long, status: String) -> Unit = { _, _ -> },
    onDeleteShift: (Shift) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showShiftsDialog by remember { mutableStateOf(false) }
    var workerToEdit by remember { mutableStateOf<Worker?>(null) }
    var workerToDelete by remember { mutableStateOf<Worker?>(null) }

    val departments = listOf("All", "Operations", "Production", "Logistics", "Maintenance", "QA & Compliance")

    val filteredWorkers = workers.filter { worker ->
        val matchesSearch = searchQuery.isBlank() ||
            worker.fullName.contains(searchQuery, ignoreCase = true) ||
            worker.empCode.contains(searchQuery, ignoreCase = true) ||
            worker.role.contains(searchQuery, ignoreCase = true)
        val matchesDept = selectedDept == "All" || worker.department.equals(selectedDept, ignoreCase = true)
        matchesSearch && matchesDept
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("worker_directory_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header stats summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = workers.size.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Total Staff",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = workers.count { it.isActive }.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Active Staff",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = workers.map { it.department }.distinct().size.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Departments",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showShiftsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("btn_manage_shifts_dialog"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Shifts (${shifts.count { it.status == "ACTIVE" }} Active)")
                    }
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, role, or ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Department filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    departments.forEach { dept ->
                        FilterChip(
                            selected = selectedDept == dept,
                            onClick = { selectedDept = dept },
                            label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Workers List
            items(filteredWorkers, key = { it.id }) { worker ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_item_${worker.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WorkerAvatar(
                                name = worker.fullName,
                                colorHex = worker.avatarColorHex,
                                size = 48.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = worker.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = worker.empCode,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = "${worker.role} • ${worker.department}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { workerToEdit = worker },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Worker",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { workerToDelete = worker },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Worker",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Details row: Shift, Wage, Phone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = worker.shiftName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$${String.format("%.2f", worker.hourlyRate)}/hr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (worker.phone.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = worker.phone,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing for FAB
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_add_worker"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Worker")
        }
    }

    if (showAddDialog) {
        AddWorkerDialog(
            availableShifts = shifts,
            onDismiss = { showAddDialog = false },
            onSave = { empCode, fullName, role, department, phone, shiftName, hourlyRate, avatarColorHex ->
                onAddWorker(empCode, fullName, role, department, phone, shiftName, hourlyRate, avatarColorHex)
                showAddDialog = false
            }
        )
    }

    workerToEdit?.let { worker ->
        AddWorkerDialog(
            workerToEdit = worker,
            availableShifts = shifts,
            onDismiss = { workerToEdit = null },
            onSave = { empCode, fullName, role, department, phone, shiftName, hourlyRate, avatarColorHex ->
                onUpdateWorker(
                    worker.copy(
                        empCode = empCode,
                        fullName = fullName,
                        role = role,
                        department = department,
                        phone = phone,
                        shiftName = shiftName,
                        hourlyRate = hourlyRate,
                        avatarColorHex = avatarColorHex
                    )
                )
                workerToEdit = null
            }
        )
    }

    if (showShiftsDialog) {
        ShiftManagementDialog(
            shifts = shifts,
            onDismiss = { showShiftsDialog = false },
            onAddShift = onAddShift,
            onUpdateShift = onUpdateShift,
            onUpdateStatus = onUpdateShiftStatus,
            onDeleteShift = onDeleteShift
        )
    }

    workerToDelete?.let { worker ->
        AlertDialog(
            onDismissRequest = { workerToDelete = null },
            title = { Text("Delete Worker") },
            text = {
                Text("Are you sure you want to remove ${worker.fullName} (${worker.empCode})? All past attendance and leave history will also be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteWorker(worker)
                        workerToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { workerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
