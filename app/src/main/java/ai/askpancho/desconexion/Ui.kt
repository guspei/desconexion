package ai.askpancho.desconexion

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView

const val WHITE = 0xFFE6E6E6.toInt()
const val SOFT = 0xFFB0B0B0.toInt()
const val GRAY = 0xFF7A7A7A.toInt()
const val DIM = 0xFF3C3C3C.toInt()
const val WARN = 0xFFB35C5C.toInt()

val LIGHT: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)

fun Context.dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.label(s: CharSequence, sp: Float, color: Int) = TextView(this).apply {
    text = s
    textSize = sp
    typeface = LIGHT
    setTextColor(color)
}

fun ripple() = RippleDrawable(ColorStateList.valueOf(0x22FFFFFF), null, ColorDrawable(Color.WHITE))

fun <V : View> V.tap(f: (V) -> Unit): V = apply {
    background = ripple()
    setOnClickListener { f(this) }
}

fun Context.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

/** A row with a label on the left and a quieter value on the right. */
fun Context.row(name: String, value: String, nameColor: Int = WHITE, onTap: (LinearLayout) -> Unit): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(12), 0, dp(12))
        addView(label(name, 22f, nameColor), LinearLayout.LayoutParams(0, -2, 1f))
        addView(label(value, 18f, GRAY))
        tap(onTap)
    }

fun LinearLayout.setRow(name: String, value: String, nameColor: Int) {
    (getChildAt(0) as TextView).apply { text = name; setTextColor(nameColor) }
    (getChildAt(1) as TextView).text = value
}

/** Black, edge to edge, padded clear of the system bars and the keyboard. */
fun Activity.screen(root: View, side: Int = 32, vertical: Int = 28) {
    if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
    else @Suppress("DEPRECATION") {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }
    root.setBackgroundColor(Color.BLACK)
    root.setOnApplyWindowInsetsListener { v, ins ->
        val l: Int; val t: Int; val r: Int; val b: Int
        if (Build.VERSION.SDK_INT >= 30) {
            val bars = ins.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = ins.getInsets(WindowInsets.Type.ime())
            l = bars.left; t = bars.top; r = bars.right; b = maxOf(bars.bottom, ime.bottom)
        } else @Suppress("DEPRECATION") {
            l = ins.systemWindowInsetLeft; t = ins.systemWindowInsetTop
            r = ins.systemWindowInsetRight; b = ins.systemWindowInsetBottom
        }
        v.setPadding(dp(side) + l, dp(vertical) + t, dp(side) + r, dp(vertical) + b)
        ins
    }
    setContentView(root)
}
