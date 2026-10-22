package ai.askpancho.desconexion

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** What the app needs from the system, in the order a new install asks for it. */
object Setup {
    class Step(
        val id: String,
        val title: String,
        val why: String,
        val how: String,
        val optional: Boolean,
        val done: (Context) -> Boolean,
        val go: (Activity) -> Unit,
    )

    /** Shown once per launch, so backing out of the guide does not trap you in it. */
    var shown = false

    val steps = listOf(
        Step(
            "home", "inicio",
            "Para que desconexión sea la pantalla de inicio del teléfono.",
            "Elige desconexión.",
            false, Sys::isHome,
        ) { it.startActivityForResult(Sys.homeRequest(it), 1) },
        Step(
            "watch", "accesibilidad",
            "Para saber qué app se abre y bloquearla si toca. No lee nada más y no tiene acceso a internet.",
            "Entra en desconexión y actívala. Si sale bloqueado: información de la app → ⋮ → permitir ajustes restringidos, y vuelve a intentarlo.",
            false, Sys::watcherOn,
        ) { open(it, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        Step(
            "usage", "uso",
            "Para contar tu tiempo y aplicar los límites diarios.",
            "Entra en desconexión y actívalo.",
            false, Usage::granted,
        ) { open(it, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        Step(
            "listen", "notificaciones",
            "Para silenciar las apps que no son libres y el silencio al bloquear.",
            "Activa desconexión.",
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
            "post", "avisos",
            "Para ver tus comprobaciones en la pantalla bloqueada sin desbloquear.",
            "Permite las notificaciones.",
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
