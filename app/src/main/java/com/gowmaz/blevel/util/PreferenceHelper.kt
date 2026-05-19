package com.gowmaz.blevel.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager

/**
 * Modernized helper for managing application preferences.
 */
object PreferenceHelper {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var resources: Resources

    @JvmStatic
    fun initPrefs(context: Context) {
        val appContext = context.applicationContext
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        resources = appContext.resources
    }

    private fun getPrefKey(@StringRes prefKey: Int): String = resources.getString(prefKey)

    private fun getBoolean(@StringRes prefKey: Int, defValue: Boolean): Boolean =
        sharedPrefs.getBoolean(getPrefKey(prefKey), defValue)

    private fun getString(@StringRes prefKey: Int, @StringRes defValue: Int): String? =
        sharedPrefs.getString(getPrefKey(prefKey), getPrefKey(defValue))

    private fun isKeyEqual(currentValue: String?, @StringRes prefKey: Int): Boolean =
        currentValue == getPrefKey(prefKey)

    @JvmStatic
    fun getShowAngle(): Boolean = getBoolean(PrefKeys.PREF_SHOW_ANGLE, true)

    @JvmStatic
    fun isDisplayTypeInclination(): Boolean =
        isKeyEqual(getString(PrefKeys.PREF_DISPLAY_TYPE, PrefKeys.PREF_DISPLAY_TYPE_ANGLE), PrefKeys.PREF_DISPLAY_TYPE_INCLINATION)

    @JvmStatic
    fun getDisplayTypeMax(): Float = if (isDisplayTypeInclination()) 999.9f else 99.9f

    @JvmStatic
    fun getOrientationLocked(): Boolean = getBoolean(PrefKeys.PREF_LOCK_ORIENTATION, false)

    @JvmStatic
    fun isViscosityLow(): Boolean = 
        isKeyEqual(getString(PrefKeys.PREF_VISCOSITY, PrefKeys.PREF_VISCOSITY_MEDIUM), PrefKeys.PREF_VISCOSITY_LOW)

    @JvmStatic
    fun isViscosityHigh(): Boolean = 
        isKeyEqual(getString(PrefKeys.PREF_VISCOSITY, PrefKeys.PREF_VISCOSITY_MEDIUM), PrefKeys.PREF_VISCOSITY_HIGH)

    @JvmStatic
    fun getViscosityCoefficient(): Double = when {
        isViscosityLow() -> 0.6
        isViscosityHigh() -> 0.2
        else -> 0.4
    }

    @JvmStatic
    fun getSoundEnabled(): Boolean = getBoolean(PrefKeys.PREF_ENABLE_SOUND, false)

    @JvmStatic
    fun getOffsetAngle(): Int = sharedPrefs.getInt("pref_offsetAngle", 0)
}
