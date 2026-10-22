package ai.askpancho.desconexion

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.provider.AlarmClock
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

/** Packages that are never blocked: the plumbing a phone needs to stay usable. */
object Rules {
    private val INFRA = setOf(
        "android", "com.android.systemui", "com.android.intentresolver",
        "com.android.permissioncontroller", "com.google.android.permissioncontroller",
        "com.android.packageinstaller", "com.google.android.packageinstaller",
        "com.android.server.telecom", "com.android.phone", "com.android.incallui",
        "com.samsung.android.incallui", "com.google.android.gms", "com.google.android.gsf",
        "com.android.documentsui", "com.google.android.documentsui",
        "com.android.providers.media.module", "com.google.android.providers.media.module",
        "com.android.emergency", "com.google.android.apps.safetyhub", "com.android.stk",
        "com.android.credentialmanager", "com.android.captiveportallogin",
        "com.google.android.captiveportallogin", "com.android.cellbroadcastreceiver",
        "com.google.android.cellbroadcastreceiver",
        // Phone settings and maintenance tools (OPPO, Samsung, Xiaomi)
        "com.android.settings.intelligence", "com.coloros.phonemanager", "com.oplus.phonemanager",
        "com.coloros.safecenter", "com.oplus.safecenter", "com.oplus.battery", "com.coloros.oppoguardelf",
        "com.oplus.wirelesssettings", "com.coloros.wirelesssettings", "com.oplus.notificationmanager",
        "com.coloros.notificationmanager", "com.oplus.securitypermission", "com.coloros.securitypermission",
        "com.samsung.android.lool", "com.samsung.android.sm_cn", "com.miui.securitycenter",
    )

    /** Every package that answers a system settings screen is part of "settings". */
    private val SETTINGS_ACTIONS = listOf(
        Settings.ACTION_SETTINGS, Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_BLUETOOTH_SETTINGS,
        Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_DISPLAY_SETTINGS, Settings.ACTION_SOUND_SETTINGS,
        Settings.ACTION_BATTERY_SAVER_SETTINGS, Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        Settings.ACTION_SECURITY_SETTINGS, Settings.ACTION_APPLICATION_SETTINGS,
        Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, Settings.ACTION_DEVICE_INFO_SETTINGS,
        Settings.ACTION_DATE_SETTINGS, Settings.ACTION_LOCALE_SETTINGS, Settings.ACTION_ACCESSIBILITY_SETTINGS,
    )

    @Volatile private var cache: Set<String>? = null
    @Volatile private var uncountedCache: Set<String>? = null

    fun exempt(c: Context): Set<String> = cache ?: refresh(c)

    fun refresh(c: Context): Set<String> {
        val pm = c.packageManager
        val s = HashSet(INFRA)
        s += c.packageName
        c.getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.forEach { s += it.packageName }
        c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let { s += it }
        recents()?.let { s += it }
        for (action in SETTINGS_ACTIONS + AlarmClock.ACTION_SHOW_ALARMS + AlarmClock.ACTION_SET_ALARM)
            pm.queryIntentActivities(Intent(action), 0).forEach { s += it.activityInfo.packageName }
        cache = s
        uncountedCache = null
        return s
    }

    /** The launcher that draws the recents screen, even when it is not the home app. */
    private fun recents(): String? {
        val res = Resources.getSystem()
        val id = res.getIdentifier("config_recentsComponentName", "string", "android")
        if (id == 0) return null
        return ComponentName.unflattenFromString(res.getString(id))?.packageName
    }

    /** Not counted as phone use: launchers and system chrome. */
    fun uncounted(c: Context): Set<String> = uncountedCache ?: run {
        val settings = Sys.settingsPackage(c)
        val home = c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.filter { it != settings }
        (home.toHashSet() + setOf(c.packageName, "android", "com.android.systemui")).also { uncountedCache = it }
    }
}
