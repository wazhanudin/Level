package com.gowmaz.blevel.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.gowmaz.blevel.orientation.Orientation
import com.gowmaz.blevel.orientation.OrientationListener

/**
 * ViewModel to bridge OrientationProvider data to Compose State.
 * Follows modern Android MVVM patterns.
 */
class LevelViewModel : ViewModel(), OrientationListener {

    private val _orientation = mutableStateOf(Orientation.TOP)
    val orientation: State<Orientation> = _orientation

    private val _pitch = mutableStateOf(0f)
    val pitch: State<Float> = _pitch

    private val _roll = mutableStateOf(0f)
    val roll: State<Float> = _roll

    private val _balance = mutableStateOf(0f)
    val balance: State<Float> = _balance

    private val _calibrationSaved = mutableStateOf<Boolean?>(null)
    val calibrationSaved: State<Boolean?> = _calibrationSaved

    private val _calibrationReset = mutableStateOf<Boolean?>(null)
    val calibrationReset: State<Boolean?> = _calibrationReset

    private val _isLocked = mutableStateOf(false)
    val isLocked: State<Boolean> = _isLocked

    fun setLocked(locked: Boolean) {
        _isLocked.value = locked
    }
    
    fun toggleLock() {
        _isLocked.value = !_isLocked.value
    }

    override fun onOrientationChanged(orientation: Orientation, pitch: Float, roll: Float, balance: Float) {
        _orientation.value = orientation
        _pitch.value = pitch
        _roll.value = roll
        _balance.value = balance
    }

    override fun onCalibrationSaved(success: Boolean) {
        _calibrationSaved.value = success
    }

    override fun onCalibrationReset(success: Boolean) {
        _calibrationReset.value = success
    }

    fun clearCalibrationStatus() {
        _calibrationSaved.value = null
        _calibrationReset.value = null
    }

    fun cycleOrientation() {
        val entries = Orientation.entries
        val currentOrdinal = _orientation.value.ordinal
        val nextOrdinal = (currentOrdinal + 1) % entries.size
        _orientation.value = entries[nextOrdinal]
    }
}
