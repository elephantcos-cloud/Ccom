package com.cleaner.pro

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun wrap(ctx: Context): Context {
        val lang = PrefsHelper.getLanguage(ctx).ifEmpty { "bn" }
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val cfg = Configuration(ctx.resources.configuration)
        cfg.setLocale(locale)
        return ctx.createConfigurationContext(cfg)
    }
}
