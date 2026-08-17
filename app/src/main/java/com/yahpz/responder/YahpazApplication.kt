package com.yahpz.responder

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class YahpazApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(hebrewContext(base))
    }
}

fun hebrewContext(base: Context): Context {
    val locale = Locale("he")
    Locale.setDefault(locale)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    config.setLayoutDirection(locale)
    return base.createConfigurationContext(config)
}
