package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AttendanceViewModel
import com.example.ui.components.AddWorkerDialog
import com.example.ui.screens.AttendanceDailyScreen
import com.example.ui.screens.LeaveManagementScreen
import com.example.ui.screens.LivePunchScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.WorkerManagementScreen
import com.example.ui.theme.StaffTrackTheme
import kotlinx.coroutines.flow.collectLatest

enum class NavigationTab(val title: String, val icon: ImageVector, val tag: String) {
    DAILY("Attendance", Icons.Default.Today, "tab_daily"),
    KIOSK("Punch Clock", Icons.Default.Fingerprint, "tab_kiosk"),
    WORKERS("Workers", Icons.Default.People, "tab_workers"),
    LEAVES("Leaves", Icons.Default.BeachAccess, "tab_leaves"),
    REPORTS("Reports", Icons.Default.Assessment, "tab_reports")
}

class MainActivity : ComponentActivity() {
    private val viewModel: AttendanceViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StaffTrackTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: AttendanceViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DAILY) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showGlobalAddWorkerDialog by remember { mutableStateOf(false) }

    // Observe ViewModel flows
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDepartment by viewModel.selectedDepartment.collectAsStateWithLifecycle()
    val dailyAttendanceItems by viewModel.dailyAttendanceItems.collectAsStateWithLifecycle()
    val dailyStats by viewModel.dailyStats.collectAsStateWithLifecycle()
    val allWorkers by viewModel.allWorkers.collectAsStateWithLifecycle()
    val allLeaves by viewModel.allLeaves.collectAsStateWithLifecycle()
    val payrollSummary by viewModel.payrollSummary.collectAsStateWithLifecycle()
    val reportMonthOffset by viewModel.reportMonthOffset.collectAsStateWithLifecycle()

    // Handle user messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Android Back button handling: return to main daily tab first
    BackHandler(enabled = selectedTab != NavigationTab.DAILY) {
        selectedTab = NavigationTab.DAILY
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            NavigationTab.DAILY -> "StaffTrack Attendance"
                            NavigationTab.KIOSK -> "Live Punch Kiosk"
                            NavigationTab.WORKERS -> "Worker Directory"
                            NavigationTab.LEAVES -> "Absence & Leave Desk"
                            NavigationTab.REPORTS -> "Payroll & Attendance Reports"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            if (tab == NavigationTab.LEAVES && allLeaves.any { it.status == "PENDING" }) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(allLeaves.count { it.status == "PENDING" }.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavigationTab.DAILY -> {
                    AttendanceDailyScreen(
                        selectedDate = selectedDate,
                        items = dailyAttendanceItems,
                        stats = dailyStats,
                        searchQuery = searchQuery,
                        selectedDepartment = selectedDepartment,
                        onDateChange = { viewModel.setSelectedDate(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onDepartmentChange = { viewModel.setSelectedDepartment(it) },
                        onMarkStatus = { id, status -> viewModel.markStatus(id, status) },
                        onUpdateDetails = { id, status, cin, cout, hrs, ot, notes ->
                            viewModel.updateAttendanceDetails(id, status, cin, cout, hrs, ot, notes)
                        },
                        onMarkAllPresent = { viewModel.markAllPresent() },
                        onOpenAddWorker = { showGlobalAddWorkerDialog = true }
                    )
                }

                NavigationTab.KIOSK -> {
                    LivePunchScreen(
                        items = dailyAttendanceItems,
                        onPunchClock = { worker -> viewModel.punchClock(worker) }
                    )
                }

                NavigationTab.WORKERS -> {
                    WorkerManagementScreen(
                        workers = allWorkers,
                        onAddWorker = { empCode, name, role, dept, phone, shift, rate, hex ->
                            viewModel.addWorker(empCode, name, role, dept, phone, shift, rate, hex)
                        },
                        onUpdateWorker = { viewModel.updateWorker(it) },
                        onDeleteWorker = { viewModel.deleteWorker(it) }
                    )
                }

                NavigationTab.LEAVES -> {
                    LeaveManagementScreen(
                        leaves = allLeaves,
                        workers = allWorkers,
                        onAddLeave = { workerId, leaveType, start, end, reason ->
                            viewModel.addLeave(workerId, leaveType, start, end, reason)
                        },
                        onUpdateStatus = { leave, status -> viewModel.updateLeaveStatus(leave, status) },
                        onDeleteLeave = { viewModel.deleteLeave(it) }
                    )
                }

                NavigationTab.REPORTS -> {
                    ReportsScreen(
                        summaries = payrollSummary,
                        monthOffset = reportMonthOffset,
                        onMonthOffsetChange = { viewModel.setReportMonthOffset(it) }
                    )
                }
            }
        }
    }

    if (showGlobalAddWorkerDialog) {
        AddWorkerDialog(
            onDismiss = { showGlobalAddWorkerDialog = false },
            onSave = { empCode, fullName, role, department, phone, shiftName, hourlyRate, avatarColorHex ->
                viewModel.addWorker(empCode, fullName, role, department, phone, shiftName, hourlyRate, avatarColorHex)
                showGlobalAddWorkerDialog = false
            }
        )
    }
}
