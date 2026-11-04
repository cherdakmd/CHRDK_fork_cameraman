package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class MessagesFormatTest {

    @Test
    void formatsWellFormedTemplates() {
        assertEquals("Цель выбрана: Steve",
                Messages.format(Locale.ROOT, "Цель выбрана: %s", "Steve"));
        assertEquals("Next switch: 12s", Messages.format(Locale.ROOT, "Next switch: %ss", 12));
        assertEquals("Distance: 3.5", Messages.format(Locale.ROOT, "Distance: %.1f", 3.5));
    }

    @Test
    void keepsEscapedPercentSigns() {
        assertEquals("100% done", Messages.format(Locale.ROOT, "100%% done"));
    }

    @Test
    void strayPercentSignDoesNotThrowAndStaysVisible() {
        assertEquals("Fatal 100% error: boom",
                Messages.format(Locale.ROOT, "Fatal 100% error: %s", "boom"));
    }

    @Test
    void missingArgumentKeepsThePlaceholderInsteadOfFailing() {
        assertEquals("Target: %s", Messages.format(Locale.ROOT, "Target: %s"));
    }

    @Test
    void extraArgumentsAreIgnored() {
        assertEquals("Only one", Messages.format(Locale.ROOT, "Only one", "extra"));
    }

    @Test
    void positionalFallbackSubstitutesInOrder() {
        assertEquals("A=1 B=two", Messages.positionalFormat("A=%s B=%s", 1, "two"));
        assertEquals("100% A=1", Messages.positionalFormat("100% A=%s", 1));
        assertEquals("A=%s B=%s", Messages.positionalFormat("A=%s B=%s"));
    }
}
