package ai.askpancho.desconexion

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/** Each app: blocked (default), free, or free up to some minutes a day. */
class RulesActivity : Activity() {
    private companion object {
        const val LIMIT = Int.MAX_VALUE
        const val STEP = 5
        const val MAX = 240
    }

    private lateinit var col: LinearLayout
    private var openEditor: LinearLayout? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        col = column()
        screen(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) })
        Bg.run {
            val apps = Apps.all(this)
            runOnUiThread { for (a in apps) col.addView(item(a)) }
        }
    }

    /** Only what differs from "the rest" is spelled out. */
    private fun state(pkg: String): String {
        val r = Store.rule(pkg)
        val base = when {
            r == Store.CHALLENGE -> if (Store.restFree) "bloqueada" else ""
            r > 0 -> Fmt.dur(r * 60_000L) + " al día"
            else -> if (Store.restFree) "" else "libre"
        }
        return listOf(base, if (Store.onHome(pkg)) "inicio" else "").filter { it.isNotEmpty() }.joinToString(" · ")
    }

    private fun color(pkg: String) = if (Store.isAllowed(pkg)) WHITE else GRAY

    private fun item(a: Apps.App): View {
        val editor = column().apply { visibility = View.GONE; setPadding(0, 0, 0, dp(14)) }
        lateinit var head: LinearLayout

        fun fill() {
            head.setRow(a.label, state(a.pkg), color(a.pkg))
            editor.removeAllViews()
            val r = Store.rule(a.pkg)
            editor.addView(LinearLayout(this).apply {
                for ((name, rule) in listOf("bloqueada" to Store.CHALLENGE, "libre" to Store.FREE, "límite" to LIMIT)) {
                    val on = if (rule == LIMIT) r > 0 else r == rule
                    addView(label(name, 17f, if (on) WHITE else DIM).apply {
                        setPadding(0, dp(8), dp(24), dp(8))
                        setOnClickListener {
                            Store.setRule(a.pkg, if (rule != LIMIT) rule else if (r > 0) r else 30)
                            fill()
                        }
                    })
                }
            })
            if (r != Store.CHALLENGE) editor.addView(LinearLayout(this).apply {
                val on = Store.onHome(a.pkg)
                addView(label("en inicio", 17f, if (on) WHITE else DIM).apply {
                    setPadding(0, dp(8), dp(24), dp(8))
                    setOnClickListener { Store.setOnHome(a.pkg, !on); fill() }
                })
            })
            if (r > 0) editor.addView(LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(step("−") { Store.setRule(a.pkg, (r - STEP).coerceAtLeast(STEP)); fill() })
                addView(label(Fmt.dur(r * 60_000L), 22f, WHITE).apply { gravity = Gravity.CENTER; minWidth = dp(88) })
                addView(step("+") { Store.setRule(a.pkg, (r + STEP).coerceAtMost(MAX)); fill() })
            })
        }

        head = row(a.label, state(a.pkg), color(a.pkg)) {
            val show = editor.visibility != View.VISIBLE
            openEditor?.visibility = View.GONE
            if (show) fill()
            editor.visibility = if (show) View.VISIBLE else View.GONE
            openEditor = if (show) editor else null
        }
        return column().apply { addView(head); addView(editor) }
    }

    private fun step(s: String, f: () -> Unit) = label(s, 28f, GRAY).apply {
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(2), dp(14), dp(2))
        tap { f() }
    }
}
