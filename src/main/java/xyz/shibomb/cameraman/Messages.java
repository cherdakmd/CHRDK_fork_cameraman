package xyz.shibomb.cameraman;

import java.io.File;
import java.util.Locale;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Loads the selected in-game language from messages.yml. */
public final class Messages {

    private final FileConfiguration messages;
    private final String language;

    public Messages(JavaPlugin plugin) {
        plugin.saveResource("messages.yml", false);
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        String configuredLanguage = plugin.getConfig().getString("language", "ru");
        if ("en".equalsIgnoreCase(configuredLanguage) || "ru".equalsIgnoreCase(configuredLanguage)) {
            this.language = configuredLanguage.toLowerCase(Locale.ROOT);
        } else {
            this.language = "ru";
            plugin.getLogger().warning("Unknown language '" + configuredLanguage + "'; using Russian.");
        }
    }

    public String get(String key, Object... arguments) {
        String pattern = messages.getString(language + "." + key,
                messages.getString("ru." + key, key));
        if (pattern == null) return key;
        if (arguments.length == 0) return pattern;
        return format(Locale.ROOT, pattern, arguments);
    }

    /**
     * Formats a template without ever throwing at the call site.
     *
     * <p>A stray {@code %} or a mismatched argument count in an administrator-edited
     * {@code messages.yml} used to raise {@link java.util.IllegalFormatException} inside command
     * handling, so the whole message was lost. This replaces placeholders positionally instead.</p>
     */
    static String format(Locale locale, String pattern, Object... arguments) {
        try {
            return String.format(locale, pattern, arguments);
        } catch (java.util.IllegalFormatException e) {
            return positionalFormat(pattern, arguments);
        }
    }

    /** Minimal {@code String.format} replacement used when the template is malformed. */
    static String positionalFormat(String pattern, Object... arguments) {
        StringBuilder result = new StringBuilder(pattern.length());
        int argumentIndex = 0;
        int index = 0;
        while (index < pattern.length()) {
            char current = pattern.charAt(index);
            if (current != '%') {
                result.append(current);
                index++;
                continue;
            }
            if (index + 1 < pattern.length() && pattern.charAt(index + 1) == '%') {
                result.append('%');
                index += 2;
                continue;
            }
            int conversionIndex = skipFormatSpecifier(pattern, index + 1);
            if (conversionIndex < 0) {
                // A lone '%' is rendered as plain text instead of failing the whole message.
                result.append('%');
                index++;
                continue;
            }
            result.append(argumentIndex < arguments.length ? arguments[argumentIndex] : "%"
                    + pattern.substring(index + 1, conversionIndex + 1));
            argumentIndex++;
            index = conversionIndex + 1;
        }
        return result.toString();
    }

    /** @return index of the conversion character, or {@code -1} when this is not a format specifier */
    private static int skipFormatSpecifier(String pattern, int start) {
        int index = start;
        while (index < pattern.length() && "-+ 0,#(".indexOf(pattern.charAt(index)) >= 0) index++;
        while (index < pattern.length() && (Character.isDigit(pattern.charAt(index))
                || pattern.charAt(index) == '$')) {
            index++;
        }
        if (index < pattern.length() && pattern.charAt(index) == '.') {
            index++;
            while (index < pattern.length() && Character.isDigit(pattern.charAt(index))) index++;
        }
        if (index >= pattern.length() || !Character.isLetter(pattern.charAt(index))) return -1;
        return index;
    }
}
