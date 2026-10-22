package ai.askpancho.desconexion

object Fmt {
    fun dur(ms: Long): String {
        val m = (ms / 60_000).coerceAtLeast(0)
        return when {
            m < 60 -> "${m}m"
            m % 60 == 0L -> "${m / 60}h"
            else -> "${m / 60}h ${m % 60}m"
        }
    }
}
