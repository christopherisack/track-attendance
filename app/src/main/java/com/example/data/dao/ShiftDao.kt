package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Shift
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY startTime ASC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' ORDER BY startTime ASC")
    fun getActiveShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE status = :status ORDER BY startTime ASC")
    fun getShiftsByStatus(status: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :id")
    fun getShiftById(id: Long): Flow<Shift?>

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getShiftByIdOnce(id: Long): Shift?

    @Query("SELECT COUNT(*) FROM shifts")
    suspend fun getShiftCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<Shift>)

    @Update
    suspend fun updateShift(shift: Shift)

    @Query("UPDATE shifts SET status = :status WHERE id = :id")
    suspend fun updateShiftStatus(id: Long, status: String)

    @Delete
    suspend fun deleteShift(shift: Shift)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Long)
}
