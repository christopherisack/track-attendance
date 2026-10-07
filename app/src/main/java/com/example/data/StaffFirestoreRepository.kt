package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.firestore.FirestoreAttendanceRecord
import com.example.data.model.firestore.FirestoreLeaveRecord
import com.example.data.model.firestore.FirestoreShift
import com.example.data.model.firestore.FirestoreUserProfile
import com.example.data.model.firestore.FirestoreWorker
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class StaffFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    // Secondary constructor resolving named database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---
    fun observeUserProfile(): Flow<FirestoreUserProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(FirestoreUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun createOrUpdateProfile(displayName: String?, email: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val data = mapOf(
            "userId" to uid,
            "displayName" to displayName,
            "email" to email,
            "role" to "Manager",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Workers ---
    fun observeWorkers(): Flow<List<FirestoreWorker>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("workers")
        emitAll(
            colRef.snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreWorker::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.fullName }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    suspend fun addWorker(worker: FirestoreWorker): String {
        val uid = requireUserId()
        val id = worker.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = db.collection("users").document(uid).collection("workers").document(id)
        val payload = mapOf(
            "id" to id,
            "userId" to uid,
            "empCode" to worker.empCode,
            "fullName" to worker.fullName,
            "role" to worker.role,
            "department" to worker.department,
            "phone" to worker.phone,
            "shiftName" to worker.shiftName,
            "hourlyRate" to worker.hourlyRate,
            "avatarColorHex" to worker.avatarColorHex,
            "isActive" to worker.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
            return id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateWorker(worker: FirestoreWorker) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workers").document(worker.id)
        val payload = mapOf(
            "id" to worker.id,
            "userId" to uid,
            "empCode" to worker.empCode,
            "fullName" to worker.fullName,
            "role" to worker.role,
            "department" to worker.department,
            "phone" to worker.phone,
            "shiftName" to worker.shiftName,
            "hourlyRate" to worker.hourlyRate,
            "avatarColorHex" to worker.avatarColorHex,
            "isActive" to worker.isActive,
            "createdAt" to (worker.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteWorker(workerId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("workers").document(workerId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Attendance Records ---
    fun observeAttendanceForDate(date: String): Flow<List<FirestoreAttendanceRecord>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("attendance_records")
        emitAll(
            colRef.whereEqualTo("date", date)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreAttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    fun observeAllAttendance(): Flow<List<FirestoreAttendanceRecord>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("attendance_records")
        emitAll(
            colRef.snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreAttendanceRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    suspend fun recordAttendance(record: FirestoreAttendanceRecord) {
        val uid = requireUserId()
        val docId = if (record.id.isNotBlank()) record.id else "${record.workerId}_${record.date}"
        val docRef = db.collection("users").document(uid).collection("attendance_records").document(docId)
        val payload = mapOf(
            "id" to docId,
            "userId" to uid,
            "workerId" to record.workerId,
            "date" to record.date,
            "status" to record.status,
            "checkInTime" to record.checkInTime,
            "checkOutTime" to record.checkOutTime,
            "hoursWorked" to record.hoursWorked,
            "overtimeHours" to record.overtimeHours,
            "notes" to record.notes,
            "createdAt" to (record.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun markAllPresent(date: String, workerIds: List<String>) {
        val uid = requireUserId()
        val batch = db.batch()
        workerIds.forEach { workerId ->
            val docId = "${workerId}_$date"
            val docRef = db.collection("users").document(uid).collection("attendance_records").document(docId)
            val payload = mapOf(
                "id" to docId,
                "userId" to uid,
                "workerId" to workerId,
                "date" to date,
                "status" to "PRESENT",
                "checkInTime" to "08:00 AM",
                "checkOutTime" to "04:30 PM",
                "hoursWorked" to 8.0,
                "overtimeHours" to 0.0,
                "notes" to "Auto-marked present",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(docRef, payload)
        }
        try {
            batch.commit().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "batch_mark_present")
            throw e
        }
    }

    // --- Shifts ---
    fun observeShifts(): Flow<List<FirestoreShift>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("shifts")
        emitAll(
            colRef.snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreShift::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.startTime }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    suspend fun addShift(shift: FirestoreShift): String {
        val uid = requireUserId()
        val id = shift.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = db.collection("users").document(uid).collection("shifts").document(id)
        val payload = mapOf(
            "id" to id,
            "userId" to uid,
            "name" to shift.name,
            "code" to shift.code,
            "startTime" to shift.startTime,
            "endTime" to shift.endTime,
            "status" to shift.status,
            "breakMinutes" to shift.breakMinutes,
            "colorHex" to shift.colorHex,
            "description" to shift.description,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
            return id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateShift(shift: FirestoreShift) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("shifts").document(shift.id)
        val payload = mapOf(
            "id" to shift.id,
            "userId" to uid,
            "name" to shift.name,
            "code" to shift.code,
            "startTime" to shift.startTime,
            "endTime" to shift.endTime,
            "status" to shift.status,
            "breakMinutes" to shift.breakMinutes,
            "colorHex" to shift.colorHex,
            "description" to shift.description,
            "createdAt" to (shift.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateShiftStatus(shiftId: String, status: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteShift(shiftId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("shifts").document(shiftId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Leaves ---
    fun observeLeaves(): Flow<List<FirestoreLeaveRecord>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("leave_records")
        emitAll(
            colRef.snapshots()
                .map { snap ->
                    snap.toObjects(FirestoreLeaveRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.startDate }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, colRef.path)
                    throw error
                }
        )
    }

    suspend fun addLeave(leave: FirestoreLeaveRecord): String {
        val uid = requireUserId()
        val id = leave.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = db.collection("users").document(uid).collection("leave_records").document(id)
        val payload = mapOf(
            "id" to id,
            "userId" to uid,
            "workerId" to leave.workerId,
            "leaveType" to leave.leaveType,
            "startDate" to leave.startDate,
            "endDate" to leave.endDate,
            "reason" to leave.reason,
            "status" to leave.status,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(payload).await()
            return id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLeaveStatus(leaveId: String, status: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("leave_records").document(leaveId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteLeave(leaveId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("leave_records").document(leaveId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Seeder for New Cloud Users ---
    suspend fun seedInitialDataIfEmpty() {
        val uid = requireUserId()
        val workersRef = db.collection("users").document(uid).collection("workers")
        val existingWorkers = workersRef.limit(1).get().await()
        if (!existingWorkers.isEmpty) return

        // 1. Seed Shifts
        val defaultShifts = listOf(
            FirestoreShift(
                id = "shift_morning",
                userId = uid,
                name = "Morning Shift",
                code = "M-01",
                startTime = "07:00 AM",
                endTime = "03:30 PM",
                status = "ACTIVE",
                breakMinutes = 30,
                colorHex = "#1E40AF",
                description = "Early production shift"
            ),
            FirestoreShift(
                id = "shift_day",
                userId = uid,
                name = "General Day Shift",
                code = "G-01",
                startTime = "08:30 AM",
                endTime = "05:00 PM",
                status = "ACTIVE",
                breakMinutes = 45,
                colorHex = "#0F766E",
                description = "Standard operational workday"
            ),
            FirestoreShift(
                id = "shift_evening",
                userId = uid,
                name = "Evening Shift",
                code = "E-01",
                startTime = "03:00 PM",
                endTime = "11:30 PM",
                status = "ACTIVE",
                breakMinutes = 30,
                colorHex = "#D97706",
                description = "Late shift logistics & assembly"
            )
        )
        defaultShifts.forEach { addShift(it) }

        // 2. Seed Workers
        val defaultWorkers = listOf(
            FirestoreWorker(
                id = "w1",
                userId = uid,
                empCode = "EMP-101",
                fullName = "Marcus Vance",
                role = "Lead Machinist",
                department = "Operations",
                phone = "+1 (555) 234-5671",
                shiftName = "Morning (07:00 - 15:30)",
                hourlyRate = 26.50,
                avatarColorHex = "#1E40AF"
            ),
            FirestoreWorker(
                id = "w2",
                userId = uid,
                empCode = "EMP-102",
                fullName = "Sarah Jenkins",
                role = "Assembly Supervisor",
                department = "Production",
                phone = "+1 (555) 345-6782",
                shiftName = "Morning (08:00 - 16:30)",
                hourlyRate = 24.00,
                avatarColorHex = "#0F766E"
            ),
            FirestoreWorker(
                id = "w3",
                userId = uid,
                empCode = "EMP-103",
                fullName = "David Chen",
                role = "Inventory Specialist",
                department = "Logistics",
                phone = "+1 (555) 456-7893",
                shiftName = "General (08:30 - 17:00)",
                hourlyRate = 20.00,
                avatarColorHex = "#D97706"
            ),
            FirestoreWorker(
                id = "w4",
                userId = uid,
                empCode = "EMP-104",
                fullName = "Elena Rostova",
                role = "Quality Inspector",
                department = "QA & Compliance",
                phone = "+1 (555) 567-8904",
                shiftName = "Morning (08:00 - 16:30)",
                hourlyRate = 22.50,
                avatarColorHex = "#7C3AED"
            ),
            FirestoreWorker(
                id = "w5",
                userId = uid,
                empCode = "EMP-105",
                fullName = "Robert Taylor",
                role = "Equipment Operator",
                department = "Operations",
                phone = "+1 (555) 678-9015",
                shiftName = "Morning (07:00 - 15:30)",
                hourlyRate = 25.00,
                avatarColorHex = "#2563EB"
            )
        )
        defaultWorkers.forEach { addWorker(it) }

        // 3. Seed Today's Attendance
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        recordAttendance(
            FirestoreAttendanceRecord(
                userId = uid,
                workerId = "w1",
                date = todayStr,
                status = "PRESENT",
                checkInTime = "06:55 AM",
                checkOutTime = "03:35 PM",
                hoursWorked = 8.5,
                overtimeHours = 0.5,
                notes = "Shift completed"
            )
        )
        recordAttendance(
            FirestoreAttendanceRecord(
                userId = uid,
                workerId = "w2",
                date = todayStr,
                status = "PRESENT",
                checkInTime = "07:58 AM",
                checkOutTime = null,
                hoursWorked = 5.0,
                overtimeHours = 0.0
            )
        )
        recordAttendance(
            FirestoreAttendanceRecord(
                userId = uid,
                workerId = "w3",
                date = todayStr,
                status = "LATE",
                checkInTime = "09:20 AM",
                checkOutTime = null,
                hoursWorked = 4.0,
                overtimeHours = 0.0,
                notes = "Traffic delay"
            )
        )

        // 4. Seed Leave
        addLeave(
            FirestoreLeaveRecord(
                id = "leave_w4",
                userId = uid,
                workerId = "w4",
                leaveType = "Paid Leave",
                startDate = todayStr,
                endDate = todayStr,
                reason = "Scheduled family commitment",
                status = "APPROVED"
            )
        )
    }
}
