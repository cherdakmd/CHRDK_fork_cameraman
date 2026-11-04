package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

class SpectateTaskTest {

    @Test
    void distantTargetsAreSnappedInsteadOfCrawledTowards() {
        assertTrue(SpectateTask.shouldSnap(100.0, true));
        assertTrue(SpectateTask.shouldSnap(Double.NaN, true));
        assertTrue(SpectateTask.shouldSnap(0.0, false));
        assertFalse(SpectateTask.shouldSnap(2.0, true));
    }

    @Test
    void nearbyTargetsKeepSmoothInterpolation() {
        assertFalse(SpectateTask.shouldSnap(SpectateTask.SNAP_DISTANCE / 2.0, true));
        assertFalse(SpectateTask.shouldSnap(0.0, true));
        assertTrue(SpectateTask.shouldSnap(SpectateTask.SNAP_DISTANCE + 1.0, true));
    }

    @Test
    void catchUpStepGuaranteesForwardProgressForSlowSmoothing() {
        Location current = new Location(null, 0.0, 64.0, 0.0);
        Location desired = new Location(null, 20.0, 64.0, 0.0);
        Location slowFrame = CameraInterpolation.lerp(current, desired, 0.05);

        Location result = SpectateTask.ensureCatchUpStep(current, slowFrame, desired, 2.0);

        assertEquals(2.0, current.distance(result), 1.0e-6);
        assertTrue(current.distance(result) > current.distance(slowFrame));
    }

    @Test
    void catchUpStepLeavesCloseFramesUntouched() {
        Location current = new Location(null, 0.0, 64.0, 0.0);
        Location desired = new Location(null, 1.0, 64.0, 0.0);
        Location frame = CameraInterpolation.lerp(current, desired, 0.35);

        Location result = SpectateTask.ensureCatchUpStep(current, frame, desired, 2.0);

        assertEquals(frame.getX(), result.getX(), 1.0e-9);
    }

    @Test
    void catchUpStepDoesNotOvershootTheTarget() {
        Location current = new Location(null, 0.0, 64.0, 0.0);
        Location desired = new Location(null, 0.5, 64.0, 0.0);
        Location frame = CameraInterpolation.lerp(current, desired, 0.01);

        Location result = SpectateTask.ensureCatchUpStep(current, frame, desired, 4.0);

        assertTrue(current.distance(result) <= current.distance(desired));
        assertEquals(frame.getX(), result.getX(), 1.0e-9);
    }
}
