package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.LeaveRecord
import com.example.data.model.Worker
import com.example.data.model.WorkerAttendanceItem
import com.example.data.model.WorkerPayrollSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AttendanceRepository(
        database.workerDao(),
        database.attendanceDao(),
        database.leaveDao()
    )

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

    // Current selected date for roll call
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Search query and department filter for daily roll call
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDepartment = MutableStateFlow("All")
    val selectedDepartment: StateFlow<String> = _selectedDepartment.asStateFlow()

    // Notification / snackbar message events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val allWorkers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLeaves: StateFlow<List<LeaveRecord>> = repository.allLeaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time attendance items for selected date
    val dailyAttendanceItems: StateFlow<List<WorkerAttendanceItem>> = combine(
        allWorkers,
        _selectedDate.flatMapLatest { date ->
            val dateStr = date.format(dateFormatter)
            repository.getAttendanceForDate(dateStr)
        },
        _selectedDate.flatMapLatest { date ->
            val dateStr = date.format(dateFormatter)
            repository.getApprovedLeavesForDate(dateStr)
        },
        _searchQuery,
        _selectedDepartment
    ) { workers, attendances, leaves, query, dept ->
        val attendanceMap = attendances.associateBy { it.workerId }
        val leaveMap = leaves.associateBy { it.workerId }

        workers
            .filter { it.isActive }
            .filter { worker ->
                val matchesQuery = query.isBlank() ||
                    worker.fullName.contains(query, ignoreCase = true) ||
                    worker.empCode.contains(query, ignoreCase = true) ||
                    worker.role.contains(query, ignoreCase = true)
                val matchesDept = dept == "All" || worker.department.equals(dept, ignoreCase = true)
                matchesQuery && matchesDept
            }
            .map { worker ->
                WorkerAttendanceItem(
                    worker = worker,
                    attendance = attendanceMap[worker.id],
                    activeLeave = leaveMap[worker.id]
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily attendance statistics
    val dailyStats: StateFlow<DailyAttendanceStats> = combine(
        allWorkers,
        _selectedDate.flatMapLatest { date ->
            val dateStr = date.format(dateFormatter)
            repository.getAttendanceForDate(dateStr)
        },
        _selectedDate.flatMapLatest { date ->
            val dateStr = date.format(dateFormatter)
            repository.getApprovedLeavesForDate(dateStr)
        }
    ) { workers, attendances, leaves ->
        val activeWorkers = workers.filter { it.isActive }
        val total = activeWorkers.size
        val attMap = attendances.associateBy { it.workerId }
        val leaveMap = leaves.associateBy { it.workerId }

        var present = 0
        var late = 0
        var halfDay = 0
        var absent = 0
        var onLeave = 0
        var unrecorded = 0

        activeWorkers.forEach { worker ->
            val att = attMap[worker.id]
            val leave = leaveMap[worker.id]
            if (leave != null) {
                onLeave++
            } else if (att != null) {
                when (AttendanceStatus.fromString(att.status)) {
                    AttendanceStatus.PRESENT -> present++
                    AttendanceStatus.LATE -> late++
                    AttendanceStatus.HALF_DAY -> halfDay++
                    AttendanceStatus.ABSENT -> absent++
                    AttendanceStatus.ON_LEAVE -> onLeave++
                }
            } else {
                unrecorded++
            }
        }

        DailyAttendanceStats(
            totalWorkers = total,
            presentCount = present,
            lateCount = late,
            halfDayCount = halfDay,
            absentCount = absent,
            onLeaveCount = onLeave,
            unrecordedCount = unrecorded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyAttendanceStats())

    // Date range for reports (default: current month)
    private val _reportMonthOffset = MutableStateFlow(0) // 0 = current month, -1 = last month
    val reportMonthOffset: StateFlow<Int> = _reportMonthOffset.asStateFlow()

    val payrollSummary: StateFlow<List<WorkerPayrollSummary>> = combine(
        allWorkers,
        _reportMonthOffset.flatMapLatest { offset ->
            val targetMonth = LocalDate.now().plusMonths(offset.toLong())
            val start = targetMonth.withDayOfMonth(1).format(dateFormatter)
            val end = targetMonth.withDayOfMonth(targetMonth.lengthOfMonth()).format(dateFormatter)
            repository.getAttendanceForDateRange(start, end)
        }
    ) { workers, records ->
        val recordsByWorker = records.groupBy { it.workerId }

        workers.filter { it.isActive }.map { worker ->
            val workerRecords = recordsByWorker[worker.id] ?: emptyList()
            var presentDays = 0
            var lateDays = 0
            var halfDays = 0
            var absentDays = 0
            var leaveDays = 0
            var totalRegHours = 0.0
            var totalOtHours = 0.0

            workerRecords.forEach { r ->
                when (AttendanceStatus.fromString(r.status)) {
                    AttendanceStatus.PRESENT -> presentDays++
                    AttendanceStatus.LATE -> lateDays++
                    AttendanceStatus.HALF_DAY -> halfDays++
                    AttendanceStatus.ABSENT -> absentDays++
                    AttendanceStatus.ON_LEAVE -> leaveDays++
                }
                totalRegHours += r.hoursWorked
                totalOtHours += r.overtimeHours
            }

            val basePay = totalRegHours * worker.hourlyRate
            val overtimePay = totalOtHours * (worker.hourlyRate * 1.5)
            val totalPay = basePay + overtimePay

            WorkerPayrollSummary(
                worker = worker,
                presentDays = presentDays,
                lateDays = lateDays,
                halfDays = halfDays,
                absentDays = absentDays,
                leaveDays = leaveDays,
                regularHours = totalRegHours,
                overtimeHours = totalOtHours,
                basePay = basePay,
                overtimePay = overtimePay,
                totalPay = totalPay
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedInitialDataIfNeeded()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedDepartment(dept: String) {
        _selectedDepartment.value = dept
    }

    fun setReportMonthOffset(offset: Int) {
        _reportMonthOffset.value = offset
    }

    fun markStatus(workerId: Long, status: AttendanceStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = _selectedDate.value.format(dateFormatter)
            val defaultTimes = when (status) {
                AttendanceStatus.PRESENT -> Pair("08:00 AM", "04:30 PM")
                AttendanceStatus.LATE -> Pair("09:15 AM", "04:30 PM")
                AttendanceStatus.HALF_DAY -> Pair("08:00 AM", "12:30 PM")
                AttendanceStatus.ABSENT -> Pair(null, null)
                AttendanceStatus.ON_LEAVE -> Pair(null, null)
            }
            val hours = when (status) {
                AttendanceStatus.PRESENT -> 8.0
                AttendanceStatus.LATE -> 7.25
                AttendanceStatus.HALF_DAY -> 4.0
                AttendanceStatus.ABSENT -> 0.0
                AttendanceStatus.ON_LEAVE -> 0.0
            }

            repository.recordAttendance(
                workerId = workerId,
                date = dateStr,
                status = status,
                checkInTime = defaultTimes.first,
                checkOutTime = defaultTimes.second,
                hoursWorked = hours,
                overtimeHours = 0.0
            )
        }
    }

    fun updateAttendanceDetails(
        workerId: Long,
        status: AttendanceStatus,
        checkInTime: String?,
        checkOutTime: String?,
        hoursWorked: Double,
        overtimeHours: Double,
        notes: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = _selectedDate.value.format(dateFormatter)
            repository.recordAttendance(
                workerId = workerId,
                date = dateStr,
                status = status,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                hoursWorked = hoursWorked,
                overtimeHours = overtimeHours,
                notes = notes
            )
            _userMessage.emit("Updated attendance record successfully")
        }
    }

    fun markAllPresent() {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = _selectedDate.value.format(dateFormatter)
            val workers = allWorkers.value.filter { it.isActive }
            repository.markAllPresent(dateStr, workers.map { it.id })
            _userMessage.emit("Marked ${workers.size} workers present for $dateStr")
        }
    }

    fun punchClock(worker: Worker) {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = LocalDate.now().format(dateFormatter)
            val nowTimeStr = LocalTime.now().format(timeFormatter)
            val existing = repository.getAttendanceForWorkerAndDate(worker.id, todayStr)

            if (existing == null || existing.checkInTime == null) {
                // Punch in
                repository.punchIn(worker.id, todayStr, nowTimeStr)
                _userMessage.emit("${worker.fullName} clocked in at $nowTimeStr")
            } else if (existing.checkOutTime == null) {
                // Punch out
                // Estimate worked hours
                val hours = 8.0
                val ot = 0.5
                repository.punchOut(worker.id, todayStr, nowTimeStr, hours, ot)
                _userMessage.emit("${worker.fullName} clocked out at $nowTimeStr (8 hrs)")
            } else {
                _userMessage.emit("${worker.fullName} has already completed shift today")
            }
        }
    }

    fun addWorker(
        empCode: String,
        fullName: String,
        role: String,
        department: String,
        phone: String,
        shiftName: String,
        hourlyRate: Double,
        avatarColorHex: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val worker = Worker(
                empCode = empCode.trim(),
                fullName = fullName.trim(),
                role = role.trim(),
                department = department.trim(),
                phone = phone.trim(),
                shiftName = shiftName.trim(),
                hourlyRate = hourlyRate,
                avatarColorHex = avatarColorHex
            )
            repository.insertWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} added successfully")
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} updated")
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} removed")
        }
    }

    fun addLeave(
        workerId: Long,
        leaveType: String,
        startDate: String,
        endDate: String,
        reason: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val leave = LeaveRecord(
                workerId = workerId,
                leaveType = leaveType,
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                status = "APPROVED"
            )
            repository.insertLeave(leave)
            _userMessage.emit("Leave request recorded & approved")
        }
    }

    fun updateLeaveStatus(leave: LeaveRecord, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLeave(leave.copy(status = newStatus))
            _userMessage.emit("Leave status updated to $newStatus")
        }
    }

    fun deleteLeave(leave: LeaveRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLeave(leave)
            _userMessage.emit("Leave record deleted")
        }
    }
}
