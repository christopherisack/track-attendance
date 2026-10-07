package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.model.AttendanceStatus
import com.example.data.model.DailyAttendanceStats
import com.example.data.model.Shift
import com.example.data.model.ShiftStatus
import com.example.data.model.Worker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AttendanceRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = AttendanceRepository(
            database.workerDao(),
            database.attendanceDao(),
            database.leaveDao(),
            database.shiftDao()
        )
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("StaffTrack", appName)
    }

    @Test
    fun testWorkerInsertionAndAttendance() = runBlocking {
        val worker = Worker(
            empCode = "EMP-999",
            fullName = "Alex Hunter",
            role = "Quality Lead",
            department = "QA & Compliance",
            phone = "+1 555-0100",
            hourlyRate = 25.0
        )
        val workerId = repository.insertWorker(worker)
        assertTrue(workerId > 0)

        // Mark Attendance
        val testDate = "2026-10-06"
        repository.recordAttendance(
            workerId = workerId,
            date = testDate,
            status = AttendanceStatus.PRESENT,
            checkInTime = "08:00 AM",
            checkOutTime = "04:30 PM",
            hoursWorked = 8.0,
            overtimeHours = 1.0,
            notes = "Test completed"
        )

        val record = repository.getAttendanceForWorkerAndDate(workerId, testDate)
        assertNotNull(record)
        assertEquals(AttendanceStatus.PRESENT.name, record?.status)
        assertEquals("08:00 AM", record?.checkInTime)
        assertEquals(8.0, record?.hoursWorked ?: 0.0, 0.01)
    }

    @Test
    fun testShiftEntityAndDaoOperations() = runBlocking {
        val shiftDao = database.shiftDao()
        val shift = Shift(
            name = "Night Security Shift",
            code = "NS-01",
            startTime = "10:00 PM",
            endTime = "06:00 AM",
            status = ShiftStatus.ACTIVE.name,
            breakMinutes = 45,
            colorHex = "#334155",
            description = "Facility overnight monitoring"
        )

        val shiftId = shiftDao.insertShift(shift)
        assertTrue(shiftId > 0)

        val fetchedShift = shiftDao.getShiftByIdOnce(shiftId)
        assertNotNull(fetchedShift)
        assertEquals("Night Security Shift", fetchedShift?.name)
        assertEquals("NS-01", fetchedShift?.code)
        assertEquals("10:00 PM", fetchedShift?.startTime)
        assertEquals("06:00 AM", fetchedShift?.endTime)
        assertEquals("ACTIVE", fetchedShift?.status)
        assertEquals(45, fetchedShift?.breakMinutes)

        // Update shift status
        shiftDao.updateShiftStatus(shiftId, ShiftStatus.INACTIVE.name)
        val updatedShift = shiftDao.getShiftByIdOnce(shiftId)
        assertEquals("INACTIVE", updatedShift?.status)

        // Delete shift
        shiftDao.deleteShiftById(shiftId)
        val deletedShift = shiftDao.getShiftByIdOnce(shiftId)
        assertNull(deletedShift)
    }

    @Test
    fun testAttendanceStatsCalculation() {
        val stats = DailyAttendanceStats(
            totalWorkers = 10,
            presentCount = 8,
            lateCount = 1,
            halfDayCount = 1,
            absentCount = 0,
            onLeaveCount = 0
        )
        // 8 + 1 + 0.5 = 9.5 out of 10 -> 95%
        assertEquals(95, stats.attendancePercentage)
    }
}
