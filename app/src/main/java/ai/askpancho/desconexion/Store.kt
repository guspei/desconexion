package ai.askpancho.desconexion

import android.content.Context
import android.content.SharedPreferences

/**
 * Every app is blocked (asks for a sum), free, or free up to some minutes a day.
 * Apps without a rule of their own follow "the rest": free or blocked.
 * The home screen lists the apps put on it, in the order they were added.
 */
object Store {
    const val CHALLENGE = 0
    const val FREE = -1
    val DEFAULT_CHECKS = listOf("puerta", "gas", "agua", "luces", "coche")

    private lateinit var sp: SharedPreferences
    private var allowedList: List<String> = emptyList()
    private var allowedSet: Set<String> = emptySet()
    private var offHome: Set<String> = emptySet()
    private var blocked: Set<String> = emptySet()
    private var siteList: List<String> = emptyList()

    fun init(c: Context) {
        if (::sp.isInitialized) return
        sp = c.getSharedPreferences("d", Context.MODE_PRIVATE)
        load()
    }

    private fun load() {
        allowedList = sp.getString("allowed", "")!!.split(',').filter { it.isNotEmpty() }
        allowedSet = allowedList.toHashSet()
        offHome = set("offHome")
        blocked = set("blocked")
        siteList = sp.getString("sites", "")!!.split('\n').filter { it.isNotEmpty() }
    }

    private fun set(key: String) = sp.getString(key, "")!!.split(',').filter { it.isNotEmpty() }.toHashSet()

    val ready get() = sp.getBoolean("setup", false)

    fun setup(allowed: List<String>, block: List<String>) {
        sp.edit()
            .putString("allowed", allowed.joinToString(","))
            .putString("blocked", block.joinToString(","))
            .putBoolean("setup", true)
            .apply()
        load()
    }

    fun allowed(): List<String> = allowedList
    fun isAllowed(pkg: String) = pkg in allowedSet || (restFree && pkg !in blocked)
    fun home() = allowedList.filter { it !in offHome }
    fun onHome(pkg: String) = pkg in allowedSet && pkg !in offHome
    fun limit(pkg: String) = sp.getInt("limit.$pkg", 0)

    /** Apps with no rule of their own open freely (true) or ask for a sum (false). */
    var restFree: Boolean
        get() = sp.getBoolean("restFree", false)
        set(v) = sp.edit().putBoolean("restFree", v).apply()

    fun rule(pkg: String) = when {
        !isAllowed(pkg) -> CHALLENGE
        limit(pkg) > 0 -> limit(pkg)
        else -> FREE
    }

    fun setRule(pkg: String, rule: Int) {
        val list = allowedList.toMutableList()
        var off = offHome
        if (rule == CHALLENGE) {
            list.remove(pkg)
            off = off - pkg
        } else if (pkg !in list) {
            // Newly freed apps stay off the home screen until put there.
            list += pkg
            off = off + pkg
        }
        save(list, off, if (rule == CHALLENGE) blocked + pkg else blocked - pkg)
        sp.edit().apply { if (rule > 0) putInt("limit.$pkg", rule) else remove("limit.$pkg") }.apply()
    }

    fun setOnHome(pkg: String, on: Boolean) {
        val list = allowedList.toMutableList()
        if (on && pkg !in list) list += pkg
        save(list, if (on) offHome - pkg else offHome + pkg, if (on) blocked - pkg else blocked)
    }

    private fun save(list: List<String>, off: Set<String>, block: Set<String>) {
        sp.edit()
            .putString("allowed", list.joinToString(","))
            .putString("offHome", off.joinToString(","))
            .putString("blocked", block.joinToString(","))
            .apply()
        load()
    }

    var sites: List<String>
        get() = siteList
        set(v) {
            sp.edit().putString("sites", v.joinToString("\n")).apply()
            siteList = v
        }

    /** Extra minutes granted today on top of the daily limit. */
    fun extension(pkg: String): Int {
        val v = sp.getString("ext.$pkg", null) ?: return 0
        val (day, min) = v.split(':')
        return if (day.toLong() == Usage.today()) min.toInt() else 0
    }

    fun extend(pkg: String, minutes: Int) {
        sp.edit().putString("ext.$pkg", "${Usage.today()}:${extension(pkg) + minutes}").apply()
    }

    var checks: List<String>
        get() = sp.getString("checks", null)?.split('\n')?.filter { it.isNotEmpty() } ?: DEFAULT_CHECKS
        set(v) = sp.edit().putString("checks", v.joinToString("\n")).apply()

    /** Wall-clock time an item was last checked, 0 if not checked. */
    fun checkedAt(item: String) = sp.getLong("checked.$item", 0L)

    fun setChecked(item: String, at: Long) = sp.edit().putLong("checked.$item", at).apply()

    /** Minutes a check stays valid. */
    var checkSpan: Int
        get() = sp.getInt("checkSpan", 30)
        set(v) = sp.edit().putInt("checkSpan", v).apply()

    /** Do not disturb while the phone is locked. */
    var quiet: Boolean
        get() = sp.getBoolean("quiet", false)
        set(v) = sp.edit().putBoolean("quiet", v).apply()

    /** Set while do not disturb is on because we turned it on at lock. */
    var quietOurs: Boolean
        get() = sp.getBoolean("quietOurs", false)
        set(v) = sp.edit().putBoolean("quietOurs", v).apply()

    /** Optional setup steps the user chose to skip. */
    fun skipped(step: String) = sp.getBoolean("skip.$step", false)

    fun skip(step: String) = sp.edit().putBoolean("skip.$step", true).apply()

    var gray: Boolean
        get() = sp.getBoolean("gray", false)
        set(v) = sp.edit().putBoolean("gray", v).apply()

    var mute: Boolean
        get() = sp.getBoolean("mute", true)
        set(v) = sp.edit().putBoolean("mute", v).apply()
}
