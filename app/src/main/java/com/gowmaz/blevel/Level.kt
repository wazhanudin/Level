package com.gowmaz.blevel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.gowmaz.blevel.orientation.OrientationProvider
import com.gowmaz.blevel.ui.LevelTheme
import com.gowmaz.blevel.ui.LevelViewModel
import com.gowmaz.blevel.ui.MainScreen
import com.gowmaz.blevel.util.PreferenceHelper
import com.gowmaz.blevel.util.SoundManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Main Activity for the Bubble Level application.
 * Manages sensor lifecycle and bridges orientation data to the Compose UI.
 */
class Level : AppCompatActivity() {

    private val viewModel: LevelViewModel by viewModels()
    private val orientationProvider by lazy { OrientationProvider.getInstance(this) }
    private val soundManager by lazy { SoundManager(this) }

    private var isRulerShowing by mutableStateOf(false)
    private var soundEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immersive edge-to-edge layout
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val orientation by viewModel.orientation
            val pitch by viewModel.pitch
            val roll by viewModel.roll
            val balance by viewModel.balance
            val isLocked by viewModel.isLocked
            
            // Reactive sync for orientation locking
            LaunchedEffect(isLocked) {
                if (isLocked) {
                    orientationProvider.setOrientation(viewModel.orientation.value)
                }
                orientationProvider.setLocked(isLocked)
            }

            // Reactive sound effects
            LaunchedEffect(orientation, pitch, roll, balance) {
                if (soundEnabled && orientation.isLevel(pitch, roll, balance, orientationProvider.sensibility)) {
                    soundManager.playBip()
                }
            }

            LevelTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSettingsClick = { 
                        startActivity(Intent(this, SettingsActivity::class.java)) 
                    },
                    onCalibrateClick = { 
                        showCalibrationDialog() 
                    },
                    onRulerToggle = { 
                        isRulerShowing = it 
                    },
                    onCycleOrientation = { 
                        viewModel.cycleOrientation()
                        orientationProvider.setOrientation(viewModel.orientation.value)
                    },
                    isRulerShowing = isRulerShowing
                )
            }
        }
    }

    private fun showCalibrationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.calibrate_title)
            .setMessage(R.string.calibrate_message)
            .setCancelable(true)
            .setPositiveButton(R.string.calibrate) { _, _ -> 
                orientationProvider.saveCalibration() 
            }
            .setNeutralButton(R.string.reset) { _, _ -> 
                orientationProvider.resetCalibration() 
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        soundEnabled = PreferenceHelper.getSoundEnabled()
        if (orientationProvider.isSupported) {
            orientationProvider.startListening(viewModel)
        } else {
            Toast.makeText(this, R.string.not_supported, Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        orientationProvider.stopListening()
    }

    override fun onDestroy() {
        soundManager.release()
        super.onDestroy()
    }
}
