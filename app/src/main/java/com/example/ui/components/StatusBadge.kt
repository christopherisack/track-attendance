package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.ui.theme.StatusAbsent
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentText
import com.example.ui.theme.StatusHalfDay
import com.example.ui.theme.StatusHalfDayBg
import com.example.ui.theme.StatusHalfDayText
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusLateBg
import com.example.ui.theme.StatusLateText
import com.example.ui.theme.StatusLeave
import com.example.ui.theme.StatusLeaveBg
import com.example.ui.theme.StatusLeaveText
import com.example.ui.theme.StatusPresent
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentText

data class StatusVisual(
    val bg: Color,
    val text: Color,
    val accent: Color,
    val icon: ImageVector
)

fun getStatusVisual(status: AttendanceStatus): StatusVisual = when (status) {
    AttendanceStatus.PRESENT -> StatusVisual(StatusPresentBg, StatusPresentText, StatusPresent, Icons.Default.CheckCircle)
    AttendanceStatus.LATE -> StatusVisual(StatusLateBg, StatusLateText, StatusLate, Icons.Default.Schedule)
    AttendanceStatus.HALF_DAY -> StatusVisual(StatusHalfDayBg, StatusHalfDayText, StatusHalfDay, Icons.Default.Timelapse)
    AttendanceStatus.ABSENT -> StatusVisual(StatusAbsentBg, StatusAbsentText, StatusAbsent, Icons.Default.Close)
    AttendanceStatus.ON_LEAVE -> StatusVisual(StatusLeaveBg, StatusLeaveText, StatusLeave, Icons.Default.BeachAccess)
}

@Composable
fun StatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val visual = getStatusVisual(status)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(visual.bg)
            .border(1.dp, visual.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(visual.accent)
        )
        Text(
            text = status.label,
            color = visual.text,
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusSelectorRow(
    selectedStatus: AttendanceStatus,
    onStatusSelected: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "status_selector"
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttendanceStatus.entries.forEach { status ->
            val isSelected = status == selectedStatus
            val visual = getStatusVisual(status)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) visual.accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { onStatusSelected(status) }
                    .testTag("${testTagPrefix}_${status.name.lowercase()}")
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (status) {
                        AttendanceStatus.PRESENT -> "Pres"
                        AttendanceStatus.LATE -> "Late"
                        AttendanceStatus.HALF_DAY -> "Half"
                        AttendanceStatus.ABSENT -> "Abs"
                        AttendanceStatus.ON_LEAVE -> "Leave"
                    },
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
