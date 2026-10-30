package xyz.shibomb.cameraman.shots;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FlybyShotTest {

    @Test
    void durationAlwaysProducesASafePositiveTickPeriod() {
        assertEquals(1L, FlybyShot.toDurationTicks(0.01));
        assertEquals(1L, FlybyShot.toDurationTicks(0.0));
        assertEquals(1L, FlybyShot.toDurationTicks(Double.NaN));
        assertEquals(1L, FlybyShot.toDurationTicks(Double.POSITIVE_INFINITY));
        assertEquals(1_728_000L, FlybyShot.toDurationTicks(Double.MAX_VALUE));
        assertEquals(40L, FlybyShot.toDurationTicks(2.0));
    }
}
