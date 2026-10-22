package ai.askpancho.desconexion

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Reacts to app switches only (no polling). Window content is read solely while a browser
 * is in front and there are sites to block.
 */
class Watcher : AccessibilityService() {
    companion object {
        var instance: Watcher? = null
            private set
    }

    private val h = Handler(Looper.getMainLooper())
    private val activities = HashMap<String, Boolean>()
    private lateinit var keyguard: KeyguardManager
    private var fg: String? = null
    private var browsing = false
    private var urlPending = false
    private var lastBlock = ""
    private var quietUntil = 0L

    private val recheck = Runnable { evaluate() }
    private val urlCheck = Runnable { urlPending = false; checkUrl() }

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    h.removeCallbacks(recheck)
                    Silence.quiet(true)
                }
                Intent.ACTION_SCREEN_ON -> if (Checks.pending()) Checks.refresh(c)
                Intent.ACTION_USER_PRESENT -> {
                    Silence.quiet(false)
                    Rules.refresh(c)
                    // Back to the last app that was open: a pass that ran out while locked asks again.
                    h.postDelayed({ evaluate() }, 300)
                }
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
        keyguard = getSystemService(KeyguardManager::class.java)
        Rules.refresh(this)
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, f, RECEIVER_NOT_EXPORTED) else registerReceiver(screen, f)
    }

    override fun onDestroy() {
        instance = null
        h.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(screen)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (browsing && pkg == fg) checkUrlSoon()
            return
        }
        val cls = e.className?.toString() ?: return
        if (cls == BlockActivity::class.java.name || !isActivity(pkg, cls)) return
        // In case the unlock broadcast was missed: any app switch while unlocked ends the lock silence.
        if (Store.quietOurs && !keyguard.isKeyguardLocked) Silence.quiet(false)
        if (pkg != fg) lastBlock = ""
        fg = pkg
        setBrowsing(pkg in Sites.BROWSERS && Store.sites.isNotEmpty())
        evaluate()
    }

    /** Dialogs, popups and the notification shade also fire window events; only activities count. */
    private fun isActivity(pkg: String, cls: String): Boolean {
        val k = "$pkg/$cls"
        activities[k]?.let { return it }
        val r = try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0); true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
        if (activities.size > 500) activities.clear()
        activities[k] = r
        return r
    }

    private fun evaluate() {
        h.removeCallbacks(recheck)
        val pkg = fg ?: return
        if (pkg in Rules.exempt(this) || keyguard.isKeyguardLocked) return
        if (Store.isAllowed(pkg)) {
            val limit = Store.limit(pkg)
            if (limit > 0) {
                val left = (limit + Store.extension(pkg)) * 60_000L - Usage.used(this, pkg)
                if (left <= 0) return block(pkg, BlockActivity.LIMIT)
                h.postDelayed(recheck, left + 1000)
            }
        } else {
            val left = Grants.left(pkg)
            if (left <= 0) return block(pkg, BlockActivity.CHALLENGE)
            h.postDelayed(recheck, left + 500)
        }
        if (browsing) checkUrlSoon()
    }

    private fun block(key: String, mode: Int) {
        if (key == lastBlock && (BlockActivity.showing || SystemClock.elapsedRealtime() < quietUntil)) return
        lastBlock = key
        BlockActivity.start(this, key, mode)
    }

    /** Content events are only subscribed to while a browser is in front. */
    private fun setBrowsing(on: Boolean) {
        if (on == browsing) return
        val info = serviceInfo ?: return
        browsing = on
        info.eventTypes = if (on) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        else AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        serviceInfo = info
    }

    private fun checkUrlSoon() {
        if (urlPending) return
        urlPending = true
        h.postDelayed(urlCheck, 800)
    }

    private fun checkUrl() {
        val pkg = fg ?: return
        val id = Sites.BROWSERS[pkg] ?: return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val bar = root.findAccessibilityNodeInfosByViewId(id).firstOrNull() ?: return
        if (bar.isFocused) return
        val host = Sites.host(bar.text?.toString() ?: return) ?: return
        val site = Sites.match(host, Store.sites) ?: return
        if (Grants.left("web:$site") > 0) return
        block("web:$site", BlockActivity.WEB)
    }

    /** After a solved challenge: re-arm the timers for the app now in front. */
    fun reevaluate() {
        h.postDelayed({ evaluate() }, 400)
    }

    /** Giving up on a blocked site: step back out of it. */
    fun leaveSite() {
        quietUntil = SystemClock.elapsedRealtime() + 2500
        h.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, 250)
    }
}
