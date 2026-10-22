package ai.askpancho.desconexion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

/** One permission per screen: what it is for, where to tap, and a single button. */
class SetupActivity : Activity() {
    companion object {
        private const val ALL = "all"

        fun start(c: Context, all: Boolean = false) =
            c.startActivity(Intent(c, SetupActivity::class.java).putExtra(ALL, all))
    }

    private lateinit var count: TextView
    private lateinit var title: TextView
    private lateinit var why: TextView
    private lateinit var how: TextView
    private lateinit var info: TextView
    private lateinit var skip: TextView
    private lateinit var go: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        count = label("", 14f, GRAY)
        title = label("", 34f, WHITE)
        why = label("", 18f, GRAY).apply { setPadding(0, dp(14), 0, 0); setLineSpacing(0f, 1.2f) }
        how = label("", 16f, WHITE).apply { setPadding(0, dp(28), 0, 0); setLineSpacing(0f, 1.2f) }
        info = label("información de la app", 16f, GRAY).apply {
            setPadding(0, dp(14), dp(14), dp(14))
            setOnClickListener { Setup.appInfo(this@SetupActivity) }
        }
        skip = label("ahora no", 20f, GRAY).apply { setPadding(0, dp(14), dp(24), dp(14)) }
        go = label("activar", 22f, WHITE).apply { setPadding(dp(24), dp(14), 0, dp(14)) }

        val bottom = LinearLayout(this).apply {
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
        col.addView(info)
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
        title.text = step.title
        why.text = step.why
        how.text = step.how
        info.visibility = if (step.id == "watch") View.VISIBLE else View.GONE
        skip.visibility = if (step.optional) View.VISIBLE else View.INVISIBLE
        skip.setOnClickListener { Store.skip(step.id); show() }
        go.setOnClickListener { step.go(this) }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) = show()
}
