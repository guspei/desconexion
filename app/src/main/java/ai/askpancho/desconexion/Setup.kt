package ai.askpancho.desconexion

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

/** What the app needs from the system, in the order a new install asks for it. */
object Setup {
    class Step(
        val id: String,
        val title: Int,
        val why: Int,
        /** One tap per line, named as the phone shows it. */
        val how: (Context) -> List<String>,
        val optional: Boolean,
        val done: (Context) -> Boolean,
        val go: (Activity) -> Unit,
    )

    /** Shown once per launch, so backing out of the guide does not trap you in it. */
    var shown = false

    /** Steps whose settings screen was already opened, so the guide can offer the way past a block. */
    val tried = mutableSetOf<String>()

    val steps = listOf(
        Step(
            "home", R.string.home_title, R.string.home_why,
            { listOf(it.getString(R.string.home_how)) },
            false, Sys::isHome,
        ) { it.startActivityForResult(Sys.homeRequest(it), 1) },
        Step(
            "watch", R.string.watch_title, R.string.watch_why,
            { listOf(said(it, "user_installed_services_category_title", R.string.watch_downloaded), "desconexión", it.getString(R.string.setup_switch)) },
            false, Sys::watcherOn,
        ) { a ->
            // Stock Android scrolls to the service and highlights it; other skins ignore the hint.
            val me = ComponentName(a, Watcher::class.java).flattenToString()
            open(
                a, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .putExtra(":settings:fragment_args_key", me)
                    .putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", me) })
            )
        },
        Step(
            "usage", R.string.usage_title, R.string.usage_why,
            { listOf("desconexión", it.getString(R.string.setup_switch)) },
            false, Usage::granted,
        ) { open(it, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        Step(
            "listen", R.string.listen_title, R.string.listen_why,
            { listOf(it.getString(R.string.setup_switch)) },
            true, Sys::listenerOn,
        ) { a ->
            val me = ComponentName(a, Silence::class.java).flattenToString()
            open(
                a, if (Build.VERSION.SDK_INT >= 30)
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, me)
                else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            )
        },
        Step(
            "post", R.string.post_title, R.string.post_why,
            { listOf(it.getString(R.string.post_how)) },
            true, ::canPost,
        ) { a ->
            // The system dialog only appears twice; after that, the app's notification page.
            if (a.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) || !Store.skipped("postAsked")) {
                Store.skip("postAsked")
                a.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
            } else open(a, Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.packageName))
        },
    )

    private fun canPost(c: Context) = Build.VERSION.SDK_INT < 33 ||
        c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun pending(c: Context, all: Boolean = false) =
        steps.filter { !it.done(c) && (all || !(it.optional && Store.skipped(it.id))) }

    /** After app info: what Android asks before it lets an app installed outside the store use accessibility. */
    fun unblock(c: Context) =
        listOf("⋮", said(c, "app_restricted_settings_lockscreen_title", R.string.watch_restricted), c.getString(R.string.watch_back))

    fun appInfoLabel(c: Context) = said(c, "application_info_label", R.string.watch_app_info)

    /** A label exactly as the phone's Settings shows it, in its language; [fallback] if it has none. */
    @SuppressLint("DiscouragedApi")
    private fun said(c: Context, name: String, fallback: Int) = try {
        val pkg = Sys.settingsPackage(c)
        val res = c.packageManager.getResourcesForApplication(pkg)
        res.getIdentifier(name, "string", pkg).takeIf { it != 0 }?.let(res::getString)
    } catch (_: Exception) {
        null
    } ?: c.getString(fallback)

    fun appInfo(a: Activity) =
        open(a, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.packageName}")))

    private fun open(a: Activity, i: Intent) {
        Grants.give(Sys.settingsPackage(a), 5 * 60_000L)
        try {
            a.startActivity(i)
        } catch (_: ActivityNotFoundException) {
            a.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
