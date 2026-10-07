package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Shift
import com.example.data.model.Worker

private val COLOR_CHOICES = listOf(
    "#1E3A8A", "#0284C7", "#0F766E", "#059669",
    "#D97706", "#DC2626", "#7C3AED", "#475569"
)

private val DEPARTMENT_CHOICES = listOf(
    "Operations", "Production", "Logistics", "Maintenance", "QA & Compliance", "Administration"
)

private val DEFAULT_SHIFT_CHOICES = listOf(
    "Morning (07:00 - 15:30)",
    "General (08:30 - 17:00)",
    "Evening (15:00 - 23:30)",
    "Night (23:00 - 07:30)"
)

@Composable
fun AddWorkerDialog(
    workerToEdit: Worker? = null,
    availableShifts: List<Shift> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        empCode: String,
        fullName: String,
        role: String,
        department: String,
        phone: String,
        shiftName: String,
        hourlyRate: Double,
        avatarColorHex: String
    ) -> Unit
) {
    val shiftOptions = remember(availableShifts) {
        if (availableShifts.isNotEmpty()) {
            availableShifts.filter { it.status == "ACTIVE" }.map { "${it.name} (${it.startTime} - ${it.endTime})" }
        } else {
            DEFAULT_SHIFT_CHOICES
        }
    }

    var fullName by remember { mutableStateOf(workerToEdit?.fullName ?: "") }
    var empCode by remember { mutableStateOf(workerToEdit?.empCode ?: "") }
    var role by remember { mutableStateOf(workerToEdit?.role ?: "") }
    var department by remember { mutableStateOf(workerToEdit?.department ?: "Operations") }
    var phone by remember { mutableStateOf(workerToEdit?.phone ?: "") }
    var shiftName by remember { mutableStateOf(workerToEdit?.shiftName ?: shiftOptions.firstOrNull() ?: "Morning (07:00 - 15:30)") }
    var hourlyRateText by remember { mutableStateOf(workerToEdit?.hourlyRate?.toString() ?: "20.00") }
    var selectedColor by remember { mutableStateOf(workerToEdit?.avatarColorHex ?: COLOR_CHOICES.first()) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (workerToEdit != null) "Edit Worker Profile" else "Add New Worker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Michael Jordan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_name_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = empCode,
                        onValueChange = { empCode = it },
                        label = { Text("Worker ID *") },
                        placeholder = { Text("EMP-109") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("worker_id_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        placeholder = { Text("+1 555-0192") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("worker_phone_input"),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Job Title *") },
                    placeholder = { Text("e.g. Production Specialist") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_role_input"),
                    singleLine = true
                )

                Text(
                    text = "Department",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEPARTMENT_CHOICES.take(3).forEach { dept ->
                        FilterChip(
                            selected = department == dept,
                            onClick = { department = dept },
                            label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DEPARTMENT_CHOICES.drop(3).forEach { dept ->
                        FilterChip(
                            selected = department == dept,
                            onClick = { department = dept },
                            label = { Text(dept, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(
                    text = "Shift Schedule",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                shiftOptions.forEach { shift ->
                    FilterChip(
                        selected = shiftName == shift,
                        onClick = { shiftName = shift },
                        label = { Text(shift, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = hourlyRateText,
                    onValueChange = { hourlyRateText = it },
                    label = { Text("Hourly Wage Rate ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_wage_input"),
                    singleLine = true
                )

                Text(
                    text = "Badge Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    COLOR_CHOICES.forEach { hex ->
                        val isSelected = hex == selectedColor
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Blue }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                if (showError) {
                    Text(
                        text = "Please enter name, worker ID, and role.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank() || empCode.isBlank() || role.isBlank()) {
                        showError = true
                    } else {
                        val rate = hourlyRateText.toDoubleOrNull() ?: 20.0
                        onSave(
                            empCode,
                            fullName,
                            role,
                            department,
                            phone,
                            shiftName,
                            rate,
                            selectedColor
                        )
                    }
                },
                modifier = Modifier.testTag("save_worker_btn")
            ) {
                Text(if (workerToEdit != null) "Update" else "Add Worker")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_worker_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
