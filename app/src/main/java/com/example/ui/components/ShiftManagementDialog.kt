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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import com.example.data.model.Shift
import com.example.data.model.ShiftStatus

private val SHIFT_COLORS = listOf(
    "#1E40AF", "#0F766E", "#D97706", "#7C3AED", "#DC2626", "#0284C7"
)

@Composable
fun ShiftManagementDialog(
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onAddShift: (
        name: String,
        code: String,
        startTime: String,
        endTime: String,
        status: String,
        breakMinutes: Int,
        colorHex: String,
        description: String?
    ) -> Unit,
    onUpdateShift: (Shift) -> Unit,
    onUpdateStatus: (shiftId: Long, status: String) -> Unit,
    onDeleteShift: (Shift) -> Unit
) {
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var shiftToEdit by remember { mutableStateOf<Shift?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shift Management",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage schedules, timings & active statuses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        shiftToEdit = null
                        showCreateEditDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_create_new_shift"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Shift")
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shifts_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shifts, key = { it.id }) { shift ->
                        ShiftCardItem(
                            shift = shift,
                            onEdit = {
                                shiftToEdit = shift
                                showCreateEditDialog = true
                            },
                            onDelete = { onDeleteShift(shift) },
                            onToggleStatus = { newStatus ->
                                onUpdateStatus(shift.id, newStatus)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )

    if (showCreateEditDialog) {
        CreateOrEditShiftDialog(
            shiftToEdit = shiftToEdit,
            onDismiss = { showCreateEditDialog = false },
            onSave = { name, code, start, end, status, breakMin, color, desc ->
                if (shiftToEdit != null) {
                    onUpdateShift(
                        shiftToEdit!!.copy(
                            name = name,
                            code = code,
                            startTime = start,
                            endTime = end,
                            status = status,
                            breakMinutes = breakMin,
                            colorHex = color,
                            description = desc
                        )
                    )
                } else {
                    onAddShift(name, code, start, end, status, breakMin, color, desc)
                }
                showCreateEditDialog = false
            }
        )
    }
}

@Composable
private fun ShiftCardItem(
    shift: Shift,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (String) -> Unit
) {
    val shiftColor = try {
        Color(android.graphics.Color.parseColor(shift.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shift_card_${shift.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(shiftColor)
                    )
                    Text(
                        text = shift.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = shift.code,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Shift", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Shift", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Timings & Break Duration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "${shift.startTime} - ${shift.endTime}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• ${shift.breakMinutes}m break",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge & Clickable toggle
                ShiftStatusPill(
                    status = shift.status,
                    onStatusClick = {
                        val nextStatus = when (shift.status) {
                            "ACTIVE" -> "INACTIVE"
                            "INACTIVE" -> "SCHEDULED"
                            "SCHEDULED" -> "COMPLETED"
                            else -> "ACTIVE"
                        }
                        onToggleStatus(nextStatus)
                    }
                )
            }

            if (!shift.description.isNullOrBlank()) {
                Text(
                    text = shift.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShiftStatusPill(status: String, onStatusClick: () -> Unit) {
    val (bgColor, textColor) = when (status) {
        "ACTIVE" -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        "SCHEDULED" -> Pair(Color(0xFFDBEAFE), Color(0xFF1E40AF))
        "COMPLETED" -> Pair(Color(0xFFEDE9FE), Color(0xFF5B21B6))
        else -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable { onStatusClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "$status ↻",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun CreateOrEditShiftDialog(
    shiftToEdit: Shift?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        code: String,
        startTime: String,
        endTime: String,
        status: String,
        breakMinutes: Int,
        colorHex: String,
        description: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(shiftToEdit?.name ?: "") }
    var code by remember { mutableStateOf(shiftToEdit?.code ?: "") }
    var startTime by remember { mutableStateOf(shiftToEdit?.startTime ?: "08:00 AM") }
    var endTime by remember { mutableStateOf(shiftToEdit?.endTime ?: "04:30 PM") }
    var status by remember { mutableStateOf(shiftToEdit?.status ?: "ACTIVE") }
    var breakText by remember { mutableStateOf(shiftToEdit?.breakMinutes?.toString() ?: "30") }
    var colorHex by remember { mutableStateOf(shiftToEdit?.colorHex ?: SHIFT_COLORS.first()) }
    var description by remember { mutableStateOf(shiftToEdit?.description ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (shiftToEdit != null) "Edit Shift" else "Create New Shift",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shift Name *") },
                    placeholder = { Text("e.g. Afternoon Assembly") },
                    modifier = Modifier.fillMaxWidth().testTag("shift_name_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code *") },
                        placeholder = { Text("A-02") },
                        modifier = Modifier.weight(1f).testTag("shift_code_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = breakText,
                        onValueChange = { breakText = it },
                        label = { Text("Break (mins)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("shift_break_input"),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time *") },
                        placeholder = { Text("08:00 AM") },
                        modifier = Modifier.weight(1f).testTag("shift_start_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time *") },
                        placeholder = { Text("04:30 PM") },
                        modifier = Modifier.weight(1f).testTag("shift_end_input"),
                        singleLine = true
                    )
                }

                Text(
                    text = "Shift Status",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ACTIVE", "INACTIVE", "SCHEDULED").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("shift_desc_input"),
                    maxLines = 2
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
                    if (name.isBlank() || code.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                        errorMsg = "Please fill in all required shift fields."
                    } else {
                        val breakMin = breakText.toIntOrNull() ?: 30
                        onSave(name, code, startTime, endTime, status, breakMin, colorHex, description)
                    }
                },
                modifier = Modifier.testTag("save_shift_btn")
            ) {
                Text(if (shiftToEdit != null) "Update Shift" else "Save Shift")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
