package com.cleaner.pro

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {
    fun applyTheme(ctx: Context) {
        AppCompatDelegate.setDefaultNightMode(
            when (PrefsHelper.getTheme(ctx)) {
                PrefsHelper.THEME_DARK  -> AppCompatDelegate.MODE_NIGHT_YES
                PrefsHelper.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun isDark(ctx: Context): Boolean {
        val m = ctx.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return m == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun bgColor(ctx: Context) =
        if (isDark(ctx)) Color.parseColor("#0A1628") else Color.parseColor("#F2F4F8")

    fun cardColor(ctx: Context) =
        if (isDark(ctx)) Color.parseColor("#111E33") else Color.parseColor("#FFFFFF")

    fun textPrimary(ctx: Context) =
        if (isDark(ctx)) Color.WHITE else Color.parseColor("#0A1628")

    fun textSecondary(ctx: Context) =
        if (isDark(ctx)) Color.parseColor("#8A9BBE") else Color.parseColor("#5A6B8A")

    fun dividerColor(ctx: Context) =
        if (isDark(ctx)) Color.parseColor("#1E2E4A") else Color.parseColor("#DDE3EE")

    fun navColor(ctx: Context) =
        if (isDark(ctx)) Color.parseColor("#0D1B33") else Color.parseColor("#FFFFFF")
}
