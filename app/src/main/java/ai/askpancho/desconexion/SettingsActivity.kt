package ai.askpancho.desconexion

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.ScrollView

class SettingsActivity : Activity() {
    private lateinit var col: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        col = column()
        screen(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun yes(v: Boolean) = if (v) "sí" else "no"

    private fun render() {
        col.removeAllViews()
        if (Setup.pending(this, all = true).isNotEmpty()) col.addView(row("permisos", "", WARN) {
            SetupActivity.start(this, all = true)
        })

        col.addView(row("apps", "${Store.home().size}") {
            startActivity(Intent(this, RulesActivity::class.java))
        })
        col.addView(row("el resto", if (Store.restFree) "libre" else "bloqueado") {
            Store.restFree = !Store.restFree
            it.setRow("el resto", if (Store.restFree) "libre" else "bloqueado", WHITE)
        })
        col.addView(row("webs", "${Store.sites.size}") { ListActivity.start(this, ListActivity.SITES) })
        col.addView(row("comprobaciones", "${Store.checks.size}") { ListActivity.start(this, ListActivity.CHECKS) })
        col.addView(row("duran", "${Store.checkSpan}m") {
            val spans = listOf(15, 30, 45, 60)
            Store.checkSpan = spans[(spans.indexOf(Store.checkSpan) + 1) % spans.size]
            it.setRow("duran", "${Store.checkSpan}m", WHITE)
            Checks.refresh(this)
        })

        col.addView(row("silencio al bloquear", yes(Store.quiet)) {
            Store.quiet = !Store.quiet
            it.setRow("silencio al bloquear", yes(Store.quiet), WHITE)
        })
        // Without the adb-only permission, grayscale lives in the system's accessibility screen.
        val secure = Sys.canWriteSecure(this)
        col.addView(row("escala de grises", if (secure) yes(Store.gray) else "") {
            if (!secure) return@row Sys.openSettings(this, Settings.ACTION_ACCESSIBILITY_SETTINGS)
            Store.gray = !Store.gray
            Sys.setGray(this, Store.gray)
            it.setRow("escala de grises", yes(Store.gray), WHITE)
        })
        col.addView(row("silenciar el resto", yes(Store.mute)) {
            Store.mute = !Store.mute
            render()
        })
    }
}
