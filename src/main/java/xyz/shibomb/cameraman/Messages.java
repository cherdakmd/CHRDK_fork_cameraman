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
        return arguments.length == 0 ? pattern : String.format(Locale.ROOT, pattern, arguments);
    }
}
