package ai.askpancho.desconexion

import android.Manifest
import android.app.NotificationManager
import android.app.WallpaperManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.provider.Settings

/** System switches. Most need WRITE_SECURE_SETTINGS, granted once over adb. */
object Sys {
    fun canWriteSecure(c: Context) =
        c.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun setGray(c: Context, on: Boolean) {
        if (!canWriteSecure(c)) return
        val cr = c.contentResolver
        if (on) Settings.Secure.putInt(cr, "accessibility_display_daltonizer", 0)
        Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", if (on) 1 else 0)
    }

    private fun watcher(c: Context) = ComponentName(c, Watcher::class.java)

    fun watcherOn(c: Context): Boolean {
        val enabled = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = watcher(c)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    /** Turns the watcher back on if it was switched off. Disconnecting should take effort. */
    fun ensureWatcher(c: Context) {
        if (!canWriteSecure(c) || watcherOn(c)) return
        val cr = c.contentResolver
        val cur = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val me = watcher(c).flattenToString()
        Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, if (cur.isEmpty()) me else "$cur:$me")
        Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
    }

    fun listenerOn(c: Context): Boolean {
        val me = ComponentName(c, Silence::class.java)
        return if (Build.VERSION.SDK_INT >= 27)
            c.getSystemService(NotificationManager::class.java).isNotificationListenerAccessGranted(me)
        else Settings.Secure.getString(c.contentResolver, "enabled_notification_listeners")?.contains(me.flattenToString()) == true
    }

    fun isHome(c: Context) = c.packageManager
        .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName == c.packageName

    fun homeRequest(c: Context): Intent =
        if (Build.VERSION.SDK_INT >= 29) c.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)
        else Intent(Settings.ACTION_HOME_SETTINGS)

    fun settingsPackage(c: Context) =
        c.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName ?: "com.android.settings"

    /** Opens a system settings screen with a short pass, so the watcher lets it through. */
    fun openSettings(c: Context, action: String) {
        Grants.give(settingsPackage(c), 5 * 60_000L)
        c.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun blackWallpaper(c: Context) {
        val black = Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565).apply { eraseColor(Color.BLACK) }
        try {
            WallpaperManager.getInstance(c)
                .setBitmap(black, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        } catch (_: Exception) {
        }
    }
}
