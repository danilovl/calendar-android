package danilovl.calendar.util

import android.content.Context
import android.content.Intent
import danilovl.calendar.R
import danilovl.calendar.data.local.CalendarEvent

object EventUtils {
    fun shareEvents(context: Context, events: List<CalendarEvent>) {
        if (events.isEmpty()) return
        val text = events.joinToString("\n\n") { e ->
            "${e.date}: ${e.title}${if (e.description.isNotEmpty()) "\n${e.description}" else ""}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share)))
    }
}
