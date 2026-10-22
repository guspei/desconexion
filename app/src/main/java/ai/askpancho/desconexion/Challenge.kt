package ai.askpancho.desconexion

import kotlin.random.Random

/** Mental arithmetic: hard enough to break the reflex, easy enough to do without paper. */
object Challenge {
    fun next(r: Random = Random): Pair<String, Int> = when (r.nextInt(4)) {
        0 -> {
            val a = r.nextInt(13, 50); val b = r.nextInt(3, 10); val c = r.nextInt(11, 90)
            "$a × $b + $c" to a * b + c
        }
        1 -> {
            val a = r.nextInt(13, 50); val b = r.nextInt(4, 10); val c = r.nextInt(11, minOf(90, a * b - 29))
            "$a × $b − $c" to a * b - c
        }
        2 -> {
            val a = r.nextInt(12, 30); val b = r.nextInt(11, 20)
            "$a × $b" to a * b
        }
        else -> {
            val a = r.nextInt(11, 40); val b = r.nextInt(11, 40); val c = r.nextInt(3, 9)
            "($a + $b) × $c" to (a + b) * c
        }
    }
}
