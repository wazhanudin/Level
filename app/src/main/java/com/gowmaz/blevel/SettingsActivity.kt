package com.gowmaz.blevel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.gowmaz.blevel.ui.LevelTheme
import com.gowmaz.blevel.ui.SettingsScreen

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LevelTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}
