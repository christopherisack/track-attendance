package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leave_records")
data class LeaveRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: Long,
    val leaveType: String, // "Sick Leave", "Paid Leave", "Casual Leave", "Emergency"
    val startDate: String, // YYYY-MM-DD
    val endDate: String, // YYYY-MM-DD
    val reason: String,
    val status: String = "APPROVED", // "APPROVED", "PENDING", "REJECTED"
    val appliedAt: Long = System.currentTimeMillis()
)
