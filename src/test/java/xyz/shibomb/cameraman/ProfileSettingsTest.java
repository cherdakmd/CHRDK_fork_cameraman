package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Profiles must round-trip every camera setting. A setting added to {@code config.yml} but forgotten
 * in the profile list silently stops being saved and loaded.
 */
class ProfileSettingsTest {

    private static final Pattern TOP_LEVEL_KEY = Pattern.compile("^([A-Za-z][A-Za-z0-9_]*)\\s*:.*$");

    /** Global/infrastructure keys that are not part of a camera profile. */
    private static final Set<String> NOT_A_CAMERA_SETTING = Set.of(
            "cameraman", "language", "updateCheck", "cameraOperators", "operatorSettings", "profiles",
            "targetFilters");

    @Test
    void profilesCoverEveryCameraSettingFromTheDefaultConfig() throws Exception {
        Set<String> configKeys = readTopLevelConfigKeys();
        Set<String> profileSettings = readProfileSettings();

        List<String> missing = new ArrayList<>();
        for (String key : configKeys) {
            if (!profileSettings.contains(key)) missing.add(key);
        }

        assertTrue(missing.isEmpty(), "settings missing from profile persistence: " + missing);
    }

    private Set<String> readTopLevelConfigKeys() throws Exception {
        Set<String> keys = new TreeSet<>();
        try (var resource = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertTrue(resource != null, "config.yml must be packaged");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || Character.isWhitespace(line.charAt(0)) || line.startsWith("#")) continue;
                Matcher matcher = TOP_LEVEL_KEY.matcher(line);
                if (!matcher.matches()) continue;
                String key = matcher.group(1);
                if (!NOT_A_CAMERA_SETTING.contains(key)) keys.add(key);
            }
        }
        return keys;
    }

    private Set<String> readProfileSettings() throws Exception {
        java.nio.file.Path source = java.nio.file.Path.of(
                "src/main/java/xyz/shibomb/cameraman/CameramanManager.java");
        String text = java.nio.file.Files.readString(source);
        int start = text.indexOf("PROFILE_SETTINGS = List.of(");
        assertTrue(start >= 0, "PROFILE_SETTINGS must exist in CameramanManager");
        int end = text.indexOf(");", start);
        String block = text.substring(start, end);

        Set<String> settings = new TreeSet<>();
        Matcher matcher = Pattern.compile("\"([A-Za-z][A-Za-z0-9_]*)\"").matcher(block);
        while (matcher.find()) settings.add(matcher.group(1));
        return settings;
    }
}
