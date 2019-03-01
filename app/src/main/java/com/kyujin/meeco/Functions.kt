package com.kyujin.meeco

import android.content.SharedPreferences
import android.support.design.widget.Snackbar
import android.view.View

public fun getCookieFromSharedPrefs(sharedPreferences: SharedPreferences): HashMap<String, String> {
    val cookieJar = sharedPreferences.all
    val cookies = HashMap<String, String>()
    cookieJar.forEach { t, any -> cookies[t] = any as String }
    return cookies
}

public fun generateCookieString(cookies: HashMap<String, String>): String {
    return cookies.toList().map { it.first + "=" + it.second }.joinToString("; ")
}

public fun updateCookie(oldCookies: HashMap<String, String>, newCookies: Collection<String>) {
    newCookies.forEach {
        val (k, v) = it.split(";")[0].split("=")
        oldCookies[k] = v
    }
}

public fun commitCookies(newCookies: HashMap<String, String>, edit: SharedPreferences.Editor) {
    newCookies.forEach { t, u -> edit.putString(t, u) }
    edit.apply()
}

public fun createSnackbar(text: String, view: View) {
    Snackbar.make(view, text, Snackbar.LENGTH_SHORT).show()
}