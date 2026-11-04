package xyz.shibomb.cameraman;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Guards {@code messages.yml}: both languages must define the same keys, and every template must be
 * usable as a {@link String#format} pattern with the arguments the code passes to it.
 */
class MessagesBundleTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[-+0,#(]*\\d*(\\.\\d+)?([a-zA-Z%])");

    @Test
    void bothLanguagesDefineTheSameKeys() throws Exception {
        Map<String, String> ru = section("ru");
        Map<String, String> en = section("en");

        assertTrue(ru.keySet().equals(en.keySet()),
                "ru/en key mismatch; ru-only=" + difference(ru, en) + " en-only=" + difference(en, ru));
    }

    @Test
    void templatesAreValidFormatPatterns() throws Exception {
        List<String> broken = new ArrayList<>();
        for (Map.Entry<String, String> entry : section("ru").entrySet()) {
            String problem = validate(entry.getValue());
            if (problem != null) broken.add("ru." + entry.getKey() + ": " + problem);
        }
        for (Map.Entry<String, String> entry : section("en").entrySet()) {
            String problem = validate(entry.getValue());
            if (problem != null) broken.add("en." + entry.getKey() + ": " + problem);
        }
        assertTrue(broken.isEmpty(), "invalid message templates: " + broken);
    }

    /** A template is valid when its placeholders are well formed and numbering is consistent. */
    private static String validate(String template) {
        if (template == null) return "missing value";
        Deque<Integer> numbered = new ArrayDeque<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        int implicitIndex = 0;
        while (matcher.find()) {
            String conversion = matcher.group(3);
            if ("%".equals(conversion)) continue;
            if (matcher.group(1) != null) {
                numbered.add(Integer.parseInt(matcher.group(1).replace("$", "")));
            } else {
                implicitIndex++;
            }
        }
        if (!numbered.isEmpty() && implicitIndex > 0) {
            return "mixes explicit and implicit argument indexes";
        }
        // Every remaining '%' must be part of a placeholder or the escaped "%%" form.
        String withoutPlaceholders = template.replace("%%", "");
        if (withoutPlaceholders.indexOf('%') >= 0
                && !PLACEHOLDER.matcher(withoutPlaceholders).find()) {
            return "stray '%'";
        }
        return null;
    }

    private static List<String> difference(Map<String, String> left, Map<String, String> right) {
        return left.keySet().stream().filter(key -> !right.containsKey(key)).sorted().toList();
    }

    private static Map<String, String> section(String language) throws Exception {
        try (InputStream resource = MessagesBundleTest.class.getClassLoader()
                .getResourceAsStream("messages.yml")) {
            assertNotNull(resource, "messages.yml must be packaged at the JAR root");
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(
                    new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8));
            ConfigurationSection section = configuration.getConfigurationSection(language);
            assertNotNull(section, "missing language section: " + language);
            Map<String, String> values = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                values.put(key, section.getString(key));
            }
            return values;
        }
    }
}
