package ai.askpancho.desconexion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LogicTest {
    @Test
    fun challengesAreSolvableAndPositive() {
        val r = Random(7)
        repeat(5000) {
            val (q, a) = Challenge.next(r)
            assertTrue(q, a in 30..1999)
            val expr = q.replace("×", "*").replace("−", "-")
            assertEquals(q, eval(expr), a)
        }
    }

    @Test
    fun hostsAreReducedToDomains() {
        assertEquals("youtube.com", Sites.host("https://www.youtube.com/watch?v=1"))
        assertEquals("m.youtube.com", Sites.host("m.youtube.com/feed"))
        assertEquals("instagram.com", Sites.host("instagram.com:443"))
        assertNull(Sites.host("como hacer pan"))
        assertNull(Sites.normalize("youtube"))
        assertEquals("x.com", Sites.normalize(" X.com "))
    }

    @Test
    fun siteLinesCarryOptionalDailyMinutes() {
        assertEquals("youtube.com" to 10, Sites.parse("https://www.YouTube.com 10m"))
        assertEquals("9gag.com" to 15, Sites.parse(" 9gag.com   15 min"))
        assertEquals("tiktok.com" to 0, Sites.parse("tiktok.com"))
        assertNull(Sites.parse("   "))
    }

    @Test
    fun subdomainsMatchButLookalikesDoNot() {
        val sites = listOf("youtube.com")
        assertEquals("youtube.com", Sites.match("m.youtube.com", sites))
        assertEquals("youtube.com", Sites.match("youtube.com", sites))
        assertNull(Sites.match("notyoutube.com", sites))
    }

    @Test
    fun durations() {
        assertEquals("0m", Fmt.dur(0))
        assertEquals("59m", Fmt.dur(59 * 60_000L + 59_000))
        assertEquals("1h", Fmt.dur(60 * 60_000L))
        assertEquals("2h 5m", Fmt.dur(125 * 60_000L))
        assertEquals("0m", Fmt.dur(-5000))
    }

    // Tiny evaluator for "a * b + c", "a * b - c", "a * b", "(a + b) * c".
    private fun eval(e: String): Int {
        val s = e.replace(" ", "")
        if (s.startsWith("(")) {
            val inner = s.substring(1, s.indexOf(')')).split('+').sumOf { it.toInt() }
            return inner * s.substringAfter(")*").toInt()
        }
        val (a, rest) = s.split('*', limit = 2)
        val m = Regex("(\\d+)([+-])?(\\d+)?").matchEntire(rest)!!.groupValues
        val prod = a.toInt() * m[1].toInt()
        return when (m[2]) { "+" -> prod + m[3].toInt(); "-" -> prod - m[3].toInt(); else -> prod }
    }
}
