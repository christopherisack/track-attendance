package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["workerId", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val date: String, // Format: YYYY-MM-DD
    val status: String = AttendanceStatus.PRESENT.name,
    val checkInTime: String? = null, // e.g. "08:05 AM"
    val checkOutTime: String? = null, // e.g. "04:30 PM"
    val hoursWorked: Double = 8.0,
    val overtimeHours: Double = 0.0,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
