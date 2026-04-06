package com.cleaner.pro

import android.content.Context

object PrefsHelper {
    private const val P = "cleaner_pro_prefs"
    const val THEME_DARK = "dark"
    const val THEME_LIGHT = "light"
    const val THEME_SYSTEM = "system"

    private fun p(ctx: Context) = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)

    fun getTheme(ctx: Context) = p(ctx).getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM
    fun setTheme(ctx: Context, t: String) = p(ctx).edit().putString("theme", t).apply()

    fun getLanguage(ctx: Context) = p(ctx).getString("language", "bn") ?: "bn"
    fun setLanguage(ctx: Context, l: String) = p(ctx).edit().putString("language", l).apply()

    fun isAutoClean(ctx: Context) = p(ctx).getBoolean("auto_clean", false)
    fun setAutoClean(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("auto_clean", v).apply()

    fun isNotifOn(ctx: Context) = p(ctx).getBoolean("notifications", true)
    fun setNotif(ctx: Context, v: Boolean) = p(ctx).edit().putBoolean("notifications", v).apply()
}
