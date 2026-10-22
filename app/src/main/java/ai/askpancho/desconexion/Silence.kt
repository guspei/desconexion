package ai.askpancho.desconexion

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Dismisses notifications from apps that are not free, so they cannot pull you back in. */
class Silence : NotificationListenerService() {
    companion object {
        var instance: Silence? = null
            private set

        /** Locked: priority-only do not disturb (the user's own exceptions still ring). Unlocked: back to normal. */
        fun quiet(on: Boolean) {
            val s = instance ?: return
            if (on) {
                if (!Store.quiet || s.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) return
                s.requestInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                Store.quietOurs = true
            } else if (Store.quietOurs) {
                s.requestInterruptionFilter(INTERRUPTION_FILTER_ALL)
                Store.quietOurs = false
            }
        }
    }

    override fun onListenerConnected() {
        instance = this
        if (Store.mute) activeNotifications?.forEach(::check)
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (Store.mute) check(sbn)
    }

    private fun check(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (!sbn.isClearable || Store.isAllowed(pkg) || pkg in Rules.exempt(this)) return
        when (sbn.notification.category) {
            Notification.CATEGORY_CALL, Notification.CATEGORY_ALARM -> return
        }
        cancelNotification(sbn.key)
    }
}
