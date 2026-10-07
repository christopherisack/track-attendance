package com.example.data.model.firestore

import com.google.firebase.Timestamp

data class FirestoreUserProfile(
    val userId: String = "",
    val displayName: String? = null,
    val email: String = "",
    val role: String = "Manager",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreWorker(
    val id: String = "",
    val userId: String = "",
    val empCode: String = "",
    val fullName: String = "",
    val role: String = "",
    val department: String = "",
    val phone: String? = null,
    val shiftName: String = "Morning (07:00 - 15:30)",
    val hourlyRate: Double = 20.0,
    val avatarColorHex: String = "#1E3A8A",
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreAttendanceRecord(
    val id: String = "",
    val userId: String = "",
    val workerId: String = "",
    val date: String = "",
    val status: String = "PRESENT",
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val hoursWorked: Double = 8.0,
    val overtimeHours: Double = 0.0,
    val notes: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreShift(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "ACTIVE",
    val breakMinutes: Int = 30,
    val colorHex: String = "#1E3A8A",
    val description: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class FirestoreLeaveRecord(
    val id: String = "",
    val userId: String = "",
    val workerId: String = "",
    val leaveType: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val reason: String = "",
    val status: String = "APPROVED",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)
