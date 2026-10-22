package ai.askpancho.desconexion

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Telephony
import android.telecom.TelecomManager

/** First run: phone, messages, camera, maps and mail stay free; the screen goes black. */
object Defaults {
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
        Store.setup(free)
        Bg.run { Sys.blackWallpaper(c.applicationContext) }
    }
}
