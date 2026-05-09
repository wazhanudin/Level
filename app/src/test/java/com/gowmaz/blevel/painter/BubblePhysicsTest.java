package com.gowmaz.blevel.painter;

import org.junit.Test;
import com.gowmaz.blevel.orientation.Orientation;
import static org.junit.Assert.*;

public class BubblePhysicsTest {

    @Test
    public void testUpdateTop() {
        BubblePhysics physics = new BubblePhysics();
        physics.x = 100;
        physics.y = 100;
        physics.angleX = 0.5; // Tilted
        
        // Orientation TOP, reverse = 1
        physics.update(Orientation.TOP, 10.0, 0.016, 
                0, 200, 0, 50, // minX, maxX, minY, maxY
                200, 50, 200, // width, height, maxDim
                30, 30, 15, // bubbleWidth, bubbleHeight, halfWidth
                2, 166, 16, // borderWidth, minusWidth, minusHeight
                100, 25); // middleX, middleY
        
        // posX should be (2*100 - 0 - 200) / 166 = 0
        // speedX = 1 * (2 * 0.5 - 0) * 10.0 = 10.0
        // x = 100 + 10.0 * 0.016 = 100.16
        assertEquals(100.16, physics.x, 0.01);
    }

    @Test
    public void testLandingConstraint() {
        BubblePhysics physics = new BubblePhysics();
        physics.x = 200; // Far outside
        physics.y = 200;
        
        // Orientation LANDING
        physics.update(Orientation.LANDING, 10.0, 0.016, 
                0, 200, 0, 200, 
                200, 200, 200,
                30, 30, 15,
                2, 166, 166,
                100, 100);
        
        double distance = Math.sqrt(Math.pow(physics.x - 100, 2) + Math.pow(physics.y - 100, 2));
        double maxRadius = 200 / 2.0f - 15 - 2; // middle - halfBubble - border = 100 - 15 - 2 = 83
        
        assertTrue("Bubble should be constrained within radius " + maxRadius + ", but was at " + distance, 
                distance <= maxRadius + 0.01);
    }
}
