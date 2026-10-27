package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InputValidatorsTest {

    @Test
    void acceptsOnlyExplicitBooleanValues() {
        assertTrue(InputValidators.isBoolean("true"));
        assertTrue(InputValidators.isBoolean("FALSE"));
        assertFalse(InputValidators.isBoolean("yes"));
        assertFalse(InputValidators.isBoolean("1"));
        assertFalse(InputValidators.isBoolean(null));
    }

    @Test
    void validatesPositiveIntervalsWithoutTickOverflow() {
        assertTrue(InputValidators.isPositiveInteger("1"));
        assertFalse(InputValidators.isPositiveInteger("0"));
        assertFalse(InputValidators.isPositiveInteger("-3"));
        assertFalse(InputValidators.isPositiveInteger("60.5"));
        assertFalse(InputValidators.isPositiveInteger("9999999999999999999"));
    }

    @Test
    void acceptsFiniteNumberRangesIncludingNegativeBounds() {
        assertTrue(InputValidators.isFiniteNumberOrRange("2.5"));
        assertTrue(InputValidators.isFiniteNumberOrRange("-1.0-1.0"));
        assertTrue(InputValidators.isFiniteNumberOrRange("-5--2"));
        assertFalse(InputValidators.isFiniteNumberOrRange("NaN"));
        assertFalse(InputValidators.isFiniteNumberOrRange("1-Infinity"));
        assertFalse(InputValidators.isFiniteNumberOrRange("1-2-3"));
    }

    @Test
    void profileNamesAreSafeForConfigurationPaths() {
        assertTrue(InputValidators.isValidProfileName("cinematic_01"));
        assertFalse(InputValidators.isValidProfileName("../unsafe"));
        assertFalse(InputValidators.isValidProfileName("name.with.dots"));
        assertFalse(InputValidators.isValidProfileName(""));
    }
}
