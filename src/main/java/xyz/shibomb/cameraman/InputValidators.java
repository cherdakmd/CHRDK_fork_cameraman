package xyz.shibomb.cameraman;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure validation helpers shared by command handling and unit tests. */
public final class InputValidators {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)$");
    private static final Pattern PROFILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private InputValidators() {
    }

    public static boolean isBoolean(String value) {
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
    }

    public static boolean isPositiveInteger(String value) {
        if (value == null) return false;
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 && parsed <= Long.MAX_VALUE / 20L;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isPositiveFiniteNumber(String value) {
        if (value == null) return false;
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isFiniteNumberOrRange(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        try {
            if (NUMBER_PATTERN.matcher(trimmed).matches()) {
                return Double.isFinite(Double.parseDouble(trimmed));
            }
            Matcher matcher = RANGE_PATTERN.matcher(trimmed);
            return matcher.matches()
                    && Double.isFinite(Double.parseDouble(matcher.group(1)))
                    && Double.isFinite(Double.parseDouble(matcher.group(2)));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidProfileName(String value) {
        return value != null && PROFILE_NAME_PATTERN.matcher(value).matches();
    }
}
