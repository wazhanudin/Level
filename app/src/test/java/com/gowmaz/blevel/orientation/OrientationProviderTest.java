package com.gowmaz.blevel.orientation;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class OrientationProviderTest {

    @Mock
    private AppCompatActivity mockActivity;
    @Mock
    private Context mockAppContext;
    @Mock
    private SensorManager mockSensorManager;
    @Mock
    private WindowManager mockWindowManager;
    @Mock
    private Display mockDisplay;
    @Mock
    private SharedPreferences mockPrefs;
    @Mock
    private SharedPreferences.Editor mockEditor;

    private OrientationProvider orientationProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockActivity.getApplicationContext()).thenReturn(mockAppContext);
        when(mockAppContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager);
        when(mockAppContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager);
        when(mockAppContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        
        when(mockActivity.getPreferences(Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor);
        when(mockEditor.commit()).thenReturn(true);
        when(mockEditor.clear()).thenReturn(mockEditor);
        when(mockSensorManager.registerListener(any(OrientationProvider.class), any(Sensor.class), anyInt())).thenReturn(true);
        
        when(mockWindowManager.getDefaultDisplay()).thenReturn(mockDisplay);
        when(mockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
        
        // Use reflection to reset singleton for each test
        try {
            Field instance = OrientationProvider.class.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(null, null);
        } catch (Exception e) {}
        
        orientationProvider = OrientationProvider.getInstance(mockActivity);
    }

    @Test
    public void testIsSupported() {
        Sensor mockSensor = mock(Sensor.class);
        when(mockSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER))
                .thenReturn(Collections.singletonList(mockSensor));
        assertTrue(orientationProvider.isSupported());
    }

    @Test
    public void testStartStopListening() {
        Sensor mockSensor = mock(Sensor.class);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(mockSensor);
        
        OrientationListener mockListener = mock(OrientationListener.class);
        orientationProvider.startListening(mockListener);
        assertTrue(orientationProvider.isListening());

        orientationProvider.stopListening();
        assertFalse(orientationProvider.isListening());
    }

    @Test
    public void testOnSensorChanged() throws Exception {
        OrientationListener mockListener = mock(OrientationListener.class);
        orientationProvider.startListening(mockListener);

        SensorEvent mockEvent = mock(SensorEvent.class);
        Sensor mockSensor = mock(Sensor.class);
        when(mockSensor.getType()).thenReturn(Sensor.TYPE_ACCELEROMETER);
        
        Field sensorField = SensorEvent.class.getField("sensor");
        sensorField.setAccessible(true);
        sensorField.set(mockEvent, mockSensor);
        
        Field valuesField = SensorEvent.class.getField("values");
        valuesField.setAccessible(true);
        valuesField.set(mockEvent, new float[]{0, 0, 9.81f});

        orientationProvider.onSensorChanged(mockEvent);
        verify(mockListener, atLeastOnce()).onOrientationChanged(any(Orientation.class), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    public void testResetCalibration() {
        OrientationListener mockListener = mock(OrientationListener.class);
        orientationProvider.startListening(mockListener);
        
        orientationProvider.resetCalibration();
        verify(mockEditor, atLeastOnce()).clear();
        verify(mockListener).onCalibrationReset(true);
    }
}
