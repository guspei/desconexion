package ai.askpancho.desconexion

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat

/**
 * "I closed the door at 12:41." A check holds for a while and then clears itself.
 * While any holds, a silent notification shows it on the lock screen, so there is no need to unlock.
 */
object Checks {
    private const val CHANNEL = "checks"
    private const val ID = 1

    private fun span() = Store.checkSpan * 60_000L

    /** When the item was checked, or null if it is not (or no longer) checked. */
    fun at(item: String): Long? =
        Store.checkedAt(item).takeIf { it > 0 && System.currentTimeMillis() - it < span() }

    fun active() = Store.checks.mapNotNull { i -> at(i)?.let { i to it } }

    fun nextExpiry() = active().minOfOrNull { it.second }?.plus(span())

    /** Something was checked at some point and may still need clearing. */
    fun pending() = Store.checks.any { Store.checkedAt(it) > 0 }

    fun mark(c: Context, item: String) {
        Store.setChecked(item, System.currentTimeMillis())
        refresh(c)
    }

    fun time(c: Context, at: Long): String = DateFormat.getTimeFormat(c).format(at)

    fun refresh(c: Context) {
        for (i in Store.checks) if (Store.checkedAt(i) > 0 && at(i) == null) Store.setChecked(i, 0)
        val nm = c.getSystemService(NotificationManager::class.java)
        val alarms = c.getSystemService(AlarmManager::class.java)
        val tick = PendingIntent.getBroadcast(
            c, 0, Intent(c, ChecksReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val a = active()
        if (a.isEmpty()) {
            nm.cancel(ID)
            alarms.cancel(tick)
            return
        }
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "comprobaciones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
        val first = a.minOf { it.second }
        val last = a.maxOf { it.second }
        val stamp = time(c, first) + if (last - first >= 60_000) "–" + time(c, last) else ""
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, ChecksActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(a.joinToString(" · ") { it.first })
            .setContentText("✓ $stamp")
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS)
            .setTimeoutAfter(last + span() - System.currentTimeMillis())
            .build()
        try {
            nm.notify(ID, n)
        } catch (_: SecurityException) {
        }
        // Non-waking: drops the first expired item next time the phone is awake anyway.
        alarms.setWindow(AlarmManager.RTC, first + span(), 60_000, tick)
    }
}

class ChecksReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) = Checks.refresh(c)
}
