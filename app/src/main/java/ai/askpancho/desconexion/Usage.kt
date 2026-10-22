package ai.askpancho.desconexion

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

object Usage {
    private const val RESUMED = 1
    private const val PAUSED = 2
    private const val SCREEN_ON = 15
    private const val SCREEN_OFF = 16
    private const val KEYGUARD_HIDDEN = 18
    private const val SHUTDOWN = 26

    class Day(val start: Long, val total: Long, val apps: Map<String, Long>, val unlocks: Int)

    fun startOfDay(daysAgo: Int = 0): Long = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        timeInMillis
    }

    fun today() = startOfDay()

    fun granted(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        else @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Foreground time per app between start and end, from the system's own activity log.
     * An app counts while at least one of its activities is resumed and the screen is on.
     */
    fun day(c: Context, start: Long, end: Long = System.currentTimeMillis()): Day {
        val events = c.getSystemService(UsageStatsManager::class.java).queryEvents(start, end)
        val e = UsageEvents.Event()
        val since = HashMap<String, Long>()
        val live = HashMap<String, MutableSet<String>>()
        val seen = HashSet<String>()
        val apps = HashMap<String, Long>()
        var unlocks = 0
        var screenKnown = false
        fun add(p: String, from: Long, to: Long) { apps[p] = (apps[p] ?: 0L) + (to - from) }
        fun stopAll(at: Long) {
            since.forEach { (p, from) -> add(p, from, at) }
            since.clear()
            live.clear()
        }
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val p = e.packageName ?: continue
            when (e.eventType) {
                RESUMED -> {
                    val set = live.getOrPut(p) { HashSet() }
                    if (set.isEmpty() && p !in since) since[p] = e.timeStamp
                    set += e.className.orEmpty()
                    seen += p
                }
                PAUSED -> {
                    val set = live[p]
                    if (set != null && set.remove(e.className.orEmpty())) {
                        if (set.isEmpty()) since.remove(p)?.let { add(p, it, e.timeStamp) }
                    } else if (!screenKnown && seen.add(p)) {
                        // Open since before the window started, with the screen on all along.
                        add(p, start, e.timeStamp)
                    }
                }
                KEYGUARD_HIDDEN -> unlocks++
                SCREEN_ON -> screenKnown = true
                SCREEN_OFF, SHUTDOWN -> { screenKnown = true; stopAll(e.timeStamp) }
            }
        }
        stopAll(end)
        apps.keys.removeAll(Rules.uncounted(c))
        return Day(start, apps.values.sum().coerceAtMost(end - start), apps, unlocks)
    }

    fun used(c: Context, pkg: String) = day(c, today()).apps[pkg] ?: 0L
}
