package danilovl.calendar.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import danilovl.calendar.data.repository.AppSettings
import danilovl.calendar.ui.CalendarViewMode
import danilovl.calendar.ui.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun UseCalendarPermissions(context: Context, appSettings: AppSettings, viewModel: CalendarViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.loadContactBirthdays()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(appSettings.importContactBirthdays) {
        if (!appSettings.importContactBirthdays) {
            viewModel.clearContactBirthdays()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadContactBirthdays()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}

@Composable
fun SyncPagerWithSelectedDate(
    pagerState: PagerState,
    selectedDate: LocalDate,
    currentViewMode: CalendarViewMode,
    viewModel: CalendarViewModel
) {
    LaunchedEffect(selectedDate) {
        val monthsOffset = java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(selectedDate))
        val targetPage = (1200 + monthsOffset).toInt()
        if (pagerState.currentPage != targetPage && !pagerState.isScrollInProgress && targetPage in 0 until 2400) {
            if (currentViewMode == CalendarViewMode.MONTH) pagerState.animateScrollToPage(targetPage)
            else pagerState.scrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val targetMonth = YearMonth.now().plusMonths((page - 1200).toLong())
            if (viewModel.currentMonth.value != targetMonth) {
                val day = viewModel.selectedDate.value.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
                viewModel.selectDate(targetMonth.atDay(day), false)
            }
        }
    }
}
