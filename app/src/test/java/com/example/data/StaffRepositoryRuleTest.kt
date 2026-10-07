package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.firestore.FirestoreAttendanceRecord
import com.example.data.model.firestore.FirestoreWorker
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun addWorker_authenticatedOwner_createsDocumentSuccessfully() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = StaffFirestoreRepository(firestore, auth)

        val worker = FirestoreWorker(
            id = "test_worker_1",
            userId = aliceUid,
            empCode = "EMP-777",
            fullName = "Alice Cooper",
            role = "Supervisor",
            department = "Operations"
        )

        val workerId = repo.addWorker(worker)
        assertEquals("test_worker_1", workerId)

        val workers = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeWorkers().first { it.any { w -> w.id == "test_worker_1" } }
        }
        assertTrue(workers.any { it.fullName == "Alice Cooper" })
    }

    @Test
    fun recordAttendance_authenticatedOwner_recordsSuccessfully() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = StaffFirestoreRepository(firestore, auth)

        val record = FirestoreAttendanceRecord(
            id = "att_test_1",
            userId = aliceUid,
            workerId = "test_worker_1",
            date = "2026-10-07",
            status = "PRESENT",
            checkInTime = "08:00 AM",
            checkOutTime = "04:30 PM",
            hoursWorked = 8.0
        )

        repo.recordAttendance(record)

        val records = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeAttendanceForDate("2026-10-07").first { it.isNotEmpty() }
        }
        assertTrue(records.any { it.workerId == "test_worker_1" && it.status == "PRESENT" })
    }

    @Test
    fun observeWorkers_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StaffFirestoreRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeWorkers().first()
            }
            // Should not reach here
            assertTrue(false)
        } catch (e: Exception) {
            // Unauthenticated callers should fail with IllegalStateException (requireUserId) or PERMISSION_DENIED
            assertTrue(e is IllegalStateException || e is FirebaseFirestoreException)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rules@test.com"
        const val BOB_EMAIL = "bob_rules@test.com"
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
