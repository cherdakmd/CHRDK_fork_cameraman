package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraObstructionTest {

    @Test
    void cameraStaysInFrontOfAWallBetweenItAndTheTarget() {
        // 10 block ray, wall 4 blocks away, 0.4 margin => camera may occupy 3.6 blocks.
        assertEquals(3.6, CameraInterpolation.clampRayDistance(10.0, 4.0, 0.4), 1.0e-9);
    }

    @Test
    void unobstructedRayKeepsTheRequestedDistance() {
        assertEquals(10.0, CameraInterpolation.clampRayDistance(10.0, -1.0, 0.4), 1.0e-9);
        assertEquals(10.0, CameraInterpolation.clampRayDistance(10.0, Double.NaN, 0.4), 1.0e-9);
        assertEquals(10.0, CameraInterpolation.clampRayDistance(10.0, 42.0, 0.4), 1.0e-9);
    }

    @Test
    void obstacleRightNextToTheTargetNeverPushesTheCameraThroughIt() {
        double allowed = CameraInterpolation.clampRayDistance(10.0, 0.1, 0.4);

        assertTrue(allowed >= 0.0);
        assertTrue(allowed <= 0.1, "camera must stay on the target side of the wall");
    }

    @Test
    void invalidInputsFallBackToASafeDistance() {
        assertEquals(0.0, CameraInterpolation.clampRayDistance(Double.NaN, 4.0, 0.4), 1.0e-9);
        assertEquals(0.0, CameraInterpolation.clampRayDistance(-3.0, 4.0, 0.4), 1.0e-9);
        assertEquals(5.0, CameraInterpolation.clampRayDistance(5.0, 20.0, Double.NaN), 1.0e-9);
    }

    @Test
    void clampingNeverExceedsTheRequestedDistance() {
        assertEquals(2.0, CameraInterpolation.clampRayDistance(2.0, 100.0, 0.4), 1.0e-9);
    }
}
