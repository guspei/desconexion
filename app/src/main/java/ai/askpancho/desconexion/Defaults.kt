package ai.askpancho.desconexion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager

/**
 * First run: everything is free except the usual time sinks, as apps and as sites.
 * Phone, messages, camera, maps and mail go on the home screen; the screen goes black.
 */
object Defaults {
    private val BLOCKED = listOf(
        "com.google.android.youtube",
        "com.instagram.android", "com.instagram.lite",
        "com.facebook.katana", "com.facebook.lite",
        "com.zhiliaoapp.musically", "com.zhiliaoapp.musically.go", "com.ss.android.ugc.trill",
        "com.ninegag.android.app",
    )
    private val SITES = listOf("youtube.com", "instagram.com", "facebook.com", "tiktok.com", "9gag.com")

    fun apply(c: Context) {
        val pm = c.packageManager
        fun handler(i: Intent): String? {
            val def = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
            if (def != null && def != "android") return def
            return pm.queryIntentActivities(i, 0).firstOrNull()?.activityInfo?.packageName
        }
        val free = listOfNotNull(
            c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage,
            Telephony.Sms.getDefaultSmsPackage(c),
            handler(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)),
            handler(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=0"))),
            handler(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))),
        ).distinct().filter { pm.getLaunchIntentForPackage(it) != null }
        Store.restFree = true
        Store.sites = SITES
        Store.setup(free, BLOCKED)
        Bg.run { Sys.blackWallpaper(c.applicationContext) }
    }
}
