package com.example.data.model

data class WorkerAttendanceItem(
    val worker: Worker,
    val attendance: AttendanceRecord?,
    val activeLeave: LeaveRecord? = null
) {
    val currentStatus: AttendanceStatus
        get() = when {
            attendance != null -> AttendanceStatus.fromString(attendance.status)
            activeLeave != null && activeLeave.status == "APPROVED" -> AttendanceStatus.ON_LEAVE
            else -> AttendanceStatus.ABSENT // or default unrecorded
        }

    val isRecorded: Boolean
        get() = attendance != null || (activeLeave != null && activeLeave.status == "APPROVED")
}

data class DailyAttendanceStats(
    val totalWorkers: Int = 0,
    val presentCount: Int = 0,
    val lateCount: Int = 0,
    val halfDayCount: Int = 0,
    val absentCount: Int = 0,
    val onLeaveCount: Int = 0,
    val unrecordedCount: Int = 0
) {
    val attendancePercentage: Int
        get() = if (totalWorkers > 0) {
            val attended = presentCount + lateCount + (halfDayCount * 0.5f)
            ((attended / totalWorkers.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else 0
}

data class WorkerPayrollSummary(
    val worker: Worker,
    val presentDays: Int = 0,
    val lateDays: Int = 0,
    val halfDays: Int = 0,
    val absentDays: Int = 0,
    val leaveDays: Int = 0,
    val regularHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val basePay: Double = 0.0,
    val overtimePay: Double = 0.0,
    val totalPay: Double = 0.0
)
