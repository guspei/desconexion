package ai.askpancho.desconexion

import android.content.Context
import android.content.Intent
import java.util.concurrent.ConcurrentHashMap

object Apps {
    class App(val pkg: String, val label: String)

    private val labels = ConcurrentHashMap<String, String>()

    fun all(c: Context): List<App> =
        c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != c.packageName }
            .mapNotNull { p -> label(c, p)?.let { App(p, it) } }
            .sortedBy { it.label }

    fun label(c: Context, pkg: String): String? = labels[pkg] ?: try {
        val pm = c.packageManager
        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString().lowercase().also { labels[pkg] = it }
    } catch (_: Exception) {
        null
    }

    fun launch(c: Context, pkg: String): Boolean {
        val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
        return true
    }

    /** Opens from the launcher: allowed apps go straight through, the rest ask first. */
    fun open(c: Context, pkg: String) {
        if (Store.isAllowed(pkg) || pkg in Rules.exempt(c) || Grants.left(pkg) > 0) launch(c, pkg)
        else BlockActivity.start(c, pkg, BlockActivity.CHALLENGE, launch = true)
    }
}
