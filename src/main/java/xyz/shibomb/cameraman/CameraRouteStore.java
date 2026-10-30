package xyz.shibomb.cameraman;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Persists named camera paths and supports portable YAML import/export. */
public final class CameraRouteStore {

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration routes;
    private final File scenesFile;
    private final YamlConfiguration scenes;

    public CameraRouteStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "routes.yml");
        this.routes = YamlConfiguration.loadConfiguration(file);
        this.scenesFile = new File(plugin.getDataFolder(), "scenes.yml");
        this.scenes = YamlConfiguration.loadConfiguration(scenesFile);
    }

    public boolean addPoint(String name, Location location) {
        return addPoints(name, location == null ? List.of() : List.of(location));
    }

    public boolean addPoints(String name, List<Location> locations) {
        if (!validName(name) || locations == null || locations.isEmpty()) return false;
        List<Map<?, ?>> points = new ArrayList<>(routes.getMapList(routePath(name)));
        for (Location location : locations) {
            if (location == null || location.getWorld() == null) continue;
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("world", location.getWorld().getName());
            point.put("x", location.getX());
            point.put("y", location.getY());
            point.put("z", location.getZ());
            point.put("yaw", location.getYaw());
            point.put("pitch", location.getPitch());
            points.add(point);
        }
        if (points.isEmpty()) return false;
        routes.set(routePath(name), points);
        return save();
    }

    public List<String> names() {
        ConfigurationSection section = routes.getConfigurationSection("routes");
        return section == null ? List.of() : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public List<Location> points(String name) {
        if (!validName(name)) return List.of();
        List<Location> result = new ArrayList<>();
        for (Map<?, ?> point : routes.getMapList(routePath(name))) {
            Object worldValue = point.get("world");
            World world = worldValue instanceof String worldName ? Bukkit.getWorld(worldName) : null;
            Double x = finiteNumber(point.get("x"));
            Double y = finiteNumber(point.get("y"));
            Double z = finiteNumber(point.get("z"));
            if (world == null || !validCoordinates(x, y, z)) continue;
            result.add(new Location(world, x, y, z,
                    safeYaw(point.get("yaw")), safePitch(point.get("pitch"))));
        }
        return List.copyOf(result);
    }

    public boolean delete(String name) {
        if (!validName(name) || !routes.contains(routePath(name))) return false;
        routes.set("routes." + name, null);
        return save();
    }

    public boolean exportRoute(String name) {
        if (!validName(name) || points(name).isEmpty()) return false;
        File exportDirectory = new File(plugin.getDataFolder(), "route-exports");
        if (!exportDirectory.exists() && !exportDirectory.mkdirs()) return false;
        YamlConfiguration exported = new YamlConfiguration();
        exported.set("name", name);
        exported.set("points", routes.getMapList(routePath(name)));
        try {
            exported.save(new File(exportDirectory, name + ".yml"));
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not export camera route '" + name + "': " + e.getMessage());
            return false;
        }
    }

    public boolean importRoute(String name) {
        if (!validName(name)) return false;
        File exportedFile = new File(new File(plugin.getDataFolder(), "route-exports"), name + ".yml");
        if (!exportedFile.isFile()) return false;
        YamlConfiguration imported = YamlConfiguration.loadConfiguration(exportedFile);
        List<Map<?, ?>> rawPoints = imported.getMapList("points");
        List<Map<String, Object>> validPoints = new ArrayList<>();
        for (Map<?, ?> point : rawPoints) {
            if (!(point.get("world") instanceof String worldName) || worldName.isBlank()) continue;
            Double x = finiteNumber(point.get("x"));
            Double y = finiteNumber(point.get("y"));
            Double z = finiteNumber(point.get("z"));
            if (!validCoordinates(x, y, z)) continue;
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("world", worldName);
            normalized.put("x", x);
            normalized.put("y", y);
            normalized.put("z", z);
            normalized.put("yaw", safeYaw(point.get("yaw")));
            normalized.put("pitch", safePitch(point.get("pitch")));
            validPoints.add(normalized);
        }
        if (validPoints.size() < 2) return false;
        routes.set(routePath(name), validPoints);
        return save();
    }

    public int pointCount(String name) {
        return validName(name) ? routes.getMapList(routePath(name)).size() : 0;
    }

    public List<String> sceneNames() {
        ConfigurationSection section = scenes.getConfigurationSection("scenes");
        return section == null ? List.of() : section.getKeys(false).stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public boolean addRouteToScene(String sceneName, String routeName) {
        if (!validName(sceneName) || pointCount(routeName) < 2) return false;
        List<String> routeNames = new ArrayList<>(scenes.getStringList("scenes." + sceneName + ".routes"));
        if (!routeNames.contains(routeName)) routeNames.add(routeName);
        scenes.set("scenes." + sceneName + ".routes", routeNames);
        return saveScenes();
    }

    public boolean deleteScene(String sceneName) {
        if (!validName(sceneName) || !scenes.contains("scenes." + sceneName)) return false;
        scenes.set("scenes." + sceneName, null);
        return saveScenes();
    }

    public List<Location> scenePoints(String sceneName) {
        List<Location> result = new ArrayList<>();
        for (String routeName : scenes.getStringList("scenes." + sceneName + ".routes")) {
            result.addAll(points(routeName));
        }
        return List.copyOf(result);
    }

    private boolean saveScenes() {
        try {
            scenes.save(scenesFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save camera scenes: " + e.getMessage());
            return false;
        }
    }

    private boolean save() {
        try {
            routes.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save camera routes: " + e.getMessage());
            return false;
        }
    }

    private static boolean validName(String name) {
        return InputValidators.isValidProfileName(name);
    }

    private static String routePath(String name) {
        return "routes." + name + ".points";
    }

    private static Double finiteNumber(Object value) {
        if (!(value instanceof Number number)) return null;
        double result = number.doubleValue();
        return Double.isFinite(result) ? result : null;
    }

    private static boolean validCoordinates(Double x, Double y, Double z) {
        return x != null && y != null && z != null
                && Math.abs(x) <= 30_000_000.0
                && Math.abs(z) <= 30_000_000.0
                && y >= -2048.0 && y <= 4096.0;
    }

    private static float safeYaw(Object value) {
        Double number = finiteNumber(value);
        return number == null ? 0.0F : (float) (number % 360.0);
    }

    private static float safePitch(Object value) {
        Double number = finiteNumber(value);
        return number == null ? 0.0F : (float) Math.max(-90.0, Math.min(90.0, number));
    }
}
