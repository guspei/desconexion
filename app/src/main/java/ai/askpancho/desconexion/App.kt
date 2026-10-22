package ai.askpancho.desconexion

import android.app.Application
import java.util.concurrent.Executors

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}

object Bg {
    private val pool = Executors.newSingleThreadExecutor()
    fun run(f: () -> Unit) = pool.execute(f)
}

/** Temporary passes earned by solving a challenge. Cleared when going home. */
object Grants {
    const val SPAN = 10 * 60_000L
    private val until = HashMap<String, Long>()

    fun give(key: String, ms: Long = SPAN) {
        until[key] = android.os.SystemClock.elapsedRealtime() + ms
    }

    fun left(key: String) = (until[key] ?: 0L) - android.os.SystemClock.elapsedRealtime()

    fun clear() = until.clear()
}
