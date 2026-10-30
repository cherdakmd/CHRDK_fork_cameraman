package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

    @Test
    void comparesReleaseTagsNumerically() {
        var installed = UpdateChecker.Version.parse("v26.3.5");
        assertEquals(0, installed.compareTo(UpdateChecker.Version.parse("26.3.5")));
        assertTrue(installed.compareTo(UpdateChecker.Version.parse("v26.3.6")) < 0);
        assertTrue(installed.compareTo(UpdateChecker.Version.parse("v26.4.0")) < 0);
        assertTrue(installed.compareTo(UpdateChecker.Version.parse("v26.3.4")) > 0);
    }

    @Test
    void ignoresDevelopmentAndMalformedVersions() {
        assertNull(UpdateChecker.Version.parse("dev"));
        assertNull(UpdateChecker.Version.parse(null));
        assertNull(UpdateChecker.Version.parse("v26.3-beta"));
    }
}
