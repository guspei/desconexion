package ai.askpancho.desconexion

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Locale

/** Today in big numbers, where it went, and the last seven days. */
class StatsActivity : Activity() {
    private lateinit var col: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        col = column()
        screen(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) })
    }

    override fun onResume() {
        super.onResume()
        if (!Usage.granted(this)) {
            col.removeAllViews()
            col.addView(row("acceso a uso", "", WARN) {
                Sys.openSettings(this, android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            })
            return
        }
        Bg.run {
            val days = (0..6).map { i ->
                Usage.day(this, Usage.startOfDay(i), if (i == 0) System.currentTimeMillis() else Usage.startOfDay(i - 1))
            }
            runOnUiThread { render(days) }
        }
    }

    private fun render(days: List<Usage.Day>) {
        col.removeAllViews()
        val today = days[0]
        col.addView(label(Fmt.dur(today.total), 56f, WHITE))
        if (Build.VERSION.SDK_INT >= 28) col.addView(label("${today.unlocks} desbloqueos", 16f, GRAY))
        gap(36)

        today.apps.entries.sortedByDescending { it.value }.filter { it.value >= 60_000 }.take(10).forEach { (pkg, ms) ->
            col.addView(LinearLayout(this).apply {
                setPadding(0, dp(7), 0, dp(7))
                addView(label(Apps.label(this@StatsActivity, pkg) ?: pkg, 20f, WHITE), LinearLayout.LayoutParams(0, -2, 1f))
                addView(label(Fmt.dur(ms), 18f, GRAY))
            })
        }
        gap(44)

        val max = days.maxOf { it.total }.coerceAtLeast(1)
        val fmt = SimpleDateFormat("EEE", Locale.forLanguageTag("es"))
        days.forEachIndexed { i, d ->
            val name = if (i == 0) "hoy" else fmt.format(d.start).removeSuffix(".")
            val share = d.total.toFloat() / max
            col.addView(LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
                addView(label(name, 15f, GRAY), LinearLayout.LayoutParams(dp(44), -2))
                addView(View(context).apply { setBackgroundColor(if (i == 0) WHITE else DIM) },
                    LinearLayout.LayoutParams(0, dp(3), share * 0.75f))
                addView(View(context), LinearLayout.LayoutParams(0, 1, 1f - share * 0.75f))
                addView(label(Fmt.dur(d.total), 15f, GRAY).apply { setPadding(dp(12), 0, 0, 0) },
                    LinearLayout.LayoutParams(dp(76), -2))
            })
        }
    }

    private fun gap(h: Int) = col.addView(View(this), LinearLayout.LayoutParams(1, dp(h)))
}
