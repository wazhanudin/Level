package com.gowmaz.blevel.orientation

/**
 * Interface for orientation changes and calibration events.
 */
interface OrientationListener {
    /**
     * Called when the orientation has changed.
     * @param orientation The current orientation.
     * @param pitch The pitch in degrees.
     * @param roll The roll in degrees.
     * @param balance The balance.
     */
    fun onOrientationChanged(orientation: Orientation, pitch: Float, roll: Float, balance: Float)

    /**
     * Called when the calibration has been saved.
     * @param success Whether the calibration was saved successfully.
     */
    fun onCalibrationSaved(success: Boolean)

    /**
     * Called when the calibration has been reset.
     * @param success Whether the calibration was reset successfully.
     */
    fun onCalibrationReset(success: Boolean)
}
