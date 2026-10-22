package ai.askpancho.desconexion

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout

/** Hold an item for a second to check it. It cannot be unchecked: it clears itself. */
class ChecksActivity : Activity() {
    private lateinit var col: LinearLayout
    private val h = Handler(Looper.getMainLooper())

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        col = column().apply { gravity = Gravity.CENTER_VERTICAL }
        screen(col)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onPause() {
        h.removeCallbacksAndMessages(null)
        super.onPause()
    }

    private fun render() {
        h.removeCallbacksAndMessages(null)
        col.removeAllViews()
        for (item in Store.checks) col.addView(item(item, Checks.at(item)), LinearLayout.LayoutParams(-1, -2))
        Checks.nextExpiry()?.let { h.postDelayed(::render, it - System.currentTimeMillis() + 500) }
    }

    private fun item(name: String, at: Long?): View {
        val line = LinearLayout(this).apply {
            gravity = Gravity.BOTTOM
            addView(label(name, 28f, if (at != null) WHITE else GRAY), LinearLayout.LayoutParams(0, -2, 1f))
            addView(label(if (at != null) "✓ " + Checks.time(this@ChecksActivity, at) else "", 17f, WHITE))
        }
        val bar = View(this).apply {
            setBackgroundColor(WHITE)
            pivotX = 0f
            scaleX = 0f
        }
        return FrameLayout(this).apply {
            setPadding(0, dp(16), 0, dp(16))
            addView(line)
            addView(bar, FrameLayout.LayoutParams(-1, dp(2), Gravity.BOTTOM))
            if (at == null) setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> bar.animate().scaleX(1f).setDuration(900)
                        .setInterpolator(LinearInterpolator())
                        .withEndAction {
                            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            Checks.mark(this@ChecksActivity, name)
                            render()
                        }.start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        bar.animate().cancel()
                        bar.scaleX = 0f
                    }
                }
                true
            }
        }
    }
}
