package xyz.shibomb.cameraman;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.shots.CraneShot;
import xyz.shibomb.cameraman.shots.DynamicFollowShot;
import xyz.shibomb.cameraman.shots.FlybyShot;
import xyz.shibomb.cameraman.shots.OrbitShot;
import xyz.shibomb.cameraman.shots.StaticShot;
import xyz.shibomb.cameraman.targets.CameraTarget;
import xyz.shibomb.cameraman.targets.EntityTarget;
import xyz.shibomb.cameraman.targets.LocationTarget;
import xyz.shibomb.cameraman.shots.MoveShot;

public class CameramanManager {

    public enum SpectateState {
        ENABLED, DISABLED, RANDOM
    }

    private final JavaPlugin plugin;
    private final Messages messages;
    private final ConfigurationSection config;
    private UUID cameramanId;
    private UUID currentTargetId;
    private boolean newcomerMode = false;
    private boolean rotationMode = false;
    private RotationTask rotationTask;
    private long rotationInterval = 200L; // Default 10 seconds (20 ticks * 10)
    private double minRotationDistance = 0.0;
    private boolean mobTargetMode = false;
    private boolean autoMobTarget = false;
    private long autoMobTargetDelay = 5L; // Seconds
    private long lastPlayerTargetTime = System.currentTimeMillis();
    private boolean teleportSmooth = false;
    private long teleportSmoothDuration = 3L; // Seconds
    private double teleportSmoothSpeed;
    private double cameraSmoothing = 0.35;
    private String cameraEasing = "SMOOTHSTEP";
    private boolean cameraHud = true;
    private boolean cameraPaused;
    private SmoothTeleportTask currentTeleportTask;
    private final CameraRouteStore routeStore;
    private CameraPathTask currentPathTask;
    private SpectateState spectateMode = SpectateState.ENABLED;
    private SpectatePerspective spectatePerspective = SpectatePerspective.POV;
    private SpectateState mobSpectateMode = SpectateState.ENABLED;
    private SpectatePerspective mobSpectatePerspective = SpectatePerspective.POV;
    private SpectateTask currentSpectateTask;
    private boolean mobNightVision = false;
    private boolean showMessage = true;
    private int nightVisionThreshold = 7;
    private org.bukkit.potion.PotionEffect previousNightVisionEffect = null;
    private long previousNightVisionTime = 0;
    private BukkitTask lightCheckTask;
    private String spectateDistance = "3.0";
    private String spectateHeight = "1.0";
    private String orbitSpeed = "0.2";
    private String orbitDirection = "RANDOM";
    private String dynamicSmoothness = "0.1";
    private String flybyDuration = "30.0";
    private String craneDuration = "30.0";
    private String craneHeightMin = "1.0";
    private String craneHeightMax = "5.0";
    private List<SpectatePerspective> randomPlayerPerspectives = new ArrayList<>();
    private List<SpectatePerspective> randomMobPerspectives = new ArrayList<>();
    private List<SpectatePerspective> randomScenicPerspectives = new ArrayList<>();
    private boolean autoScenic = false;
    private SpectatePerspective autoScenicPerspective = SpectatePerspective.ORBIT;
    private String moveX = "-1-1";
    private String moveY = "0";
    private String moveZ = "-1-1";
    private String moveSpeed = "0.1";

    public CameramanManager(JavaPlugin plugin) {
        this(plugin, null);
    }

    public CameramanManager(JavaPlugin plugin, UUID operatorId) {
        this.plugin = plugin;
        this.messages = new Messages(plugin);
        this.routeStore = plugin instanceof CameramanPlugin cameramanPlugin
                && cameramanPlugin.getRouteStore() != null
                        ? cameramanPlugin.getRouteStore() : new CameraRouteStore(plugin);
        if (operatorId == null) {
            this.config = plugin.getConfig();
            String uuidString = config.getString("cameraman");
            if (uuidString != null) {
                try {
                    this.cameramanId = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Некорректный UUID оператора в конфигурации: " + uuidString);
                }
            }
        } else {
            this.cameramanId = operatorId;
            String sectionPath = "operatorSettings." + operatorId;
            ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionPath);
            if (section == null) {
                section = plugin.getConfig().createSection(sectionPath);
            }
            if (section.getKeys(false).isEmpty()) {
                for (String key : plugin.getConfig().getKeys(false)) {
                    if (List.of("cameraman", "language", "cameraOperators", "operatorSettings", "profiles").contains(key)) {
                        continue;
                    }
                    ConfigurationSection nested = plugin.getConfig().getConfigurationSection(key);
                    section.set(key, nested == null ? plugin.getConfig().get(key) : nested.getValues(false));
                }
                plugin.saveConfig();
            }
            this.config = section;
        }
        loadSettingsFromConfig();
    }

    private void loadSettingsFromConfig() {
        this.newcomerMode = config.getBoolean("newcomerMode", false);
        this.rotationMode = config.getBoolean("rotationMode", false);
        long intervalSeconds = config.getLong("rotationInterval", 60L);
        this.rotationInterval = intervalSeconds * 20L;
        this.minRotationDistance = config.getDouble("minRotationDistance", 0.0);

        this.mobTargetMode = config.getBoolean("mobTargetMode", false);
        this.autoMobTarget = config.getBoolean("autoMobTarget", false);
        this.autoMobTargetDelay = config.getLong("autoMobTargetDelay", 60L);

        this.teleportSmooth = config.getBoolean("teleportSmooth", false);
        this.teleportSmoothDuration = config.getLong("teleportSmoothDuration", 3L);
        this.teleportSmoothSpeed = config.getDouble("teleportSmoothSpeed", 50.0);
        this.cameraSmoothing = Math.max(0.01, Math.min(1.0, config.getDouble("cameraSmoothing", 0.35)));
        String configuredEasing = config.getString("cameraEasing", "SMOOTHSTEP");
        this.cameraEasing = configuredEasing != null
                && List.of("LINEAR", "SMOOTHSTEP", "EASE_IN_OUT").contains(configuredEasing.toUpperCase(Locale.ROOT))
                        ? configuredEasing.toUpperCase(Locale.ROOT) : "SMOOTHSTEP";
        this.cameraHud = config.getBoolean("cameraHud", true);

        String spectateModeStr = config.getString("spectateMode", "true");
        this.spectateMode = parseSpectateState(spectateModeStr);
        String perspectiveStr = config.getString("spectatePerspective", "RANDOM");
        try {
            this.spectatePerspective = SpectatePerspective.valueOf(perspectiveStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.spectatePerspective = SpectatePerspective.POV;
        }

        String mobSpectateModeStr = config.getString("mobSpectateMode", "true");
        this.mobSpectateMode = parseSpectateState(mobSpectateModeStr);
        String mobPerspectiveStr = config.getString("mobSpectatePerspective", "RANDOM");
        try {
            this.mobSpectatePerspective = SpectatePerspective.valueOf(mobPerspectiveStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.mobSpectatePerspective = SpectatePerspective.POV;
        }

        this.mobNightVision = config.getBoolean("mobNightVision", true);
        this.showMessage = config.getBoolean("showMessage", true);
        this.nightVisionThreshold = config.getInt("nightVisionThreshold", 7);
        this.spectateDistance = config.getString("spectateDistance", "3.0");
        this.spectateHeight = config.getString("spectateHeight", "1.0");
        this.orbitSpeed = config.getString("orbitSpeed", "0.1");
        this.orbitDirection = config.getString("orbitDirection", "RANDOM");
        this.dynamicSmoothness = config.getString("dynamicSmoothness", "0.05");
        this.flybyDuration = config.getString("flybyDuration", "30.0");
        this.craneDuration = config.getString("craneDuration", "30.0");
        this.craneHeightMin = config.getString("craneHeightMin", "1.0");
        this.craneHeightMax = config.getString("craneHeightMax", "5.0");
        this.moveX = config.getString("moveX", "-1-1");
        this.moveY = config.getString("moveY", "0");
        this.moveZ = config.getString("moveZ", "-1-1");
        this.moveSpeed = config.getString("moveSpeed", "0.1");

        this.autoScenic = config.getBoolean("autoScenic", false);
        String autoScenicPerspStr = config.getString("autoScenicPerspective", "ORBIT");
        try {
            this.autoScenicPerspective = SpectatePerspective.valueOf(autoScenicPerspStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.autoScenicPerspective = SpectatePerspective.ORBIT;
        }

        loadRandomPerspectives();

        checkAndStartRotationTask();
    }

    private SpectateState parseSpectateState(String value) {
        if (value.equalsIgnoreCase("true"))
            return SpectateState.ENABLED;
        if (value.equalsIgnoreCase("false"))
            return SpectateState.DISABLED;
        try {
            return SpectateState.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SpectateState.ENABLED;
        }
    }

    private void loadRandomPerspectives() {
        randomPlayerPerspectives = loadPerspectiveList("randomPlayerPerspectives");
        randomMobPerspectives = loadPerspectiveList("randomMobPerspectives");
        randomScenicPerspectives = loadPerspectiveList("randomScenicPerspectives");
    }

    private List<SpectatePerspective> loadPerspectiveList(String path) {
        List<String> list = config.getStringList(path);
        if (list == null || list.isEmpty()) {
            // Default fallback if missing
            List<SpectatePerspective> defaults = new ArrayList<>();
            defaults.add(SpectatePerspective.POV);
            defaults.add(SpectatePerspective.BEHIND);
            defaults.add(SpectatePerspective.FRONT);
            defaults.add(SpectatePerspective.ORBIT);
            defaults.add(SpectatePerspective.DYNAMIC);
            defaults.add(SpectatePerspective.DYNAMIC_BEHIND);
            defaults.add(SpectatePerspective.DYNAMIC_FRONT);
            defaults.add(SpectatePerspective.DYNAMIC_POV);
            defaults.add(SpectatePerspective.FLYBY);
            defaults.add(SpectatePerspective.CRANE);
            defaults.add(SpectatePerspective.FIX);
            return defaults;
        }
        return list.stream().map(s -> {
            try {
                return SpectatePerspective.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Некорректный ракурс в конфигурации: " + s);
                return null;
            }
        }).filter(p -> p != null && p != SpectatePerspective.RANDOM).collect(Collectors.toList());
    }

    private void checkAndStartRotationTask() {
        boolean shouldRun = rotationMode || mobTargetMode || autoMobTarget;

        // Always stop the existing task to ensure we use the latest interval/settings
        stopRotationTask();

        if (shouldRun) {
            this.rotationTask = new RotationTask(this);
            this.rotationTask.runTaskTimer(plugin, 0L, this.rotationInterval);
        }
    }

    public void setCameraman(Player player) {
        if (this.cameramanId != null && !this.cameramanId.equals(player.getUniqueId())) {
            Player oldCameraman = Bukkit.getPlayer(this.cameramanId);
            if (oldCameraman != null) {
                oldCameraman.setGameMode(GameMode.SURVIVAL);
                oldCameraman.sendMessage(messages.get("no-longer-cameraman"));
            }
        }
        this.cameramanId = player.getUniqueId();
        config.set("cameraman", this.cameramanId.toString());
        plugin.saveConfig();
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(messages.get("now-cameraman"));
    }

    public Player getCameraman() {
        if (cameramanId == null)
            return null;
        return Bukkit.getPlayer(cameramanId);
    }

    public void restoreCameraman(Player player) {
        if (cameramanId != null && cameramanId.equals(player.getUniqueId())) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(messages.get("welcome-cameraman"));

        }
    }

    public void removeCameraman() {
        if (cameramanId != null) {
            Player cameraman = Bukkit.getPlayer(cameramanId);
            if (cameraman != null) {
                cameraman.setGameMode(GameMode.SURVIVAL);
                cameraman.sendMessage(messages.get("no-longer-cameraman"));
            }
            cameramanId = null;
            config.set("cameraman", null);
            plugin.saveConfig();
        }
    }

    public Messages getMessages() {
        return messages;
    }

    private static final List<String> PROFILE_SETTINGS = List.of(
            "newcomerMode", "rotationMode", "rotationInterval", "minRotationDistance", "mobTargetMode",
            "autoMobTarget", "autoMobTargetDelay", "teleportSmooth", "teleportSmoothDuration",
            "teleportSmoothSpeed", "cameraSmoothing", "cameraEasing", "cameraHud", "spectateMode", "spectatePerspective", "mobSpectateMode",
            "mobSpectatePerspective", "mobNightVision", "showMessage", "nightVisionThreshold",
            "spectateDistance", "spectateHeight", "orbitSpeed", "orbitDirection", "dynamicSmoothness",
            "flybyDuration", "craneDuration", "craneHeightMin", "craneHeightMax", "moveX", "moveY",
            "moveZ", "moveSpeed", "autoScenic", "autoScenicPerspective", "randomPlayerPerspectives",
            "randomMobPerspectives", "randomScenicPerspectives");

    private boolean isValidProfileName(String name) {
        return InputValidators.isValidProfileName(name);
    }

    public boolean saveProfile(String name) {
        if (!isValidProfileName(name)) return false;
        String path = "profiles." + name;
        for (String setting : PROFILE_SETTINGS) {
            plugin.getConfig().set(path + "." + setting, config.get(setting));
        }
        plugin.saveConfig();
        return true;
    }

    public boolean renameProfile(String oldName, String newName) {
        if (!isValidProfileName(oldName) || !isValidProfileName(newName)
                || !plugin.getConfig().isConfigurationSection("profiles." + oldName)) {
            return false;
        }
        if (oldName.equals(newName)) return true;
        if (plugin.getConfig().isConfigurationSection("profiles." + newName)) return false;
        String oldPath = "profiles." + oldName + ".";
        String newPath = "profiles." + newName + ".";
        for (String setting : PROFILE_SETTINGS) {
            String settingPath = oldPath + setting;
            if (plugin.getConfig().contains(settingPath)) {
                plugin.getConfig().set(newPath + setting, plugin.getConfig().get(settingPath));
            }
        }
        plugin.getConfig().set("profiles." + oldName, null);
        plugin.saveConfig();
        return true;
    }

    public boolean loadProfile(String name) {
        if (!isValidProfileName(name) || !plugin.getConfig().isConfigurationSection("profiles." + name)) {
            return false;
        }
        for (String setting : PROFILE_SETTINGS) {
            if (plugin.getConfig().contains("profiles." + name + "." + setting)) {
                config.set(setting, plugin.getConfig().get("profiles." + name + "." + setting));
            }
        }
        plugin.saveConfig();
        loadSettingsFromConfig();
        checkAndStartRotationTask();
        return true;
    }

    public boolean deleteProfile(String name) {
        if (!isValidProfileName(name) || !plugin.getConfig().isConfigurationSection("profiles." + name)) {
            return false;
        }
        plugin.getConfig().set("profiles." + name, null);
        plugin.saveConfig();
        return true;
    }

    public List<String> getProfileNames() {
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("profiles");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public void sendInfoMessage(Player player, String message) {
        if (showMessage && player != null) {
            player.sendMessage(message);
        }
    }

    private String getTargetDisplayName(org.bukkit.entity.Entity target) {
        String displayName = target instanceof Player player ? player.getDisplayName() : target.getCustomName();
        if (displayName == null || displayName.isBlank()) displayName = target.getName();
        String withoutColors = org.bukkit.ChatColor.stripColor(displayName);
        return withoutColors == null || withoutColors.isBlank() ? displayName : withoutColors;
    }

    public String getCurrentTargetDisplayName() {
        org.bukkit.entity.Entity target = currentTargetId == null ? null : Bukkit.getEntity(currentTargetId);
        return target == null ? null : getTargetDisplayName(target);
    }

    public void setTarget(org.bukkit.entity.Entity target) {
        Player cameraman = getCameraman();
        if (cameraman != null && target != null) {
            if (!isTargetAllowed(target)) {
                sendInfoMessage(cameraman, messages.get("target-not-allowed"));
                return;
            }
            // Clean up previous night vision states first
            stopLightCheckTask();
            stopSpectateTask();
            removeCameramanNightVision(cameraman);

            if (target instanceof Player) {
                if (cameraman.getUniqueId().equals(target.getUniqueId())) {
                    return; // Невозможно наблюдать за self
                }
                // If targeting a player, disable mob target mode (conceptually, we are focusing
                // on a player now)
                if (mobTargetMode) {
                    setMobTargetMode(false);
                    sendInfoMessage(cameraman, messages.get("mob-mode-disabled-player-target"));
                }
                lastPlayerTargetTime = System.currentTimeMillis();
            }

            if (cameraman.getGameMode() != GameMode.SPECTATOR) {
                sendInfoMessage(cameraman,
                        messages.get("cannot-spectate-not-spectator", target.getName()));
                return;
            }
            currentTargetId = target.getUniqueId();
            sendInfoMessage(cameraman, messages.get("target-switched", getTargetDisplayName(target)));

            stopPathPlayback();
            if (currentTeleportTask != null && !currentTeleportTask.isCancelled()) {
                currentTeleportTask.cancel();
            }

            cameraman.setSpectatorTarget(null); // Reset first

            // Determine active perspective
            SpectatePerspective configPerspective;
            if (target instanceof Player) {
                configPerspective = spectatePerspective;
            } else {
                configPerspective = mobSpectatePerspective;
            }

            SpectatePerspective activePerspective;
            if ("RANDOM".equalsIgnoreCase(configPerspective.name())) {
                List<SpectatePerspective> pool;
                if (target instanceof LocationTarget) {
                    pool = randomScenicPerspectives;
                } else if (target instanceof Player) {
                    pool = randomPlayerPerspectives;
                } else {
                    pool = randomMobPerspectives;
                }

                if (pool.isEmpty()) {
                    activePerspective = SpectatePerspective.POV;
                } else {
                    activePerspective = pool.get(new Random().nextInt(pool.size()));
                }
            } else {
                activePerspective = configPerspective;
            }

            double maxDist = parseAndCalculate(spectateDistance);
            double maxHeight = parseAndCalculate(spectateHeight);
            boolean activeSpectateMode = target instanceof Player
                    ? resolveSpectateState(spectateMode)
                    : resolveSpectateState(mobSpectateMode);
            CameraShot shot = createCameraShot(activePerspective, maxDist, maxHeight);
            EntityTarget cameraTarget = new EntityTarget(target);

            Runnable onTargetSet = () -> {
                if (activeSpectateMode) {
                    if (activePerspective == SpectatePerspective.POV) {
                        cameraman.setSpectatorTarget(target);
                        sendInfoMessage(cameraman, messages.get("now-spectating-pov", target.getName()));
                    } else {
                        cameraman.setSpectatorTarget(null);
                        startSpectateTask(cameraman, cameraTarget, shot, activePerspective);
                        sendInfoMessage(cameraman,
                                messages.get("now-spectating-perspective", target.getName(), activePerspective));
                    }
                } else {
                    sendInfoMessage(cameraman, messages.get("arrived-at", target.getName()));
                    if (autoScenic) {
                        startScenicTask(target.getLocation(), autoScenicPerspective);
                    }
                }

                if (mobNightVision && target instanceof org.bukkit.entity.LivingEntity && !(target instanceof Player)) {
                    startLightCheckTask(cameraman, (org.bukkit.entity.LivingEntity) target);
                }
            };

            boolean canInterpolate = teleportSmooth
                    && cameraman.getWorld().equals(target.getWorld())
                    && !(activeSpectateMode && activePerspective == SpectatePerspective.POV);
            if (canInterpolate) {
                sendInfoMessage(cameraman, messages.get("moving-to", target.getName()));
                double distanceToTarget = cameraman.getLocation().distance(target.getLocation());
                double calculatedDuration = distanceToTarget / Math.max(1.0, teleportSmoothSpeed);
                long finalDuration = (long) Math.max(teleportSmoothDuration, calculatedDuration);
                stopSpectateTask();
                currentTeleportTask = new SmoothTeleportTask(cameraman, cameraTarget, finalDuration * 20L,
                        onTargetSet, shot, cameraEasing, this::isCameraPaused);
                currentTeleportTask.runTaskTimer(plugin, 0L, 1L);
                return;
            }

            if (activeSpectateMode) {
                onTargetSet.run();
            } else {
                Location targetLoc = shot.getNextLocation(cameraman, cameraTarget, 0);
                cameraman.teleport(targetLoc);
                sendInfoMessage(cameraman, messages.get("moved-to", target.getName()));
                if (autoScenic) {
                    startScenicTask(target.getLocation(), autoScenicPerspective);
                }
                if (mobNightVision && target instanceof org.bukkit.entity.LivingEntity && !(target instanceof Player)) {
                    startLightCheckTask(cameraman, (org.bukkit.entity.LivingEntity) target);
                }
            }
        }
    }

    public void clearTarget() {
        currentTargetId = null;
        Player cameraman = getCameraman();
        if (cameraman != null) {
            cameraman.setSpectatorTarget(null);
            stopLightCheckTask();
            stopSpectateTask();
            stopPathPlayback();
            if (currentTeleportTask != null && !currentTeleportTask.isCancelled()) {
                currentTeleportTask.cancel();
                currentTeleportTask = null;
            }
            removeCameramanNightVision(cameraman);
            sendInfoMessage(cameraman, messages.get("stopped-spectating"));
        }
    }

    private void startLightCheckTask(Player cameraman, org.bukkit.entity.LivingEntity target) {
        stopLightCheckTask();
        lightCheckTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (cameraman == null || !cameraman.isOnline() || target == null || !target.isValid()) {
                    this.cancel();
                    return;
                }

                int lightLevel = target.getLocation().getBlock().getLightLevel();
                if (lightLevel <= nightVisionThreshold) {
                    applyCameramanNightVision(cameraman);
                } else {
                    removeCameramanNightVision(cameraman);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }

    private void stopLightCheckTask() {
        if (lightCheckTask != null && !lightCheckTask.isCancelled()) {
            lightCheckTask.cancel();
            lightCheckTask = null;
        }
    }

    private void applyCameramanNightVision(Player player) {
        org.bukkit.potion.PotionEffect current = player
                .getPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        // If current is null or not our infinite one (duration < 1000000 is a safe bet
        // for "not infinite")
        // We assume our infinite one is Integer.MAX_VALUE
        if (current != null && current.getDuration() < 1000000) {
            previousNightVisionEffect = current;
            previousNightVisionTime = System.currentTimeMillis();
        }

        // If we don't have a current effect, or we just saved it, apply ours
        if (current == null || current.getDuration() < 1000000) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION,
                    Integer.MAX_VALUE, 0, false, false));
        }
    }

    private void removeCameramanNightVision(Player player) {
        // First, check if we have the infinite effect. If so, remove it.
        org.bukkit.potion.PotionEffect current = player
                .getPotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        if (current != null && current.getDuration() > 1000000) {
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        }

        // Restore previous effect if it exists
        if (previousNightVisionEffect != null) {
            long elapsedTicks = (System.currentTimeMillis() - previousNightVisionTime) / 50;
            int remaining = previousNightVisionEffect.getDuration() - (int) elapsedTicks;

            if (remaining > 0) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        previousNightVisionEffect.getType(),
                        remaining,
                        previousNightVisionEffect.getAmplifier(),
                        previousNightVisionEffect.isAmbient(),
                        previousNightVisionEffect.hasParticles(),
                        previousNightVisionEffect.hasIcon()));
            }
            previousNightVisionEffect = null;
        }
    }

    public void setNewcomerMode(boolean enabled) {
        this.newcomerMode = enabled;
        config.set("newcomerMode", enabled);
        plugin.saveConfig();
        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("newcomer-mode-set", enabled));
        }
    }

    public List<String> getCameraRouteNames() {
        return routeStore.names();
    }

    public int getCameraRoutePointCount(String name) {
        return routeStore.pointCount(name);
    }

    public boolean addCameraRoutePoint(String name) {
        Player cameraman = getCameraman();
        return cameraman != null && routeStore.addPoint(name, cameraman.getLocation());
    }

    public boolean deleteCameraRoute(String name) {
        return routeStore.delete(name);
    }

    public boolean exportCameraRoute(String name) {
        return routeStore.exportRoute(name);
    }

    public boolean importCameraRoute(String name) {
        return routeStore.importRoute(name);
    }

    public boolean playCameraRoute(String name, long ticksPerSegment) {
        return playCameraPath(routeStore.points(name), ticksPerSegment,
                cameraHud ? messages.get("camera-path-hud", name) : null);
    }

    public List<String> getCameraSceneNames() {
        return routeStore.sceneNames();
    }

    public boolean addRouteToScene(String sceneName, String routeName) {
        return routeStore.addRouteToScene(sceneName, routeName);
    }

    public boolean deleteCameraScene(String sceneName) {
        return routeStore.deleteScene(sceneName);
    }

    public boolean playCameraScene(String name, long ticksPerSegment) {
        return playCameraPath(routeStore.scenePoints(name), ticksPerSegment,
                cameraHud ? messages.get("camera-scene-hud", name) : null);
    }

    private boolean playCameraPath(List<Location> points, long ticksPerSegment, String hudMessage) {
        Player cameraman = getCameraman();
        if (cameraman == null || points.size() < 2) return false;
        stopFollowing();
        stopPathPlayback();
        cameraman.setSpectatorTarget(null);
        currentPathTask = new CameraPathTask(cameraman, points, ticksPerSegment, cameraEasing,
                this::isCameraPaused, hudMessage);
        currentPathTask.runTaskTimer(plugin, 0L, 1L);
        return true;
    }

    public boolean stopPathPlayback() {
        if (currentPathTask == null || currentPathTask.isCancelled()) {
            currentPathTask = null;
            return false;
        }
        currentPathTask.cancel();
        currentPathTask = null;
        return true;
    }

    public boolean isCameraPaused() {
        return cameraPaused;
    }

    public boolean toggleCameraPause() {
        cameraPaused = !cameraPaused;
        return cameraPaused;
    }

    public boolean advanceTarget() {
        List<? extends org.bukkit.entity.Entity> targets = mobTargetMode
                ? getPotentialMobTargets() : getPotentialTargets();
        if (targets.isEmpty()) return false;
        int currentIndex = -1;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getUniqueId().equals(currentTargetId)) {
                currentIndex = i;
                break;
            }
        }
        setTarget(targets.get((currentIndex + 1) % targets.size()));
        return true;
    }

    public boolean isNewcomerMode() {
        return newcomerMode;
    }

    public boolean isRotationMode() {
        return rotationMode;
    }

    public long getRotationIntervalSeconds() {
        return rotationInterval / 20L;
    }

    public boolean isTeleportSmooth() {
        return teleportSmooth;
    }

    public SpectatePerspective getSpectatePerspective() {
        return spectatePerspective;
    }

    public SpectatePerspective getMobSpectatePerspective() {
        return mobSpectatePerspective;
    }

    public String getSpectateDistance() {
        return spectateDistance;
    }

    public String getSpectateHeight() {
        return spectateHeight;
    }

    public void adjustSpectateDistance(double delta) {
        setSpectateDistance(adjustRange(spectateDistance, delta, 0.5));
    }

    public void adjustSpectateHeight(double delta) {
        setSpectateHeight(adjustRange(spectateHeight, delta, 0.0));
    }

    private String adjustRange(String value, double delta, double minimum) {
        try {
            String[] parts = value.split("-", 2);
            double low = Double.parseDouble(parts[0].trim());
            double high = parts.length == 2 ? Double.parseDouble(parts[1].trim()) : low;
            low = Math.max(minimum, low + delta);
            high = Math.max(low, high + delta);
            return parts.length == 2
                    ? String.format(Locale.ROOT, "%.1f-%.1f", low, high)
                    : String.format(Locale.ROOT, "%.1f", low);
        } catch (NumberFormatException e) {
            return String.format(Locale.ROOT, "%.1f", Math.max(minimum, delta));
        }
    }

    public long getTeleportSmoothDuration() {
        return teleportSmoothDuration;
    }

    public double getCameraSmoothing() {
        return cameraSmoothing;
    }

    public boolean isCameraHud() {
        return cameraHud;
    }

    public String getCameraEasing() {
        return cameraEasing;
    }

    public void setRotationMode(boolean enabled, long intervalSeconds) {
        this.rotationMode = enabled;

        config.set("rotationMode", enabled);
        config.set("rotationInterval", intervalSeconds);
        this.rotationInterval = intervalSeconds * 20L;
        plugin.saveConfig();

        checkAndStartRotationTask();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("rotation-mode-set", enabled, intervalSeconds));
        }
    }

    public void stopRotationTask() {
        if (this.rotationTask != null && !this.rotationTask.isCancelled()) {
            this.rotationTask.cancel();
        }
    }

    public void stopAllTasks() {
        stopRotationTask();
        stopLightCheckTask();
        stopSpectateTask();
        if (currentTeleportTask != null && !currentTeleportTask.isCancelled()) {
            currentTeleportTask.cancel();
            currentTeleportTask = null;
        }
        stopFollowing();
    }

    public List<Player> getPotentialTargets() {
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (cameramanId != null) {
            targets.removeIf(player -> player.getUniqueId().equals(cameramanId));
        }
        targets.removeIf(player -> player.getGameMode() == GameMode.SPECTATOR || !isTargetAllowed(player));
        return targets;
    }

    private boolean isTargetAllowed(org.bukkit.entity.Entity entity) {
        List<String> allowedWorlds = config.getStringList("targetFilters.allowedWorlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(entity.getWorld().getName())) {
            return false;
        }

        if (entity instanceof Player player) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                return false;
            }
            if (config.getStringList("targetFilters.excludedPlayers").stream()
                    .anyMatch(name -> name.equalsIgnoreCase(player.getName()))) {
                return false;
            }
            if (config.getStringList("targetFilters.excludedPermissions").stream()
                    .anyMatch(player::hasPermission)) {
                return false;
            }
            if (config.getBoolean("targetFilters.excludeInvisible", false)
                    && player.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)) {
                return false;
            }
            if (config.getStringList("targetFilters.excludedGameModes").stream()
                    .anyMatch(mode -> mode.equalsIgnoreCase(player.getGameMode().name()))) {
                return false;
            }
        }

        Player cameraman = getCameraman();
        if (cameraman != null) {
            double minDistance = config.getDouble("targetFilters.minDistance", 0.0);
            double maxDistance = config.getDouble("targetFilters.maxDistance", 0.0);
            if ((minDistance > 0.0 || maxDistance > 0.0)
                    && !cameraman.getWorld().equals(entity.getWorld())) {
                return false;
            }
            if (cameraman.getWorld().equals(entity.getWorld())) {
                double distance = cameraman.getLocation().distance(entity.getLocation());
                if (distance < minDistance || (maxDistance > 0.0 && distance > maxDistance)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void handleTargetUnavailable(UUID targetId) {
        if (targetId == null || !targetId.equals(currentTargetId)) {
            return;
        }
        currentTargetId = null;
        clearTarget();
        List<? extends org.bukkit.entity.Entity> replacements;
        if (mobTargetMode) {
            replacements = getPotentialMobTargets();
        } else {
            replacements = getPotentialTargets();
        }
        if (!replacements.isEmpty()) {
            setTarget(replacements.get(0));
        }
    }

    public void refreshPlayerTarget(Player player) {
        if (player != null && player.getUniqueId().equals(currentTargetId) && isTargetAllowed(player)) {
            setTarget(player);
        } else if (player != null && player.getUniqueId().equals(currentTargetId)) {
            handleTargetUnavailable(player.getUniqueId());
        }
    }

    public void scheduleTargetUnavailable(UUID targetId) {
        Bukkit.getScheduler().runTask(plugin, () -> handleTargetUnavailable(targetId));
    }

    public void setMobTargetMode(boolean enabled) {
        this.mobTargetMode = enabled;
        config.set("mobTargetMode", enabled);
        plugin.saveConfig();

        checkAndStartRotationTask();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("mob-target-mode-set", enabled));
        }
    }

    public boolean isMobTargetMode() {
        return mobTargetMode;
    }

    public void setAutoMobTarget(boolean enabled, long delay) {
        this.autoMobTarget = enabled;
        this.autoMobTargetDelay = delay;
        config.set("autoMobTarget", enabled);
        config.set("autoMobTargetDelay", delay);
        plugin.saveConfig();

        checkAndStartRotationTask();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("auto-mob-target-set", enabled, delay));
        }
    }

    public boolean isAutoMobTarget() {
        return autoMobTarget;
    }

    public long getAutoMobTargetDelay() {
        return autoMobTargetDelay;
    }

    public long getLastPlayerTargetTime() {
        return lastPlayerTargetTime;
    }

    public List<org.bukkit.entity.LivingEntity> getPotentialMobTargets() {
        Player cameraman = getCameraman();
        List<org.bukkit.entity.LivingEntity> mobs = new ArrayList<>();
        if (cameraman != null) {
            for (org.bukkit.entity.Entity entity : cameraman.getNearbyEntities(50, 50, 50)) {
                if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)
                        && !(entity instanceof org.bukkit.entity.ArmorStand) && isTargetAllowed(entity)) {
                    mobs.add((org.bukkit.entity.LivingEntity) entity);
                }
            }
        }
        return mobs;
    }

    public void setTeleportSmooth(boolean enabled, long duration) {
        this.teleportSmooth = enabled;
        this.teleportSmoothDuration = duration;
        config.set("teleportSmooth", enabled);
        config.set("teleportSmoothDuration", duration);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("teleport-smooth-set", enabled, duration));
        }
    }

    private boolean resolveSpectateState(SpectateState state) {
        if (state == SpectateState.RANDOM) {
            return new Random().nextBoolean();
        }
        return state == SpectateState.ENABLED;
    }

    public void setSpectateMode(String value) {
        this.spectateMode = parseSpectateState(value);
        config.set("spectateMode", this.spectateMode.name()); // Save as name if enum, or maybe preserve
                                                                          // true/false string?
        // To maintain backward compatibility somewhat visually in config, we could save
        // "true"/"false" for enabled/disabled
        // But simplifying to enum names is cleaner. Let's start with enum names.
        // Actually, if I want to support "true"/"false" inputs for command, I should
        // handle saving.
        // Let's just save the name or "true"/"false" if that's what was passed?
        // For simplicity:
        if (this.spectateMode == SpectateState.ENABLED)
            config.set("spectateMode", "true");
        else if (this.spectateMode == SpectateState.DISABLED)
            config.set("spectateMode", "false");
        else
            config.set("spectateMode", "RANDOM");

        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("spectate-mode-set", this.spectateMode == SpectateState.RANDOM ? "RANDOM"
                    : (this.spectateMode == SpectateState.ENABLED)));
        }
    }

    public void setSpectatePerspective(SpectatePerspective perspective) {
        this.spectatePerspective = perspective;
        config.set("spectatePerspective", perspective.name());
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("spectate-player-perspective-set", perspective));
        }
    }

    public void setMobSpectateMode(String value) {
        this.mobSpectateMode = parseSpectateState(value);
        if (this.mobSpectateMode == SpectateState.ENABLED)
            config.set("mobSpectateMode", "true");
        else if (this.mobSpectateMode == SpectateState.DISABLED)
            config.set("mobSpectateMode", "false");
        else
            config.set("mobSpectateMode", "RANDOM");

        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, 
                    messages.get("spectate-mode-set", this.mobSpectateMode == SpectateState.RANDOM ? "RANDOM"
                            : (this.mobSpectateMode == SpectateState.ENABLED)));
        }
    }

    public void setMobSpectatePerspective(SpectatePerspective perspective) {
        this.mobSpectatePerspective = perspective;
        config.set("mobSpectatePerspective", perspective.name());
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("spectate-mob-perspective-set", perspective));
        }
    }

    private void startSpectateTask(Player cameraman, CameraTarget target, CameraShot shot,
            SpectatePerspective perspective) {
        if (target == null || !target.isValid()) return;

        stopSpectateTask();
        String hudMessage = cameraHud ? messages.get("camera-hud", target.getName(), perspective.name()) : null;
        currentSpectateTask = new SpectateTask(cameraman, target, shot, getSmoothingFor(perspective), hudMessage,
                this::isCameraPaused);
        currentSpectateTask.runTaskTimer(plugin, 0L, 1L);
    }

    private double getSmoothingFor(SpectatePerspective perspective) {
        if (perspective == SpectatePerspective.DYNAMIC || perspective == SpectatePerspective.DYNAMIC_BEHIND
                || perspective == SpectatePerspective.DYNAMIC_FRONT || perspective == SpectatePerspective.DYNAMIC_POV) {
            return Math.max(0.01, Math.min(1.0, Double.parseDouble(dynamicSmoothness)));
        }
        return cameraSmoothing;
    }

    public void startScenicTask(Location startLocation, SpectatePerspective perspective) {
        Player cameraman = getCameraman();
        if (cameraman == null)
            return;

        // Ensure Spectator mode
        if (cameraman.getGameMode() != GameMode.SPECTATOR) {
            sendInfoMessage(cameraman, messages.get("need-spectator-for-scenic"));
            return;
        }

        currentTargetId = null;
        // Resolve RANDOM if needed
        SpectatePerspective actualPerspective = perspective;
        if (perspective == SpectatePerspective.RANDOM) {
            if (!randomScenicPerspectives.isEmpty()) {
                actualPerspective = randomScenicPerspectives
                        .get(new Random().nextInt(randomScenicPerspectives.size()));
            } else {
                actualPerspective = SpectatePerspective.ORBIT; // Default fallback
            }
        }

        // Display the resolved perspective
        sendInfoMessage(cameraman, messages.get("scenic-perspective", actualPerspective.name()));

        // Parse configs directly or reuse existing fields?
        // We can reuse the fields.
        double distance = parseAndCalculate(spectateDistance);
        double height = parseAndCalculate(spectateHeight);

        CameraShot shot = createCameraShot(actualPerspective, distance, height);

        // Stop other tasks
        stopFollowing();

        LocationTarget sceneTarget = new LocationTarget(startLocation);
        String hudMessage = cameraHud ? messages.get("camera-hud", sceneTarget.getName(), actualPerspective.name()) : null;
        currentSpectateTask = new SpectateTask(cameraman, sceneTarget, shot,
                getSmoothingFor(actualPerspective), hudMessage, this::isCameraPaused);
        // Execute immediately to prevent 1-tick jitter
        currentSpectateTask.run();
        // Schedule subsequent runs starting from next tick
        currentSpectateTask.runTaskTimer(plugin, 1L, 1L);
    }

    public void stopFollowing() {
        stopPathPlayback();
        if (currentSpectateTask != null && !currentSpectateTask.isCancelled()) {
            currentSpectateTask.cancel();
        }
        if (currentTeleportTask != null && !currentTeleportTask.isCancelled()) {
            currentTeleportTask.cancel();
            currentTeleportTask = null;
        }
        Player p = getCameraman();
        if (p != null)
            p.setSpectatorTarget(null);
    }

    private CameraShot createCameraShot(SpectatePerspective perspective, double distance, double height) {
        if (perspective == SpectatePerspective.ORBIT) {
            double speed = parseAndCalculate(orbitSpeed);
            boolean isRight = false;

            if ("RANDOM".equalsIgnoreCase(orbitDirection)) {
                isRight = new Random().nextBoolean();
            } else if ("RIGHT".equalsIgnoreCase(orbitDirection)) {
                isRight = true;
            }

            if (isRight) {
                speed = -speed; // Negative speed for RIGHT orbit (assuming positive is LEFT/CCW)
            }

            return new OrbitShot(distance, speed, height);
        } else if (perspective == SpectatePerspective.DYNAMIC || perspective == SpectatePerspective.DYNAMIC_BEHIND
                || perspective == SpectatePerspective.DYNAMIC_FRONT || perspective == SpectatePerspective.DYNAMIC_POV) {
            return new DynamicFollowShot(distance, height, perspective);
        } else if (perspective == SpectatePerspective.FLYBY) {
            double dur = Double.parseDouble(flybyDuration);
            return new FlybyShot(dur);
        } else if (perspective == SpectatePerspective.CRANE) {
            double dur = Double.parseDouble(craneDuration);
            double min = Double.parseDouble(craneHeightMin);
            double max = Double.parseDouble(craneHeightMax);
            return new CraneShot(dur, min, max, distance);
        } else if (perspective == SpectatePerspective.MOVE) {
            double x = parseAndCalculate(moveX);
            double y = parseAndCalculate(moveY);
            double z = parseAndCalculate(moveZ);
            double speed = parseAndCalculate(moveSpeed);
            return new MoveShot(x, y, z, speed, distance, height);
        } else {
            return new StaticShot(perspective, distance, height);
        }
    }

    private void stopSpectateTask() {
        if (currentSpectateTask != null && !currentSpectateTask.isCancelled()) {
            currentSpectateTask.cancel();
            currentSpectateTask = null;
        }
    }

    public void setMobNightVision(boolean enabled) {
        this.mobNightVision = enabled;
        config.set("mobNightVision", enabled);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("mob-night-vision-set", enabled));
        }
    }

    public void setShowMessage(boolean enabled) {
        this.showMessage = enabled;
        config.set("showMessage", enabled);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("show-message-set", enabled));
        }
    }

    public void setNightVisionThreshold(int threshold) {
        this.nightVisionThreshold = threshold;
        config.set("nightVisionThreshold", threshold);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("threshold-set-info", threshold));
        }
    }

    public void setSpectateDistance(String distance) {
        this.spectateDistance = distance;
        config.set("spectateDistance", distance);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("spectate-distance-set", distance));
        }
    }

    public void setSpectateHeight(String height) {
        this.spectateHeight = height;
        config.set("spectateHeight", height);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("spectate-height-set", height));
        }
    }

    // Regex for range: (number)-(number)
    // Supports negative numbers e.g. -1.5--0.5, -5-5
    // Group 1: Min, Group 3: Max
    private static final java.util.regex.Pattern RANGE_PATTERN = java.util.regex.Pattern
            .compile("^((-?\\d+(\\.\\d+)?))-((-?\\d+(\\.\\d+)?))$");

    private double parseAndCalculate(String value) {
        if (value == null)
            return 3.0; // Fallback matches default logic
        try {
            java.util.regex.Matcher matcher = RANGE_PATTERN.matcher(value.trim());

            if (matcher.matches()) {
                double min = Double.parseDouble(matcher.group(1));
                double max = Double.parseDouble(matcher.group(4));

                if (min > max) {
                    double temp = min;
                    min = max;
                    max = temp;
                }
                return min + (new Random().nextDouble() * (max - min));
            }

            // Not a range, try parsing as single value
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // Log warning?
            // plugin.getLogger().warning("Failed to parse value: " + value);
            return 3.0; // Fallback default
        }
    }

    public void setOrbitSpeed(String speed) {
        this.orbitSpeed = speed;
        config.set("orbitSpeed", speed);
        plugin.saveConfig();
        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("orbit-speed-value", speed));
        }
    }

    public void setMinRotationDistance(double distance) {
        this.minRotationDistance = distance;
        config.set("minRotationDistance", distance);
        plugin.saveConfig();

        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("min-rotation-distance-set", distance));
        }
    }

    public double getMinRotationDistance() {
        return minRotationDistance;
    }

    public void setOrbitDirection(String direction) {
        this.orbitDirection = direction;
        config.set("orbitDirection", direction);
        plugin.saveConfig();
        Player cameraman = getCameraman();
        if (cameraman != null) {
            sendInfoMessage(cameraman, messages.get("orbit-direction-set", direction));
        }
    }

    // Setters for new configs could go here, omitting for brevity/task balance
    // unless requested.
    // Actually, I'll add them to support the commands I planned.

    public void setDynamicSmoothness(String val) {
        this.dynamicSmoothness = val;
        config.set("dynamicSmoothness", val);
        plugin.saveConfig();
        if (getCameraman() != null)
            sendInfoMessage(getCameraman(), messages.get("dynamic-smoothness", val));
    }

    public void setFlybyDuration(String val) {
        this.flybyDuration = val;
        config.set("flybyDuration", val);
        plugin.saveConfig();
        if (getCameraman() != null)
            sendInfoMessage(getCameraman(), messages.get("flyby-duration", val));
    }

    public void setCraneDuration(String val) {
        this.craneDuration = val;
        config.set("craneDuration", val);
        plugin.saveConfig();
        if (getCameraman() != null)
            sendInfoMessage(getCameraman(), messages.get("crane-duration", val));
    }

    public void setRandomPerspectives(String type, List<String> perspectives) {
        String path;
        if (type.equalsIgnoreCase("player")) {
            path = "randomPlayerPerspectives";
        } else if (type.equalsIgnoreCase("mob")) {
            path = "randomMobPerspectives";
        } else {
            path = "randomScenicPerspectives";
        }

        config.set(path, perspectives);
        plugin.saveConfig();
        loadRandomPerspectives(); // Reload

        if (getCameraman() != null) {
            sendInfoMessage(getCameraman(), 
                    messages.get("random-perspectives-updated", type, String.join(", ", perspectives)));
        }
    }

    public void setMoveSpeed(String speed) {
        this.moveSpeed = speed;
        config.set("moveSpeed", speed);
        plugin.saveConfig();
        if (getCameraman() != null) {
            sendInfoMessage(getCameraman(), messages.get("move-speed-set", speed));
        }
    }

    public void setMoveDirection(String x, String y, String z) {
        this.moveX = x;
        this.moveY = y;
        this.moveZ = z;
        config.set("moveX", x);
        config.set("moveY", y);
        config.set("moveZ", z);
        plugin.saveConfig();
        if (getCameraman() != null) {
            sendInfoMessage(getCameraman(), messages.get("move-direction-set", x, y, z));
        }
    }

    public void setAutoScenic(boolean enabled, SpectatePerspective perspective) {
        this.autoScenic = enabled;
        if (perspective != null) {
            this.autoScenicPerspective = perspective;
            config.set("autoScenicPerspective", perspective.name());
        }
        config.set("autoScenic", enabled);
        plugin.saveConfig();

        if (getCameraman() != null) {
            String msg = messages.get("auto-scenic-status", enabled);
            if (perspective != null) {
                msg += " (" + messages.get("auto-scenic-perspective", perspective.name()) + ")";
            }
            sendInfoMessage(getCameraman(), msg);
        }
    }
}
