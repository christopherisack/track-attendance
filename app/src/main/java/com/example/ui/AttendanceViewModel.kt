package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.StaffFirestoreRepository
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.LeaveRecord
import com.example.data.model.Shift
import com.example.data.model.Worker
import com.example.data.model.WorkerAttendanceItem
import com.example.data.model.WorkerPayrollSummary
import com.example.data.model.firestore.FirestoreAttendanceRecord
import com.example.data.model.firestore.FirestoreLeaveRecord
import com.example.data.model.firestore.FirestoreShift
import com.example.data.model.firestore.FirestoreWorker
import com.example.ui.auth.authStateFlow
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
        database.leaveDao(),
        database.shiftDao()
    )
    private val firestoreRepository = StaffFirestoreRepository(application)

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

    // Cloud sync status indicator
    private val _cloudSyncStatus = MutableStateFlow("Syncing...")
    val cloudSyncStatus: StateFlow<String> = _cloudSyncStatus.asStateFlow()

    val allWorkers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLeaves: StateFlow<List<LeaveRecord>> = repository.allLeaves
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShifts: StateFlow<List<Shift>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShifts: StateFlow<List<Shift>> = repository.activeShifts
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
        viewModelScope.launch {
            Firebase.auth.authStateFlow().collectLatest { user ->
                if (user != null) {
                    onUserAuthenticated(user)
                } else {
                    _cloudSyncStatus.value = "Sign in to sync"
                }
            }
        }
    }

    fun onUserAuthenticated(user: FirebaseUser) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _cloudSyncStatus.value = "Connecting to Firestore..."
                firestoreRepository.createOrUpdateProfile(
                    displayName = user.displayName ?: "Staff Manager",
                    email = user.email ?: ""
                )
                firestoreRepository.seedInitialDataIfEmpty()
                _cloudSyncStatus.value = "Cloud Synced (${user.email})"
            } catch (e: Exception) {
                Log.w("AttendanceVM", "Initial cloud sync notice: ${e.message}")
                _cloudSyncStatus.value = "Cloud Ready"
            }
        }
    }

    fun syncWithCloud() {
        val user = Firebase.auth.currentUser
        if (user == null) {
            viewModelScope.launch {
                _userMessage.emit("Please sign in with Google to sync")
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _cloudSyncStatus.value = "Syncing..."
                firestoreRepository.createOrUpdateProfile(
                    displayName = user.displayName ?: "Staff Manager",
                    email = user.email ?: ""
                )
                _cloudSyncStatus.value = "Cloud Synced (${user.email})"
                _userMessage.emit("Data synced with Google Cloud Firestore")
            } catch (e: Exception) {
                _cloudSyncStatus.value = "Sync error"
                _userMessage.emit("Cloud sync error: ${e.localizedMessage}")
            }
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

            // Cloud sync
            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.recordAttendance(
                        FirestoreAttendanceRecord(
                            id = "${workerId}_$dateStr",
                            userId = user.uid,
                            workerId = workerId.toString(),
                            date = dateStr,
                            status = status.name,
                            checkInTime = defaultTimes.first,
                            checkOutTime = defaultTimes.second,
                            hoursWorked = hours,
                            overtimeHours = 0.0
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud attendance sync failed: ${e.message}")
                }
            }
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

            // Cloud sync
            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.recordAttendance(
                        FirestoreAttendanceRecord(
                            id = "${workerId}_$dateStr",
                            userId = user.uid,
                            workerId = workerId.toString(),
                            date = dateStr,
                            status = status.name,
                            checkInTime = checkInTime,
                            checkOutTime = checkOutTime,
                            hoursWorked = hoursWorked,
                            overtimeHours = overtimeHours,
                            notes = notes
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud attendance sync failed: ${e.message}")
                }
            }
        }
    }

    fun markAllPresent() {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = _selectedDate.value.format(dateFormatter)
            val workers = allWorkers.value.filter { it.isActive }
            repository.markAllPresent(dateStr, workers.map { it.id })
            _userMessage.emit("Marked ${workers.size} workers present for $dateStr")

            // Cloud sync
            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.markAllPresent(dateStr, workers.map { it.id.toString() })
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud mark all present failed: ${e.message}")
                }
            }
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
                Firebase.auth.currentUser?.let { user ->
                    try {
                        firestoreRepository.recordAttendance(
                            FirestoreAttendanceRecord(
                                id = "${worker.id}_$todayStr",
                                userId = user.uid,
                                workerId = worker.id.toString(),
                                date = todayStr,
                                status = "PRESENT",
                                checkInTime = nowTimeStr,
                                checkOutTime = null,
                                hoursWorked = 0.0,
                                overtimeHours = 0.0,
                                notes = "Clocked in at kiosk"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("AttendanceVM", "Cloud punch in failed: ${e.message}")
                    }
                }
            } else if (existing.checkOutTime == null) {
                // Punch out
                val hours = 8.0
                val ot = 0.5
                repository.punchOut(worker.id, todayStr, nowTimeStr, hours, ot)
                _userMessage.emit("${worker.fullName} clocked out at $nowTimeStr (8 hrs)")
                Firebase.auth.currentUser?.let { user ->
                    try {
                        firestoreRepository.recordAttendance(
                            FirestoreAttendanceRecord(
                                id = "${worker.id}_$todayStr",
                                userId = user.uid,
                                workerId = worker.id.toString(),
                                date = todayStr,
                                status = "PRESENT",
                                checkInTime = existing.checkInTime,
                                checkOutTime = nowTimeStr,
                                hoursWorked = hours,
                                overtimeHours = ot,
                                notes = "Shift completed"
                            )
                        )
                    } catch (e: Exception) {
                        Log.w("AttendanceVM", "Cloud punch out failed: ${e.message}")
                    }
                }
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
            val newId = repository.insertWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} added successfully")

            // Cloud sync
            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.addWorker(
                        FirestoreWorker(
                            id = newId.toString(),
                            userId = user.uid,
                            empCode = worker.empCode,
                            fullName = worker.fullName,
                            role = worker.role,
                            department = worker.department,
                            phone = worker.phone,
                            shiftName = worker.shiftName,
                            hourlyRate = worker.hourlyRate,
                            avatarColorHex = worker.avatarColorHex,
                            isActive = true
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud add worker failed: ${e.message}")
                }
            }
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} updated")

            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.updateWorker(
                        FirestoreWorker(
                            id = worker.id.toString(),
                            userId = user.uid,
                            empCode = worker.empCode,
                            fullName = worker.fullName,
                            role = worker.role,
                            department = worker.department,
                            phone = worker.phone,
                            shiftName = worker.shiftName,
                            hourlyRate = worker.hourlyRate,
                            avatarColorHex = worker.avatarColorHex,
                            isActive = worker.isActive
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud update worker failed: ${e.message}")
                }
            }
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWorker(worker)
            _userMessage.emit("Worker ${worker.fullName} removed")

            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.deleteWorker(worker.id.toString())
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud delete worker failed: ${e.message}")
                }
            }
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
            val newId = repository.insertLeave(leave)
            _userMessage.emit("Leave request recorded & approved")

            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.addLeave(
                        FirestoreLeaveRecord(
                            id = newId.toString(),
                            userId = user.uid,
                            workerId = workerId.toString(),
                            leaveType = leaveType,
                            startDate = startDate,
                            endDate = endDate,
                            reason = reason,
                            status = "APPROVED"
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud add leave failed: ${e.message}")
                }
            }
        }
    }

    fun updateLeaveStatus(leave: LeaveRecord, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLeave(leave.copy(status = newStatus))
            _userMessage.emit("Leave status updated to $newStatus")

            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.updateLeaveStatus(leave.id.toString(), newStatus)
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud update leave status failed: ${e.message}")
                }
            }
        }
    }

    fun deleteLeave(leave: LeaveRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLeave(leave)
            _userMessage.emit("Leave record deleted")

            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.deleteLeave(leave.id.toString())
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud delete leave failed: ${e.message}")
                }
            }
        }
    }

    fun addShift(
        name: String,
        code: String,
        startTime: String,
        endTime: String,
        status: String = "ACTIVE",
        breakMinutes: Int = 30,
        colorHex: String = "#1E3A8A",
        description: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val shift = Shift(
                name = name.trim(),
                code = code.trim(),
                startTime = startTime.trim(),
                endTime = endTime.trim(),
                status = status,
                breakMinutes = breakMinutes,
                colorHex = colorHex,
                description = description?.trim()
            )
            val newId = repository.insertShift(shift)
            _userMessage.emit("Shift ${shift.name} created")

            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.addShift(
                        FirestoreShift(
                            id = newId.toString(),
                            userId = user.uid,
                            name = shift.name,
                            code = shift.code,
                            startTime = shift.startTime,
                            endTime = shift.endTime,
                            status = shift.status,
                            breakMinutes = shift.breakMinutes,
                            colorHex = shift.colorHex,
                            description = shift.description
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud add shift failed: ${e.message}")
                }
            }
        }
    }

    fun updateShift(shift: Shift) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateShift(shift)
            _userMessage.emit("Shift ${shift.name} updated")

            Firebase.auth.currentUser?.let { user ->
                try {
                    firestoreRepository.updateShift(
                        FirestoreShift(
                            id = shift.id.toString(),
                            userId = user.uid,
                            name = shift.name,
                            code = shift.code,
                            startTime = shift.startTime,
                            endTime = shift.endTime,
                            status = shift.status,
                            breakMinutes = shift.breakMinutes,
                            colorHex = shift.colorHex,
                            description = shift.description
                        )
                    )
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud update shift failed: ${e.message}")
                }
            }
        }
    }

    fun updateShiftStatus(shiftId: Long, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateShiftStatus(shiftId, status)
            _userMessage.emit("Shift status updated to $status")

            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.updateShiftStatus(shiftId.toString(), status)
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud update shift status failed: ${e.message}")
                }
            }
        }
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteShift(shift)
            _userMessage.emit("Shift ${shift.name} removed")

            Firebase.auth.currentUser?.let { _ ->
                try {
                    firestoreRepository.deleteShift(shift.id.toString())
                } catch (e: Exception) {
                    Log.w("AttendanceVM", "Cloud delete shift failed: ${e.message}")
                }
            }
        }
    }
}
