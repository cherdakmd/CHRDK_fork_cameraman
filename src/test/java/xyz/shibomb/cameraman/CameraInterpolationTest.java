package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class CameraInterpolationTest {

    @Test
    void interpolatesPositionAndTakesShortestYawPath() {
        Location from = new Location(null, 0.0, 10.0, 0.0, 170.0F, 0.0F);
        Location to = new Location(null, 10.0, 20.0, 30.0, -170.0F, 40.0F);

        Location halfway = CameraInterpolation.lerp(from, to, 0.5);

        assertEquals(5.0, halfway.getX(), 1.0e-9);
        assertEquals(15.0, halfway.getY(), 1.0e-9);
        assertEquals(15.0, halfway.getZ(), 1.0e-9);
        assertEquals(180.0F, halfway.getYaw(), 1.0e-5F);
        assertEquals(20.0F, halfway.getPitch(), 1.0e-5F);
    }

    @Test
    void supportsConfiguredEasingCurves() {
        assertEquals(0.5, CameraInterpolation.ease(0.5, "LINEAR"), 1.0e-9);
        assertEquals(0.5, CameraInterpolation.ease(0.5, "SMOOTHSTEP"), 1.0e-9);
        assertEquals(0.5, CameraInterpolation.ease(0.5, "EASE_IN_OUT"), 1.0e-9);
        assertEquals(0.5, CameraInterpolation.ease(0.5, "unknown"), 1.0e-9);
    }

    @Test
    void smoothStepHasZeroVelocityAtEndpoints() {
        assertEquals(0.0, CameraInterpolation.smoothStep(0.0), 1.0e-9);
        assertEquals(0.5, CameraInterpolation.smoothStep(0.5), 1.0e-9);
        assertEquals(1.0, CameraInterpolation.smoothStep(1.0), 1.0e-9);
    }
}
