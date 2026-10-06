package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LeaveRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_records ORDER BY appliedAt DESC")
    fun getAllLeaves(): Flow<List<LeaveRecord>>

    @Query("SELECT * FROM leave_records WHERE :date BETWEEN startDate AND endDate AND status = 'APPROVED'")
    fun getApprovedLeavesForDate(date: String): Flow<List<LeaveRecord>>

    @Query("SELECT * FROM leave_records WHERE workerId = :workerId ORDER BY appliedAt DESC")
    fun getLeavesForWorker(workerId: Long): Flow<List<LeaveRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(leave: LeaveRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaves(leaves: List<LeaveRecord>)

    @Update
    suspend fun updateLeave(leave: LeaveRecord)

    @Delete
    suspend fun deleteLeave(leave: LeaveRecord)

    @Query("DELETE FROM leave_records WHERE id = :id")
    suspend fun deleteLeaveById(id: Long)

    @Query("DELETE FROM leave_records WHERE workerId = :workerId")
    suspend fun deleteLeavesByWorkerId(workerId: Long)
}
