package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RotationTaskTest {

    @Test
    void closerTargetsReceiveHigherWeightWithoutZeroingDistantTargets() {
        double nearWeight = RotationTask.distanceWeight(5.0);
        double farWeight = RotationTask.distanceWeight(100.0);

        assertTrue(nearWeight > farWeight);
        assertTrue(farWeight > 0.0);
        assertEquals(1.0, RotationTask.distanceWeight(0.0), 1.0e-12);
    }

    @Test
    void invalidDistancesUseSafeLowWeight() {
        assertEquals(RotationTask.distanceWeight(256.0), RotationTask.distanceWeight(Double.NaN), 1.0e-12);
        assertEquals(RotationTask.distanceWeight(256.0), RotationTask.distanceWeight(Double.POSITIVE_INFINITY), 1.0e-12);
    }
}
