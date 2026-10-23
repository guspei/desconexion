package ai.askpancho.desconexion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText

/** Free text, one entry per line: blocked sites or things to check. */
class ListActivity : Activity() {
    companion object {
        private const val WHAT = "w"
        const val SITES = 0
        const val CHECKS = 1

        fun start(c: Context, what: Int) = c.startActivity(Intent(c, ListActivity::class.java).putExtra(WHAT, what))
    }

    private lateinit var edit: EditText
    private var what = SITES

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        what = intent.getIntExtra(WHAT, SITES)
        edit = EditText(this).apply {
            background = null
            gravity = Gravity.TOP
            textSize = 22f
            typeface = LIGHT
            setTextColor(WHITE)
            setHintTextColor(DIM)
            if (what == SITES) {
                hint = "youtube.com\ninstagram.com 10m"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                setText(Store.sites.joinToString("\n") { s -> Store.limit("web:$s").let { if (it > 0) "$s ${it}m" else s } })
            } else {
                hint = "puerta\ngas"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setText(Store.checks.joinToString("\n"))
            }
            setSelection(text.length)
        }
        screen(edit)
        edit.requestFocus()
    }

    override fun onPause() {
        super.onPause()
        val lines = edit.text.lines()
        if (what == SITES) {
            val parsed = lines.mapNotNull(Sites::parse).distinctBy { it.first }
            Store.sites = parsed.map { it.first }
            for ((site, min) in parsed) Store.setLimit("web:$site", min)
        } else {
            Store.checks = lines.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
            Checks.refresh(this)
        }
    }
}
