package ai.askpancho.desconexion

object Sites {
    /** Browser package to the view id of its address bar. */
    val BROWSERS = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.chrome.beta" to "com.chrome.beta:id/url_bar",
        "com.chrome.dev" to "com.chrome.dev:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.vivaldi.browser" to "com.vivaldi.browser:id/url_bar",
        "com.kiwibrowser.browser" to "com.kiwibrowser.browser:id/url_bar",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.opera.browser" to "com.opera.browser:id/url_field",
        "com.duckduckgo.mobile.android" to "com.duckduckgo.mobile.android:id/omnibarTextInput",
    )

    fun host(url: String): String? {
        val s = url.trim().lowercase()
        if (s.isEmpty() || ' ' in s) return null
        return s.substringAfter("://")
            .substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
            .removePrefix("www.")
            .ifEmpty { null }
    }

    /** A line typed by the user, reduced to a bare domain. */
    fun normalize(line: String) = host(line)?.takeIf { '.' in it }

    fun match(host: String, sites: List<String>) =
        sites.firstOrNull { host == it || host.endsWith(".$it") }
}
