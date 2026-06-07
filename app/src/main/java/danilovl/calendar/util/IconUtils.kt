package danilovl.calendar.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

object IconUtils {
    private const val TAG = "IconUtils"
    private const val PREFS = "dynamic_icon_prefs"
    private const val KEY_CURRENT = "current_alias"
    private const val KEY_BOOTSTRAP_RETIRED = "bootstrap_retired"
    private const val KEY_LAST_CLEANUP = "last_cleanup_day_v11"

    private const val DAYS_IN_MONTH = 31
    private const val DAYS_IN_WEEK = 7

    fun applyIconForToday(context: Context, retireBootstrap: Boolean = false) {
        val pm = context.packageManager
        val pkg = context.packageName
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val now = LocalDate.now()
        val targetAliasName = aliasName(pkg, now.dayOfMonth, now.dayOfWeek.value)
        val target = ComponentName(pkg, targetAliasName)
        val previousAliasName = prefs.getString(KEY_CURRENT, null)

        val alreadyCorrect = previousAliasName == targetAliasName &&
                safeState(pm, target) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        if (alreadyCorrect && (!retireBootstrap || prefs.getBoolean(KEY_BOOTSTRAP_RETIRED, false))) {
            return
        }

        try {
            setState(pm, target, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)

            if (previousAliasName != null && previousAliasName != targetAliasName) {
                setState(
                    pm,
                    ComponentName(pkg, previousAliasName),
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                )
            }

            if (retireBootstrap) {
                retireBootstrapLaunchersOnce(pm, pkg, prefs, targetAliasName)
            }

            prefs.edit().putString(KEY_CURRENT, targetAliasName).apply()

            val lastCleanup = prefs.getInt(KEY_LAST_CLEANUP, -1)
            if (lastCleanup != now.dayOfMonth) {
                cleanupStaleAliases(pm, pkg, targetAliasName)
                prefs.edit().putInt(KEY_LAST_CLEANUP, now.dayOfMonth).apply()
            }
        } catch (e: Exception) {
            AppLog.e(TAG, "applyIconForToday failed", e)
        }
    }

    private fun retireBootstrapLaunchersOnce(
        pm: PackageManager,
        pkg: String,
        prefs: android.content.SharedPreferences,
        targetAliasName: String
    ) {
        if (prefs.getBoolean(KEY_BOOTSTRAP_RETIRED, false)) return

        val bootstrapNames = listOf(
            "$pkg.CalendarLauncher",
            "$pkg.Launcher",
            "$pkg.AppLauncher"
        )
        for (name in bootstrapNames) {
            if (name == targetAliasName) continue
            setState(pm, ComponentName(pkg, name), PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        }
        prefs.edit().putBoolean(KEY_BOOTSTRAP_RETIRED, true).apply()
    }


    private fun cleanupStaleAliases(
        pm: PackageManager,
        pkg: String,
        excludeAliasName: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (d in 1..DAYS_IN_MONTH) {
                    for (w in 1..DAYS_IN_WEEK) {
                        val name = aliasName(pkg, d, w)
                        if (name == excludeAliasName) {
                            continue
                        }

                        val comp = ComponentName(pkg, name)
                        if (safeState(pm, comp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                            setState(pm, comp, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Cleanup failed", e)
            }
        }
    }

    private fun aliasName(pkg: String, dayOfMonth: Int, dayOfWeek: Int): String =
        "$pkg.MainActivity_D${dayOfMonth}_W${dayOfWeek}"

    private fun safeState(pm: PackageManager, comp: ComponentName): Int =
        try {
            pm.getComponentEnabledSetting(comp)
        } catch (e: Exception) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }

    private fun setState(pm: PackageManager, comp: ComponentName, state: Int) {
        try {
            if (pm.getComponentEnabledSetting(comp) != state) {
                pm.setComponentEnabledSetting(comp, state, PackageManager.DONT_KILL_APP)
            }
        } catch (e: Exception) {
            AppLog.w(TAG, "Failed to set state for ${comp.className}", e)
        }
    }
}
