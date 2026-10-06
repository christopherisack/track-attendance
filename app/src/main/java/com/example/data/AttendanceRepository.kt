package com.example.data

import com.example.data.dao.AttendanceDao
import com.example.data.dao.LeaveDao
import com.example.data.dao.WorkerDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.LeaveRecord
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AttendanceRepository(
    private val workerDao: WorkerDao,
    private val attendanceDao: AttendanceDao,
    private val leaveDao: LeaveDao
) {
    val allWorkers: Flow<List<Worker>> = workerDao.getAllWorkers()
    val activeWorkers: Flow<List<Worker>> = workerDao.getActiveWorkers()
    val allLeaves: Flow<List<LeaveRecord>> = leaveDao.getAllLeaves()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForDate(date)

    fun getAttendanceForDateRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForDateRange(startDate, endDate)

    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceForWorker(workerId)

    fun getApprovedLeavesForDate(date: String): Flow<List<LeaveRecord>> =
        leaveDao.getApprovedLeavesForDate(date)

    suspend fun getAttendanceForWorkerAndDate(workerId: Long, date: String): AttendanceRecord? =
        attendanceDao.getAttendanceForWorkerAndDate(workerId, date)

    suspend fun insertWorker(worker: Worker): Long = workerDao.insertWorker(worker)

    suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker)

    suspend fun deleteWorker(worker: Worker) {
        attendanceDao.deleteByWorkerId(worker.id)
        leaveDao.deleteLeavesByWorkerId(worker.id)
        workerDao.deleteWorker(worker)
    }

    suspend fun recordAttendance(
        workerId: Long,
        date: String,
        status: AttendanceStatus,
        checkInTime: String? = null,
        checkOutTime: String? = null,
        hoursWorked: Double = 8.0,
        overtimeHours: Double = 0.0,
        notes: String? = null
    ) {
        val existing = attendanceDao.getAttendanceForWorkerAndDate(workerId, date)
        val record = AttendanceRecord(
            id = existing?.id ?: 0,
            workerId = workerId,
            date = date,
            status = status.name,
            checkInTime = checkInTime ?: existing?.checkInTime,
            checkOutTime = checkOutTime ?: existing?.checkOutTime,
            hoursWorked = hoursWorked,
            overtimeHours = overtimeHours,
            notes = notes ?: existing?.notes,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.insertOrUpdate(record)
    }

    suspend fun markAllPresent(date: String, workerIds: List<Long>, defaultCheckIn: String = "08:00 AM", defaultCheckOut: String = "04:30 PM") {
        val records = workerIds.map { workerId ->
            AttendanceRecord(
                id = 0,
                workerId = workerId,
                date = date,
                status = AttendanceStatus.PRESENT.name,
                checkInTime = defaultCheckIn,
                checkOutTime = defaultCheckOut,
                hoursWorked = 8.0,
                overtimeHours = 0.0,
                notes = "Auto-marked present",
                updatedAt = System.currentTimeMillis()
            )
        }
        attendanceDao.insertOrUpdateAll(records)
    }

    suspend fun punchIn(workerId: Long, date: String, timeStr: String) {
        val existing = attendanceDao.getAttendanceForWorkerAndDate(workerId, date)
        val record = AttendanceRecord(
            id = existing?.id ?: 0,
            workerId = workerId,
            date = date,
            status = AttendanceStatus.PRESENT.name,
            checkInTime = timeStr,
            checkOutTime = existing?.checkOutTime,
            hoursWorked = existing?.hoursWorked ?: 0.0,
            overtimeHours = existing?.overtimeHours ?: 0.0,
            notes = existing?.notes,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.insertOrUpdate(record)
    }

    suspend fun punchOut(
        workerId: Long,
        date: String,
        timeStr: String,
        calculatedHours: Double,
        overtime: Double
    ) {
        val existing = attendanceDao.getAttendanceForWorkerAndDate(workerId, date)
        val status = if (calculatedHours < 5.0) AttendanceStatus.HALF_DAY.name else AttendanceStatus.PRESENT.name
        val record = AttendanceRecord(
            id = existing?.id ?: 0,
            workerId = workerId,
            date = date,
            status = existing?.status ?: status,
            checkInTime = existing?.checkInTime ?: "08:00 AM",
            checkOutTime = timeStr,
            hoursWorked = calculatedHours,
            overtimeHours = overtime,
            notes = existing?.notes,
            updatedAt = System.currentTimeMillis()
        )
        attendanceDao.insertOrUpdate(record)
    }

    suspend fun insertLeave(leave: LeaveRecord): Long = leaveDao.insertLeave(leave)

    suspend fun updateLeave(leave: LeaveRecord) = leaveDao.updateLeave(leave)

    suspend fun deleteLeave(leave: LeaveRecord) = leaveDao.deleteLeave(leave)

    suspend fun seedInitialDataIfNeeded() {
        if (workerDao.getWorkerCount() > 0) return

        val sampleWorkers = listOf(
            Worker(
                empCode = "EMP-101",
                fullName = "Marcus Vance",
                role = "Lead Machinist",
                department = "Operations",
                phone = "+1 (555) 234-5671",
                shiftName = "Morning (07:00 - 15:30)",
                hourlyRate = 26.50,
                avatarColorHex = "#1E40AF"
            ),
            Worker(
                empCode = "EMP-102",
                fullName = "Sarah Jenkins",
                role = "Assembly Supervisor",
                department = "Production",
                phone = "+1 (555) 345-6782",
                shiftName = "Morning (08:00 - 16:30)",
                hourlyRate = 24.00,
                avatarColorHex = "#0F766E"
            ),
            Worker(
                empCode = "EMP-103",
                fullName = "David Chen",
                role = "Inventory Specialist",
                department = "Logistics",
                phone = "+1 (555) 456-7893",
                shiftName = "General (09:00 - 17:30)",
                hourlyRate = 20.00,
                avatarColorHex = "#D97706"
            ),
            Worker(
                empCode = "EMP-104",
                fullName = "Elena Rostova",
                role = "Quality Inspector",
                department = "QA & Compliance",
                phone = "+1 (555) 567-8904",
                shiftName = "Morning (08:00 - 16:30)",
                hourlyRate = 22.50,
                avatarColorHex = "#7C3AED"
            ),
            Worker(
                empCode = "EMP-105",
                fullName = "Robert Taylor",
                role = "Heavy Equipment Operator",
                department = "Operations",
                phone = "+1 (555) 678-9015",
                shiftName = "Morning (07:00 - 15:30)",
                hourlyRate = 25.00,
                avatarColorHex = "#2563EB"
            ),
            Worker(
                empCode = "EMP-106",
                fullName = "Amara Diallo",
                role = "Safety Coordinator",
                department = "Operations",
                phone = "+1 (555) 789-0126",
                shiftName = "General (09:00 - 17:30)",
                hourlyRate = 23.00,
                avatarColorHex = "#059669"
            ),
            Worker(
                empCode = "EMP-107",
                fullName = "Carlos Mendez",
                role = "Maintenance Technician",
                department = "Maintenance",
                phone = "+1 (555) 890-1237",
                shiftName = "Evening (15:00 - 23:30)",
                hourlyRate = 21.50,
                avatarColorHex = "#DC2626"
            ),
            Worker(
                empCode = "EMP-108",
                fullName = "Jessica Wong",
                role = "Logistics Dispatcher",
                department = "Logistics",
                phone = "+1 (555) 901-2348",
                shiftName = "General (08:30 - 17:00)",
                hourlyRate = 19.50,
                avatarColorHex = "#0284C7"
            )
        )

        workerDao.insertWorkers(sampleWorkers)

        // Generate realistic attendance for the past 5 days + today
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val allCreatedWorkers = workerDao.getWorkerCount() // to ensure IDs

        // Fetch back inserted workers
        // Since insertWorkers was done, let's create historical records
        val dates = (0..5).map { offset -> today.minusDays(offset.toLong()).format(formatter) }

        val sampleRecords = mutableListOf<AttendanceRecord>()
        for (i in 1..8) {
            val workerId = i.toLong()
            dates.forEachIndexed { dayIndex, dateStr ->
                // Variation per worker and date
                when {
                    // Worker 4 is on approved leave today and yesterday
                    workerId == 4L && dayIndex <= 1 -> {
                        // Handled via leave
                    }
                    // Worker 1: stellar attendance
                    workerId == 1L -> {
                        sampleRecords.add(
                            AttendanceRecord(
                                workerId = workerId,
                                date = dateStr,
                                status = AttendanceStatus.PRESENT.name,
                                checkInTime = "06:55 AM",
                                checkOutTime = "03:35 PM",
                                hoursWorked = 8.5,
                                overtimeHours = 0.5,
                                notes = "Early shift completed"
                            )
                        )
                    }
                    // Worker 3 was late on day 2
                    workerId == 3L && dayIndex == 2 -> {
                        sampleRecords.add(
                            AttendanceRecord(
                                workerId = workerId,
                                date = dateStr,
                                status = AttendanceStatus.LATE.name,
                                checkInTime = "09:42 AM",
                                checkOutTime = "05:30 PM",
                                hoursWorked = 7.8,
                                overtimeHours = 0.0,
                                notes = "Traffic delay on Route 4"
                            )
                        )
                    }
                    // Worker 7 on evening shift
                    workerId == 7L -> {
                        sampleRecords.add(
                            AttendanceRecord(
                                workerId = workerId,
                                date = dateStr,
                                status = AttendanceStatus.PRESENT.name,
                                checkInTime = "02:50 PM",
                                checkOutTime = "11:30 PM",
                                hoursWorked = 8.5,
                                overtimeHours = 0.5,
                                notes = "Evening routine"
                            )
                        )
                    }
                    // Day 0 (today) - some clocked in, one late
                    dayIndex == 0 -> {
                        when (workerId) {
                            2L -> sampleRecords.add(
                                AttendanceRecord(
                                    workerId = workerId,
                                    date = dateStr,
                                    status = AttendanceStatus.PRESENT.name,
                                    checkInTime = "07:58 AM",
                                    checkOutTime = null,
                                    hoursWorked = 5.5,
                                    overtimeHours = 0.0
                                )
                            )
                            3L -> sampleRecords.add(
                                AttendanceRecord(
                                    workerId = workerId,
                                    date = dateStr,
                                    status = AttendanceStatus.LATE.name,
                                    checkInTime = "09:25 AM",
                                    checkOutTime = null,
                                    hoursWorked = 4.0,
                                    overtimeHours = 0.0,
                                    notes = "Late arrival reported"
                                )
                            )
                            5L -> sampleRecords.add(
                                AttendanceRecord(
                                    workerId = workerId,
                                    date = dateStr,
                                    status = AttendanceStatus.PRESENT.name,
                                    checkInTime = "06:58 AM",
                                    checkOutTime = null,
                                    hoursWorked = 6.5,
                                    overtimeHours = 0.0
                                )
                            )
                            6L -> sampleRecords.add(
                                AttendanceRecord(
                                    workerId = workerId,
                                    date = dateStr,
                                    status = AttendanceStatus.PRESENT.name,
                                    checkInTime = "08:50 AM",
                                    checkOutTime = null,
                                    hoursWorked = 4.5,
                                    overtimeHours = 0.0
                                )
                            )
                            8L -> sampleRecords.add(
                                AttendanceRecord(
                                    workerId = workerId,
                                    date = dateStr,
                                    status = AttendanceStatus.PRESENT.name,
                                    checkInTime = "08:28 AM",
                                    checkOutTime = null,
                                    hoursWorked = 5.0,
                                    overtimeHours = 0.0
                                )
                            )
                        }
                    }
                    else -> {
                        sampleRecords.add(
                            AttendanceRecord(
                                workerId = workerId,
                                date = dateStr,
                                status = AttendanceStatus.PRESENT.name,
                                checkInTime = "08:00 AM",
                                checkOutTime = "04:30 PM",
                                hoursWorked = 8.0,
                                overtimeHours = 0.0
                            )
                        )
                    }
                }
            }
        }

        attendanceDao.insertOrUpdateAll(sampleRecords)

        // Seed sample leave
        leaveDao.insertLeave(
            LeaveRecord(
                workerId = 4L,
                leaveType = "Paid Leave",
                startDate = today.minusDays(1).format(formatter),
                endDate = today.plusDays(1).format(formatter),
                reason = "Annual family event approved by operations manager",
                status = "APPROVED"
            )
        )
    }
}
