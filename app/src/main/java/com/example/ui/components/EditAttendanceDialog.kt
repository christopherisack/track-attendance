package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.WorkerAttendanceItem

@Composable
fun EditAttendanceDialog(
    item: WorkerAttendanceItem,
    onDismiss: () -> Unit,
    onSave: (
        status: AttendanceStatus,
        checkIn: String?,
        checkOut: String?,
        hoursWorked: Double,
        overtime: Double,
        notes: String?
    ) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(item.currentStatus) }
    var checkInTime by remember { mutableStateOf(item.attendance?.checkInTime ?: "08:00 AM") }
    var checkOutTime by remember { mutableStateOf(item.attendance?.checkOutTime ?: "04:30 PM") }
    var hoursWorkedText by remember { mutableStateOf(item.attendance?.hoursWorked?.toString() ?: "8.0") }
    var overtimeText by remember { mutableStateOf(item.attendance?.overtimeHours?.toString() ?: "0.0") }
    var notesText by remember { mutableStateOf(item.attendance?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Edit Attendance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.worker.fullName} • ${item.worker.empCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Attendance Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                StatusSelectorRow(
                    selectedStatus = selectedStatus,
                    onStatusSelected = { selectedStatus = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = checkInTime,
                        onValueChange = { checkInTime = it },
                        label = { Text("Check-In") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_attendance_checkin"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = checkOutTime,
                        onValueChange = { checkOutTime = it },
                        label = { Text("Check-Out") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_attendance_checkout"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hoursWorkedText,
                        onValueChange = { hoursWorkedText = it },
                        label = { Text("Regular Hrs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_attendance_hours"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = overtimeText,
                        onValueChange = { overtimeText = it },
                        label = { Text("Overtime Hrs") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_attendance_ot"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Remarks (Optional)") },
                    placeholder = { Text("e.g. Approved early leave, shift coverage") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_attendance_notes"),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursWorkedText.toDoubleOrNull() ?: 8.0
                    val ot = overtimeText.toDoubleOrNull() ?: 0.0
                    onSave(
                        selectedStatus,
                        checkInTime.ifBlank { null },
                        checkOutTime.ifBlank { null },
                        hours,
                        ot,
                        notesText.ifBlank { null }
                    )
                },
                modifier = Modifier.testTag("edit_attendance_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("edit_attendance_cancel_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
