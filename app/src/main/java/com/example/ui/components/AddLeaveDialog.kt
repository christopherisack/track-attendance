package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Worker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val LEAVE_TYPES = listOf("Paid Leave", "Sick Leave", "Casual Leave", "Emergency")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLeaveDialog(
    workers: List<Worker>,
    initialWorkerId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (
        workerId: Long,
        leaveType: String,
        startDate: String,
        endDate: String,
        reason: String
    ) -> Unit
) {
    var selectedWorkerId by remember {
        mutableStateOf(initialWorkerId ?: workers.firstOrNull()?.id ?: 0L)
    }
    var workerDropdownExpanded by remember { mutableStateOf(false) }
    var selectedLeaveType by remember { mutableStateOf(LEAVE_TYPES.first()) }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
    var startDate by remember { mutableStateOf(todayStr) }
    var endDate by remember { mutableStateOf(todayStr) }
    var reason by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val currentWorker = workers.firstOrNull { it.id == selectedWorkerId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Worker Leave",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Worker selector
                ExposedDropdownMenuBox(
                    expanded = workerDropdownExpanded,
                    onExpandedChange = { workerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentWorker?.let { "${it.fullName} (${it.empCode})" } ?: "Select Worker",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Worker *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = workerDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("leave_worker_dropdown")
                    )

                    ExposedDropdownMenu(
                        expanded = workerDropdownExpanded,
                        onDismissRequest = { workerDropdownExpanded = false }
                    ) {
                        workers.forEach { worker ->
                            DropdownMenuItem(
                                text = { Text("${worker.fullName} • ${worker.department}") },
                                onClick = {
                                    selectedWorkerId = worker.id
                                    workerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "Leave Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LEAVE_TYPES.forEach { type ->
                        FilterChip(
                            selected = selectedLeaveType == type,
                            onClick = { selectedLeaveType = type },
                            label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("From Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leave_start_date"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("To Date") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("leave_end_date"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Absence *") },
                    placeholder = { Text("e.g. Scheduled medical appointment") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leave_reason_input"),
                    minLines = 2,
                    maxLines = 3
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedWorkerId <= 0L) {
                        errorMsg = "Please select a worker."
                    } else if (reason.isBlank()) {
                        errorMsg = "Please enter a reason."
                    } else {
                        onSave(selectedWorkerId, selectedLeaveType, startDate, endDate, reason)
                    }
                },
                modifier = Modifier.testTag("save_leave_btn")
            ) {
                Text("Approve & Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_leave_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
