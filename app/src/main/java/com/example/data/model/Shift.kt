package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val startTime: String, // e.g. "07:00 AM" or "07:00"
    val endTime: String,   // e.g. "03:30 PM" or "15:30"
    val status: String = ShiftStatus.ACTIVE.name, // "ACTIVE", "INACTIVE", "SCHEDULED", "COMPLETED"
    val breakMinutes: Int = 30,
    val colorHex: String = "#1E3A8A",
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
