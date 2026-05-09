package com.gowmaz.blevel.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import java.util.Collections
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.sqrt

/**
 * Thread-safe provider for orientation data from device sensors.
 * Optimized for performance and prevents memory leaks by using ApplicationContext.
 */
class OrientationProvider private constructor(context: Context) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var listener: OrientationListener? = null
    private var running = false
    private var supported: Boolean? = null
    private var calibrating = false
    private var locked = false
    private var lockedOrientation: Orientation? = null

    private val calibratedPitch = FloatArray(Orientation.entries.size)
    private val calibratedRoll = FloatArray(Orientation.entries.size)
    private val calibratedBalance = FloatArray(Orientation.entries.size)

    // Pre-allocated buffers for sensor calculations to avoid GC pressure
    private val bufferMAG = floatArrayOf(1f, 1f, 1f)
    private val bufferI = FloatArray(16)
    private val bufferR = FloatArray(16)
    private val bufferOutR = FloatArray(16)
    private val bufferLOC = FloatArray(3)

    private var pitch = 0f
    private var roll = 0f
    private var balance = 0f
    private var minStep = 360f
    private var refValues = 0f

    companion object {
        private const val MIN_VALUES = 20
        private const val PREFS_NAME = "OrientationCalibration"
        private const val SAVED_PITCH = "pitch."
        private const val SAVED_ROLL = "roll."
        private const val SAVED_BALANCE = "balance."

        @Volatile
        private var instance: OrientationProvider? = null

        @JvmStatic
        fun getInstance(context: Context): OrientationProvider {
            return instance ?: synchronized(this) {
                instance ?: OrientationProvider(context).also { instance = it }
            }
        }
    }

    val isListening: Boolean get() = running

    fun stopListening() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
    }

    val isSupported: Boolean
        get() {
            if (supported == null) {
                val sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)
                supported = sensors.isNotEmpty()
            }
            return supported!!
        }

    fun startListening(orientationListener: OrientationListener) {
        if (running) stopListening()
        
        this.listener = orientationListener
        calibrating = false
        
        loadCalibration()

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor != null) {
            running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun loadCalibration() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Orientation.entries.forEach { orient ->
            val index = orient.ordinal
            calibratedPitch[index] = prefs.getFloat(SAVED_PITCH + orient, 0f)
            calibratedRoll[index] = prefs.getFloat(SAVED_ROLL + orient, 0f)
            calibratedBalance[index] = prefs.getFloat(SAVED_BALANCE + orient, 0f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val oldPitch = pitch
        val oldRoll = roll
        val oldBalance = balance

        SensorManager.getRotationMatrix(bufferR, bufferI, event.values, bufferMAG)

        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }

        when (rotation) {
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(bufferR, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, bufferOutR)
            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(bufferR, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, bufferOutR)
            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(bufferR, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, bufferOutR)
            else -> SensorManager.remapCoordinateSystem(bufferR, SensorManager.AXIS_X, SensorManager.AXIS_Y, bufferOutR)
        }

        SensorManager.getOrientation(bufferOutR, bufferLOC)

        var tmp = sqrt((bufferOutR[8] * bufferOutR[8] + bufferOutR[9] * bufferOutR[9]).toDouble()).toFloat()
        tmp = if (tmp == 0f) 0f else bufferOutR[8] / tmp

        pitch = Math.toDegrees(bufferLOC[1].toDouble()).toFloat()
        roll = -Math.toDegrees(bufferLOC[2].toDouble()).toFloat()
        balance = Math.toDegrees(asin(tmp.toDouble())).toFloat()

        // Sensibility auto-detection
        if (oldRoll != roll || oldPitch != pitch || oldBalance != balance) {
            minStep = minOf(minStep, abs(pitch - oldPitch).takeIf { it > 0 } ?: minStep)
            minStep = minOf(minStep, abs(roll - oldRoll).takeIf { it > 0 } ?: minStep)
            minStep = minOf(minStep, abs(balance - oldBalance).takeIf { it > 0 } ?: minStep)
            if (refValues < MIN_VALUES) refValues++
        }

        val currentOrientation = if (locked && lockedOrientation != null) {
            lockedOrientation!!
        } else {
            when {
                pitch < -45 && pitch > -135 -> Orientation.TOP
                pitch > 45 && pitch < 135 -> Orientation.BOTTOM
                roll > 45 -> Orientation.RIGHT
                roll < -45 -> Orientation.LEFT
                else -> Orientation.LANDING
            }
        }

        if (calibrating) {
            saveCurrentCalibration(currentOrientation)
        } else {
            val idx = currentOrientation.ordinal
            pitch -= calibratedPitch[idx]
            roll -= calibratedRoll[idx]
            balance -= calibratedBalance[idx]
        }

        listener?.onOrientationChanged(currentOrientation, pitch, roll, balance)
    }

    private fun saveCurrentCalibration(orient: Orientation) {
        calibrating = false
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val success = prefs.edit().apply {
            putFloat(SAVED_PITCH + orient, pitch)
            putFloat(SAVED_ROLL + orient, roll)
            putFloat(SAVED_BALANCE + orient, balance)
        }.commit()
        
        if (success) {
            val idx = orient.ordinal
            calibratedPitch[idx] = pitch
            calibratedRoll[idx] = roll
            calibratedBalance[idx] = balance
        }
        
        listener?.onCalibrationSaved(success)
        pitch = 0f
        roll = 0f
        balance = 0f
    }

    fun resetCalibration() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val success = prefs.edit().clear().commit()
        if (success) {
            calibratedPitch.fill(0f)
            calibratedRoll.fill(0f)
            calibratedBalance.fill(0f)
        }
        listener?.onCalibrationReset(success)
    }

    fun saveCalibration() {
        calibrating = true
    }

    fun setLocked(locked: Boolean) {
        this.locked = locked
    }

    fun setOrientation(orientation: Orientation) {
        this.lockedOrientation = orientation
    }

    val sensibility: Float get() = if (refValues >= MIN_VALUES) minStep else 0f
}
