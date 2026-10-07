package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.LeaveDao
import com.example.data.dao.ShiftDao
import com.example.data.dao.WorkerDao
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRecord
import com.example.data.model.Shift
import com.example.data.model.Worker

@Database(
    entities = [
        Worker::class,
        AttendanceRecord::class,
        LeaveRecord::class,
        Shift::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun leaveDao(): LeaveDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "staff_track_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
