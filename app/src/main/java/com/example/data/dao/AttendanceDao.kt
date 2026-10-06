package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getAttendanceForDateRange(startDate: String, endDate: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId ORDER BY date DESC")
    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE workerId = :workerId AND date = :date LIMIT 1")
    suspend fun getAttendanceForWorkerAndDate(workerId: Long, date: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: AttendanceRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(records: List<AttendanceRecord>)

    @Update
    suspend fun update(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE workerId = :workerId AND date = :date")
    suspend fun deleteByWorkerAndDate(workerId: Long, date: String)

    @Query("DELETE FROM attendance_records WHERE workerId = :workerId")
    suspend fun deleteByWorkerId(workerId: Long)
}
