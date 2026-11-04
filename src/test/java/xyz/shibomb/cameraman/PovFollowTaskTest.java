package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PovFollowTaskTest {

    @Test
    void targetsInsideTheTrackingRangeDoNotForceRepositioning() {
        assertTrue(PovFollowTask.tracksAt(0.0));
        assertTrue(PovFollowTask.tracksAt(PovFollowTask.TRACKING_DISTANCE * PovFollowTask.TRACKING_DISTANCE));
        assertFalse(PovFollowTask.tracksAt((PovFollowTask.TRACKING_DISTANCE + 1.0)
                * (PovFollowTask.TRACKING_DISTANCE + 1.0)));
        assertFalse(PovFollowTask.tracksAt(Double.NaN));
    }

    @Test
    void attachAttemptsAreThrottled() {
        assertTrue(PovFollowTask.shouldRetry(10L, -1L));
        assertFalse(PovFollowTask.shouldRetry(11L, 10L));
        assertTrue(PovFollowTask.shouldRetry(10L + PovFollowTask.RETRY_INTERVAL_TICKS, 10L));
    }

    @Test
    void aDeadTargetIsToleratedOnlyForARespawnWindow() {
        assertFalse(PovFollowTask.exceedsDeadTicks(0L));
        assertFalse(PovFollowTask.exceedsDeadTicks(PovFollowTask.MAX_DEAD_TICKS));
        assertTrue(PovFollowTask.exceedsDeadTicks(PovFollowTask.MAX_DEAD_TICKS + 1L));
    }

    @Test
    void repeatedFailuresEventuallyTeleportTheCameraOntoTheTarget() {
        assertFalse(PovFollowTask.shouldForceTeleport(1));
        assertFalse(PovFollowTask.shouldForceTeleport(PovFollowTask.FORCE_TELEPORT_ATTEMPTS - 1));
        assertTrue(PovFollowTask.shouldForceTeleport(PovFollowTask.FORCE_TELEPORT_ATTEMPTS));
    }
}
