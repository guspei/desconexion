package ai.askpancho.desconexion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class HomeActivity : Activity() {
    private lateinit var time: TextView
    private lateinit var checks: TextView
    private lateinit var warn: TextView
    private lateinit var list: LinearLayout
    private val h = Handler(Looper.getMainLooper())

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (!Store.ready) Defaults.apply(this)

        time = label("", 44f, GRAY).apply {
            setOnClickListener { startActivity(Intent(this@HomeActivity, StatsActivity::class.java)) }
        }
        checks = label("", 14f, GRAY).apply {
            setPadding(0, dp(8), 0, 0)
            setOnClickListener { startActivity(Intent(this@HomeActivity, ChecksActivity::class.java)) }
        }
        warn = label("", 15f, WARN).apply { setPadding(0, dp(6), 0, 0) }
        val top = column().apply { addView(time); addView(checks); addView(warn) }
        list = column()
        val check = label("✓", 32f, SOFT).apply {
            setPadding(0, dp(10), dp(40), dp(10))
            setOnClickListener { startActivity(Intent(this@HomeActivity, ChecksActivity::class.java)) }
        }
        val more = label("•••", 22f, SOFT).apply {
            setPadding(dp(40), dp(18), 0, dp(18))
            setOnClickListener { startActivity(Intent(this@HomeActivity, AppsActivity::class.java)) }
            // Settings sit behind a long press here, where it is hard to hit by accident.
            setOnLongClickListener {
                BlockActivity.start(this@HomeActivity, "ajustes", BlockActivity.SETTINGS, launch = true)
                true
            }
        }

        val root = FrameLayout(this)
        root.addView(top, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        root.addView(list, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL or Gravity.START))
        root.addView(check, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START))
        root.addView(more, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.END))
        screen(root, vertical = 40)
    }

    override fun onResume() {
        super.onResume()
        Grants.clear()
        Sys.ensureWatcher(this)
        if (!Setup.shown && Setup.pending(this).isNotEmpty()) {
            Setup.shown = true
            SetupActivity.start(this)
        }
        render()
    }

    override fun onPause() {
        h.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun renderChecks() {
        h.removeCallbacksAndMessages(null)
        val a = Checks.active()
        checks.visibility = if (a.isEmpty()) View.GONE else View.VISIBLE
        checks.text = "✓ " + a.joinToString(" · ") { it.first }
        Checks.nextExpiry()?.let { h.postDelayed(::renderChecks, it - System.currentTimeMillis() + 500) }
    }

    private fun render() {
        renderChecks()
        list.removeAllViews()
        val limited = HashMap<String, TextView>()
        for (pkg in Store.home()) {
            val name = Apps.label(this, pkg) ?: continue
            val left = label("", 15f, GRAY).apply { setPadding(dp(14), 0, 0, 0) }
            val row = LinearLayout(this).apply {
                gravity = Gravity.BOTTOM
                setPadding(0, dp(9), 0, dp(9))
                addView(label(name, 30f, WHITE))
                addView(left)
                setOnClickListener { Apps.launch(this@HomeActivity, pkg) }
            }
            list.addView(row, LinearLayout.LayoutParams(-2, -2))
            if (Store.limit(pkg) > 0) limited[pkg] = left
        }

        val watching = Sys.watcherOn(this)
        val usage = Usage.granted(this)
        warn.visibility = if (watching && usage) View.GONE else View.VISIBLE
        warn.setText(if (!watching) R.string.home_off else R.string.home_no_usage)
        warn.setOnClickListener { SetupActivity.start(this) }
        if (!usage) {
            time.text = ""
            return
        }

        Bg.run {
            val d = Usage.day(this, Usage.today())
            runOnUiThread {
                time.text = Fmt.dur(d.total)
                for ((pkg, v) in limited) {
                    val left = (Store.limit(pkg) + Store.extension(pkg)) * 60_000L - (d.apps[pkg] ?: 0L)
                    v.text = Fmt.dur(left)
                }
            }
        }
    }
}
