package xyz.shibomb.cameraman;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

/** Checks the latest stable release of this repository without blocking the server thread. */
public final class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/cherdakmd/CHRDK_fork_cameraman/releases/latest";
    private static final String RELEASES_URL = "https://github.com/cherdakmd/CHRDK_fork_cameraman/releases";
    private static final String TELEGRAM_URL = "https://t.me/cherdakmd";
    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"html_url\"\\s*:\\s*\"([^\"]+)\"");

    private final JavaPlugin plugin;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        if (!plugin.getConfig().getBoolean("updateCheck", true)) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::checkLatestRelease);
    }

    private void checkLatestRelease() {
        String installedVersion = readInstalledReleaseVersion();
        Version installed = Version.parse(installedVersion);
        if (installed == null) {
            plugin.getLogger().fine("Skipping update check: release build version is not embedded in this JAR.");
            return;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(5_000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Cameraman-UpdateChecker/26.3");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                plugin.getLogger().fine("GitHub update check returned HTTP " + status + ".");
                return;
            }

            String response;
            try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                response = scanner.hasNext() ? scanner.next() : "";
            }

            String tag = extract(TAG_PATTERN, response);
            Version latest = Version.parse(tag);
            if (latest == null || latest.compareTo(installed) <= 0) return;

            String releaseUrl = extract(URL_PATTERN, response);
            if (releaseUrl == null || !releaseUrl.startsWith("https://github.com/cherdakmd/CHRDK_fork_cameraman/")) {
                releaseUrl = RELEASES_URL;
            }
            plugin.getLogger().info("Доступна новая версия Cameraman " + tag + " (установлена " + installedVersion
                    + "). Скачать: " + releaseUrl + " | Telegram: " + TELEGRAM_URL);
        } catch (IOException | IllegalArgumentException e) {
            plugin.getLogger().fine("GitHub update check failed: " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readInstalledReleaseVersion() {
        try (var input = plugin.getResource("release.properties")) {
            if (input == null) return null;
            java.util.Properties properties = new java.util.Properties();
            properties.load(input);
            return properties.getProperty("release.version");
        } catch (IOException e) {
            plugin.getLogger().fine("Could not read embedded release version: " + e.getMessage());
            return null;
        }
    }

    private static String extract(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        return matcher.find() ? matcher.group(1) : null;
    }

    record Version(int major, int minor, int patch) implements Comparable<Version> {
        static Version parse(String value) {
            if (value == null) return null;
            String normalized = value.trim();
            if (normalized.startsWith("v") || normalized.startsWith("V")) normalized = normalized.substring(1);
            String[] parts = normalized.split("\\.");
            if (parts.length < 2 || parts.length > 3) return null;
            try {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = parts.length == 3 ? Integer.parseInt(parts[2]) : 0;
                if (major < 0 || minor < 0 || patch < 0) return null;
                return new Version(major, minor, patch);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public int compareTo(Version other) {
            int result = Integer.compare(major, other.major);
            if (result != 0) return result;
            result = Integer.compare(minor, other.minor);
            return result != 0 ? result : Integer.compare(patch, other.patch);
        }
    }
}
