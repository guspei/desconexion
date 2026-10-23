package ai.askpancho.desconexion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import android.window.OnBackInvokedDispatcher

/**
 * The wall in front of a blocked app or site: a sum to solve before going in.
 * Past a daily limit, the way in is three sums for ten more minutes.
 */
class BlockActivity : Activity() {
    companion object {
        private const val KEY = "k"
        private const val MODE = "m"
        private const val LAUNCH = "l"
        const val CHALLENGE = 0
        const val LIMIT = 1
        const val WEB = 2
        const val SETTINGS = 3
        private const val EXTRA_MINUTES = 10

        /** On screen right now; the watcher does not stack a second one. */
        var showing = false
            private set
        private const val EXTRA_ROUNDS = 3

        /** [launch]: opened from the launcher, so success opens the app and giving up just closes. */
        fun start(c: Context, key: String, mode: Int, launch: Boolean = false) {
            c.startActivity(
                Intent(c, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    // Not the user leaving the app below, so it does not shrink into a floating video.
                    .addFlags(if (launch) 0 else Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    .putExtra(KEY, key).putExtra(MODE, mode).putExtra(LAUNCH, launch)
            )
        }
    }

    private val h = Handler(Looper.getMainLooper())
    private val typed = StringBuilder()
    private var key = ""
    private var mode = CHALLENGE
    private var launch = false
    private var answer = ""
    private var rounds = 1
    private var solved = 0
    private var busy = false
    private var decided = false
    private var away = false

    private lateinit var title: TextView
    private lateinit var sub: TextView
    private lateinit var problem: TextView
    private lateinit var input: TextView
    private lateinit var more: TextView
    private lateinit var pad: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        title = label("", 20f, GRAY)
        sub = label("", 15f, GRAY).apply { setPadding(0, dp(4), 0, 0) }
        problem = label("", 36f, WHITE).apply { gravity = Gravity.CENTER }
        input = label("", 36f, WHITE).apply { gravity = Gravity.CENTER; minHeight = dp(64) }
        more = label("+${EXTRA_MINUTES}m", 22f, GRAY).apply {
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(14), dp(28), dp(14))
            tap { extend() }
        }
        pad = keypad()

        val col = column()
        col.addView(title)
        col.addView(sub)
        col.addView(Space(this), LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(problem, LinearLayout.LayoutParams(-1, -2))
        col.addView(input, LinearLayout.LayoutParams(-1, -2))
        col.addView(more, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL })
        col.addView(Space(this), LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(pad, LinearLayout.LayoutParams(-1, -2))
        screen(col)

        if (Build.VERSION.SDK_INT >= 33)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) { giveUp() }
        read(intent)
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        if (i.getStringExtra(KEY) != key || i.getIntExtra(MODE, CHALLENGE) != mode) read(i)
    }

    /**
     * The sum is only visible while this screen has focus. A calculator floated on top
     * (side panel, split screen) finds it hidden, and coming back brings a new one.
     */
    override fun onWindowFocusChanged(focus: Boolean) {
        super.onWindowFocusChanged(focus)
        if (pad.visibility != View.VISIBLE) return
        if (!focus) {
            problem.visibility = View.INVISIBLE
            input.visibility = View.INVISIBLE
            away = true
        } else if (away) {
            away = false
            problem.visibility = View.VISIBLE
            input.visibility = View.VISIBLE
            next()
        }
    }

    private fun read(i: Intent) {
        key = i.getStringExtra(KEY) ?: return finish()
        mode = i.getIntExtra(MODE, CHALLENGE)
        launch = i.getBooleanExtra(LAUNCH, false)
        title.text = when (mode) {
            WEB -> key.removePrefix("web:")
            SETTINGS -> key
            else -> Apps.label(this, key) ?: key
        }
        if (mode == LIMIT) {
            sub.text = ""
            problem.text = Fmt.dur((Store.limit(key) + Store.extension(key)) * 60_000L)
            input.text = ""
            more.visibility = View.VISIBLE
            pad.visibility = View.INVISIBLE
        } else {
            rounds = 1
            begin()
        }
    }

    private fun extend() {
        rounds = EXTRA_ROUNDS
        begin()
    }

    private fun begin() {
        solved = 0
        more.visibility = View.GONE
        pad.visibility = View.VISIBLE
        next()
    }

    private fun next() {
        val (q, a) = Challenge.next()
        problem.text = q
        answer = a.toString()
        typed.clear()
        input.text = ""
        input.setTextColor(WHITE)
        sub.text = if (rounds > 1) "${solved + 1}/$rounds" else ""
    }

    private fun press(k: Char) {
        if (busy) return
        if (k == '<') {
            if (typed.isNotEmpty()) typed.setLength(typed.length - 1)
        } else typed.append(k)
        input.text = typed
        if (typed.length < answer.length) return
        if (typed.toString() == answer) {
            if (++solved >= rounds) success() else next()
        } else {
            busy = true
            input.setTextColor(WARN)
            h.postDelayed({ busy = false; next() }, 700)
        }
    }

    private fun success() {
        decided = true
        when (mode) {
            CHALLENGE -> {
                Grants.give(key)
                if (launch) Apps.launch(this, key)
            }
            LIMIT -> {
                Store.extend(key, EXTRA_MINUTES)
                if (launch) Apps.launch(this, key)
            }
            WEB -> Grants.give(key)
            SETTINGS -> startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Watcher.instance?.reevaluate()
        finish()
    }

    private fun giveUp() {
        decided = true
        when {
            mode == WEB -> Watcher.instance?.leaveSite()
            !launch -> startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    @Deprecated("Below API 33 only")
    override fun onBackPressed() = giveUp()

    override fun onResume() {
        super.onResume()
        showing = true
    }

    override fun onPause() {
        showing = false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        // Pushed away without an answer (e.g. the app finished opening on top): look again.
        if (!decided) Watcher.instance?.reevaluate()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun keypad() = column().apply {
        for (line in listOf("123", "456", "789", " 0<")) {
            val r = LinearLayout(context)
            for (k in line) {
                val key = label(if (k == '<') "⌫" else k.toString(), 28f, WHITE).apply { gravity = Gravity.CENTER }
                if (k != ' ') key.tap {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    press(k)
                }
                r.addView(key, LinearLayout.LayoutParams(0, dp(68), 1f))
            }
            addView(r, LinearLayout.LayoutParams(-1, -2))
        }
    }
}
