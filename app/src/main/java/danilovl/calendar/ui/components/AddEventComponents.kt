package danilovl.calendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import danilovl.calendar.R
import danilovl.calendar.data.EventType
import danilovl.calendar.ui.theme.XiaomiBg
import danilovl.calendar.ui.theme.XiaomiBlue
import danilovl.calendar.ui.theme.XiaomiTextSecondary
import danilovl.calendar.ui.trans

@Composable
fun CategoryItem(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ).padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) XiaomiBlue else XiaomiBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else XiaomiTextSecondary
            )
        }
        Text(
            label,
            fontSize = 11.sp,
            color = if (isSelected) XiaomiBlue else XiaomiTextSecondary,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ReminderMelodyRow(melody: String, enabled: Boolean, onMelodyClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onMelodyClick() }.padding(10.dp)
    ) {
        Text(trans(R.string.settings_reminder_melody_title), modifier = Modifier.weight(1f), fontSize = 16.sp)
        Text(settingLabel(melody), color = XiaomiTextSecondary, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = enabled, 
            onCheckedChange = onToggle, 
            colors = SwitchDefaults.colors(checkedThumbColor = XiaomiBlue),
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent
)

@Composable
fun screenTitle(type: String, editing: Boolean): String {
    val base = when (type) {
        EventType.BIRTHDAY.value -> trans(R.string.type_birthday)
        EventType.ANNIVERSARY.value -> trans(R.string.type_anniversary)
        EventType.REMINDER.value -> trans(R.string.type_reminder)
        else -> trans(R.string.type_event)
    }
    val action = if (editing) trans(R.string.action_edit) else trans(R.string.action_add)

    return "$action $base"
}

