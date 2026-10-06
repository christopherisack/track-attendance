package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkerPayrollSummary
import com.example.ui.components.WorkerAvatar
import com.example.ui.theme.StatusLate
import com.example.ui.theme.StatusPresent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReportsScreen(
    summaries: List<WorkerPayrollSummary>,
    monthOffset: Int,
    onMonthOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val targetMonth = remember(monthOffset) {
        LocalDate.now().plusMonths(monthOffset.toLong())
    }
    val monthTitle = remember(targetMonth) {
        targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
    }

    val totalRegularHours = summaries.sumOf { it.regularHours }
    val totalOtHours = summaries.sumOf { it.overtimeHours }
    val totalEstimatedPay = summaries.sumOf { it.totalPay }
    val totalPresentDays = summaries.sumOf { it.presentDays }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Selector Bar
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onMonthOffsetChange(monthOffset - 1) },
                        modifier = Modifier.testTag("reports_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { onMonthOffsetChange(monthOffset + 1) },
                        enabled = monthOffset < 0,
                        modifier = Modifier.testTag("reports_next_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                    }
                }
            }
        }

        // Executive Overview Metrics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Monthly Financial & Hours Overview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Est. Total Payroll",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", totalEstimatedPay)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Logged Hours",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalRegularHours + totalOtHours)} hrs",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricPill("Regular", "${String.format(Locale.US, "%.1f", totalRegularHours)}h", MaterialTheme.colorScheme.onSurface)
                        MetricPill("Overtime", "${String.format(Locale.US, "%.1f", totalOtHours)}h", StatusLate)
                        MetricPill("Present Logs", "$totalPresentDays", StatusPresent)
                    }

                    Button(
                        onClick = {
                            shareAttendanceReport(context, monthTitle, summaries, totalEstimatedPay, totalRegularHours, totalOtHours)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_report_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Share Report")
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Worker Attendance & Wage Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Worker Breakdown List
        items(summaries, key = { it.worker.id }) { summary ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payroll_card_${summary.worker.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            name = summary.worker.fullName,
                            colorHex = summary.worker.avatarColorHex,
                            size = 46.dp
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = summary.worker.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${summary.worker.empCode} • ${summary.worker.department}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", summary.totalPay)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", summary.worker.hourlyRate)}/hr",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Days Attendance Summary Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Pres: ${summary.presentDays}", fontSize = 11.sp, color = StatusPresent, fontWeight = FontWeight.Bold)
                        Text(text = "Late: ${summary.lateDays}", fontSize = 11.sp, color = StatusLate, fontWeight = FontWeight.Bold)
                        Text(text = "Half: ${summary.halfDays}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                        Text(text = "Abs: ${summary.absentDays}", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                        Text(text = "Leave: ${summary.leaveDays}", fontSize = 11.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Medium)
                    }

                    // Hours & Wages Detail
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Regular: ${String.format(Locale.US, "%.1f", summary.regularHours)}h ($${String.format(Locale.US, "%.2f", summary.basePay)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (summary.overtimeHours > 0) {
                            Text(
                                text = "OT: ${String.format(Locale.US, "%.1f", summary.overtimeHours)}h ($${String.format(Locale.US, "%.2f", summary.overtimePay)})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusLate
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun shareAttendanceReport(
    context: Context,
    monthTitle: String,
    summaries: List<WorkerPayrollSummary>,
    totalEstimatedPay: Double,
    totalRegularHours: Double,
    totalOtHours: Double
) {
    val reportBuilder = StringBuilder()
    reportBuilder.appendLine("==========================================")
    reportBuilder.appendLine("STAFF ATTENDANCE & PAYROLL REPORT")
    reportBuilder.appendLine("Period: $monthTitle")
    reportBuilder.appendLine("==========================================")
    reportBuilder.appendLine("Workforce Total: ${summaries.size} Workers")
    reportBuilder.appendLine("Total Regular Hours: ${String.format(Locale.US, "%.1f", totalRegularHours)} hrs")
    reportBuilder.appendLine("Total Overtime Hours: ${String.format(Locale.US, "%.1f", totalOtHours)} hrs")
    reportBuilder.appendLine("Total Estimated Payroll: $${String.format(Locale.US, "%,.2f", totalEstimatedPay)}")
    reportBuilder.appendLine("------------------------------------------")
    reportBuilder.appendLine("INDIVIDUAL WORKER SUMMARY:")
    summaries.forEach { s ->
        reportBuilder.appendLine("• ${s.worker.fullName} (${s.worker.empCode}) - ${s.worker.department}")
        reportBuilder.appendLine("  Days: ${s.presentDays} Present, ${s.lateDays} Late, ${s.absentDays} Absent, ${s.leaveDays} Leave")
        reportBuilder.appendLine("  Hours: ${String.format(Locale.US, "%.1f", s.regularHours + s.overtimeHours)} hrs | Est Pay: $${String.format(Locale.US, "%.2f", s.totalPay)}")
    }
    reportBuilder.appendLine("==========================================")
    reportBuilder.appendLine("Generated by StaffTrack Attendance Management")

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Staff Attendance & Payroll - $monthTitle")
        putExtra(Intent.EXTRA_TEXT, reportBuilder.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Attendance Report"))
}
