package com.gowmaz.blevel

import android.app.Application
import com.gowmaz.blevel.util.PreferenceHelper

class LevelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.initPrefs(this)
    }
}
