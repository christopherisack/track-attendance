package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Worker
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY fullName ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE isActive = 1 ORDER BY fullName ASC")
    fun getActiveWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: Long): Flow<Worker?>

    @Query("SELECT * FROM workers WHERE id = :id")
    suspend fun getWorkerByIdOnce(id: Long): Worker?

    @Query("SELECT COUNT(*) FROM workers")
    suspend fun getWorkerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkers(workers: List<Worker>)

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    @Query("DELETE FROM workers WHERE id = :id")
    suspend fun deleteWorkerById(id: Long)
}
