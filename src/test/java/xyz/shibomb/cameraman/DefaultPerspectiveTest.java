package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Guards the documented default perspectives for players and mobs. */
class DefaultPerspectiveTest {

    private static final Pattern DEFAULT_PERSPECTIVE = Pattern.compile(
            "^\\s*(spectatePerspective|mobSpectatePerspective)\\s*:\\s*([A-Za-z_]+)\\s*$");

    @Test
    void configDefaultsToPovForPlayersAndMobs() throws Exception {
        Map<String, String> values = new HashMap<>();
        try (var resource = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            assertNotNull(resource, "config.yml must be packaged at the JAR root");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = DEFAULT_PERSPECTIVE.matcher(line);
                    if (matcher.matches()) {
                        values.put(matcher.group(1), matcher.group(2));
                    }
                }
            }
        }

        assertEquals("POV", values.get("spectatePerspective"));
        assertEquals("POV", values.get("mobSpectatePerspective"));
    }

    @Test
    void povPerspectiveExistsForSelection() {
        assertEquals("POV", SpectatePerspective.POV.name());
    }
}
