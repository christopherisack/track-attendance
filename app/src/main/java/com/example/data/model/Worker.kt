package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val empCode: String,
    val fullName: String,
    val role: String,
    val department: String,
    val phone: String,
    val shiftName: String = "Morning (08:00 - 16:30)",
    val hourlyRate: Double = 18.0,
    val avatarColorHex: String = "#1E3A8A",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
