package com.candrive.app

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class CanDriveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        val cfg = Configuration.getInstance()
        cfg.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        cfg.userAgentValue = packageName
        cfg.osmdroidBasePath = File(cacheDir, "osmdroid")
        cfg.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        Ui.applyTheme()
        Ui.applyLanguage(this)
    }
}
