package ai.askpancho.desconexion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

/** One permission per screen: what it is for, each tap in order, and a single button. */
class SetupActivity : Activity() {
    companion object {
        private const val ALL = "all"

        fun start(c: Context, all: Boolean = false) =
            c.startActivity(Intent(c, SetupActivity::class.java).putExtra(ALL, all))
    }

    private lateinit var count: TextView
    private lateinit var title: TextView
    private lateinit var why: TextView
    private lateinit var how: LinearLayout
    private lateinit var skip: TextView
    private lateinit var go: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        count = label("", 14f, GRAY)
        title = label("", 34f, WHITE)
        why = label("", 18f, GRAY).apply { setPadding(0, dp(14), 0, 0); setLineSpacing(0f, 1.2f) }
        how = column().apply { setPadding(0, dp(20), 0, 0) }
        skip = label(getString(R.string.setup_skip), 20f, GRAY).apply { setPadding(0, dp(14), dp(24), dp(14)) }
        go = button(getString(R.string.setup_go))

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(skip)
            addView(Space(context), LinearLayout.LayoutParams(0, 1, 1f))
            addView(go)
        }
        val col = column()
        col.addView(count)
        col.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))
        col.addView(title)
        col.addView(why)
        col.addView(how)
        col.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))
        col.addView(bottom, LinearLayout.LayoutParams(-1, -2))
        screen(col)
    }

    override fun onResume() {
        super.onResume()
        show()
    }

    private fun show() {
        val step = Setup.pending(this, intent.getBooleanExtra(ALL, false)).firstOrNull() ?: return finish()
        count.text = "${Setup.steps.indexOf(step) + 1}/${Setup.steps.size}"
        title.setText(step.title)
        why.setText(step.why)
        how.removeAllViews()
        taps(step.how(this).map(::line))
        // Back without it on: likely Android's block on apps installed outside the store.
        if (step.id == "watch" && step.id in Setup.tried) {
            how.addView(label(getString(R.string.watch_stuck), 18f, GRAY).apply { setPadding(0, dp(28), 0, dp(4)) })
            val info = button(Setup.appInfoLabel(this), 18f).apply { setOnClickListener { Setup.appInfo(this@SetupActivity) } }
            taps(listOf(info) + Setup.unblock(this).map(::line))
        }
        skip.visibility = if (step.optional) View.VISIBLE else View.INVISIBLE
        skip.setOnClickListener { Store.skip(step.id); show() }
        go.setOnClickListener { Setup.tried += step.id; step.go(this) }
    }

    private fun line(s: String) = label(s, 20f, WHITE)

    /** Numbered when there is more than one, so it reads like the screens that follow. */
    private fun taps(views: List<View>) = views.forEachIndexed { i, v ->
        how.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, dp(8))
            if (views.size > 1) addView(label("${i + 1}", 18f, GRAY), LinearLayout.LayoutParams(dp(32), -2))
            addView(v)
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) = show()
}
