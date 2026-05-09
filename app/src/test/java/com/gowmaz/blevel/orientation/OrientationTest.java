package com.gowmaz.blevel.orientation;

import org.junit.Test;
import static org.junit.Assert.*;

public class OrientationTest {

    @Test
    public void testIsLevelLanding() {
        Orientation orientation = Orientation.LANDING;
        float sensibility = 0.5f;
        
        // Perfectly level
        assertTrue(orientation.isLevel(0, 0, 0, sensibility));
        
        // Within sensibility
        assertTrue(orientation.isLevel(0.4f, 0.4f, 10, sensibility));
        assertTrue(orientation.isLevel(179.6f, -0.4f, 10, sensibility));
        
        // Outside sensibility
        assertFalse(orientation.isLevel(0.6f, 0, 0, sensibility));
        assertFalse(orientation.isLevel(0, 0.6f, 0, sensibility));
    }

    @Test
    public void testIsLevelTopBottom() {
        float sensibility = 0.5f;
        
        assertTrue(Orientation.TOP.isLevel(10, 10, 0.4f, sensibility));
        assertFalse(Orientation.TOP.isLevel(10, 10, 0.6f, sensibility));
        
        assertTrue(Orientation.BOTTOM.isLevel(10, 10, -0.4f, sensibility));
        assertFalse(Orientation.BOTTOM.isLevel(10, 10, -0.6f, sensibility));
    }

    @Test
    public void testIsLevelLeftRight() {
        float sensibility = 0.5f;
        
        assertTrue(Orientation.LEFT.isLevel(0.4f, 10, 10, sensibility));
        assertTrue(Orientation.LEFT.isLevel(179.6f, 10, 10, sensibility));
        assertFalse(Orientation.LEFT.isLevel(1.0f, 10, 10, sensibility));
        
        assertTrue(Orientation.RIGHT.isLevel(-0.4f, 10, 10, sensibility));
        assertFalse(Orientation.RIGHT.isLevel(45f, 10, 10, sensibility));
    }
}
