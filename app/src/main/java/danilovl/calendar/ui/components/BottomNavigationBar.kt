package danilovl.calendar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.ui.CalendarViewMode
import danilovl.calendar.ui.theme.XiaomiSurface

@Composable
fun BottomNavigationBar(currentMode: CalendarViewMode, onModeSelect: (CalendarViewMode) -> Unit) {
    Surface(color = XiaomiSurface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    Triple(CalendarViewMode.YEAR, stringResource(R.string.view_year), Icons.Outlined.GridView),
                    Triple(CalendarViewMode.MONTH, stringResource(R.string.view_month), Icons.Outlined.CalendarMonth),
                    Triple(CalendarViewMode.WEEK, stringResource(R.string.view_week), Icons.Outlined.ViewWeek),
                    Triple(CalendarViewMode.DAY, stringResource(R.string.view_day), Icons.Outlined.CalendarToday)
                ).forEach { (mode, label, icon) ->
                    val isSelected = currentMode == mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { onModeSelect(mode) }
                    ) {
                        Icon(icon, label, tint = if (isSelected) Color.Black else Color.Gray, modifier = Modifier.size(24.dp))
                        Text(
                            label,
                            fontSize = 10.sp,
                            color = if (isSelected) Color.Black else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
