package ai.askpancho.desconexion

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView

/** Every app. Free ones open; the rest ask for a sum first. */
class AppsActivity : Activity() {
    private lateinit var col: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        col = column()
        screen(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) })
        Bg.run {
            val apps = Apps.all(this)
            val exempt = Rules.exempt(this)
            runOnUiThread {
                for (a in apps) {
                    val open = Store.isAllowed(a.pkg) || a.pkg in exempt
                    col.addView(label(a.label, 24f, if (open) WHITE else GRAY).apply {
                        setPadding(0, dp(10), 0, dp(10))
                        tap {
                            Apps.open(this@AppsActivity, a.pkg)
                            finish()
                        }
                    }, LinearLayout.LayoutParams(-1, -2))
                }
            }
        }
    }
}
