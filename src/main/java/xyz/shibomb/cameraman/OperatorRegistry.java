package xyz.shibomb.cameraman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns one independent camera manager and settings section per operator. */
public final class OperatorRegistry {

    private static final String OPERATORS_PATH = "cameraOperators";
    private final JavaPlugin plugin;
    private final Map<UUID, CameramanManager> sessions = new LinkedHashMap<>();
    private CameramanManager fallbackManager;

    public OperatorRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        List<UUID> ids = new ArrayList<>();
        for (String value : plugin.getConfig().getStringList(OPERATORS_PATH)) {
            try {
                UUID id = UUID.fromString(value);
                if (!ids.contains(id)) ids.add(id);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid operator UUID in config: " + value);
            }
        }

        String legacyId = plugin.getConfig().getString("cameraman");
        if (legacyId != null && !legacyId.isBlank()) {
            try {
                UUID id = UUID.fromString(legacyId);
                if (!ids.contains(id)) ids.add(id);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Ignoring invalid legacy cameraman UUID in config.");
            }
        }

        for (UUID id : ids) {
            sessions.put(id, new CameramanManager(plugin, id));
        }
        persistOperatorIds();
        fallbackManager = sessions.isEmpty() ? new CameramanManager(plugin) : sessions.values().iterator().next();
    }

    public CameramanManager getManagerFor(CommandSender sender) {
        if (sender instanceof Player player) {
            CameramanManager ownSession = sessions.get(player.getUniqueId());
            if (ownSession != null) return ownSession;
        }
        return fallbackManager;
    }

    public CameramanManager getPrimaryManager() {
        return fallbackManager;
    }

    public CameramanManager getSession(UUID operatorId) {
        return sessions.get(operatorId);
    }

    public Collection<CameramanManager> getSessions() {
        return List.copyOf(sessions.values());
    }

    public List<String> getOperatorNames() {
        return sessions.keySet().stream()
                .map(id -> {
                    Player player = Bukkit.getPlayer(id);
                    return player == null ? id.toString() : player.getName();
                })
                .toList();
    }

    public boolean isOperator(UUID id) {
        return sessions.containsKey(id);
    }

    public UUID getPrimaryOperatorId() {
        return sessions.isEmpty() ? null : sessions.keySet().iterator().next();
    }

    public CameramanManager addOperator(Player player) {
        UUID id = player.getUniqueId();
        CameramanManager manager = sessions.get(id);
        if (manager == null) {
            manager = new CameramanManager(plugin, id);
            sessions.put(id, manager);
        }
        manager.setCameraman(player);
        persistOperatorIds();
        if (fallbackManager == null || sessions.size() == 1) fallbackManager = manager;
        return manager;
    }

    public boolean removeOperator(UUID id) {
        CameramanManager manager = sessions.remove(id);
        if (manager == null) return false;
        manager.removeCameraman();
        manager.stopAllTasks();
        plugin.getConfig().set("operatorSettings." + id, null);
        persistOperatorIds();
        fallbackManager = sessions.isEmpty() ? new CameramanManager(plugin) : sessions.values().iterator().next();
        return true;
    }

    public int removeAllOperators() {
        List<UUID> ids = new ArrayList<>(sessions.keySet());
        ids.forEach(this::removeOperator);
        return ids.size();
    }

    public void restoreOperator(Player player) {
        CameramanManager manager = sessions.get(player.getUniqueId());
        if (manager != null) manager.restoreCameraman(player);
    }

    public void playerJoined(Player player) {
        for (CameramanManager manager : sessions.values()) {
            Player cameraman = manager.getCameraman();
            if (cameraman == null || cameraman.getUniqueId().equals(player.getUniqueId())) continue;
            if (manager.isNewcomerMode()) {
                manager.setTarget(player);
            } else if (manager.isMobTargetMode()) {
                manager.setMobTargetMode(false);
                cameraman.sendMessage(manager.getMessages().get("mob-mode-disabled-join"));
                manager.setTarget(player);
            } else {
                manager.ensureTargetSelected();
            }
        }
    }

    public void scheduleTargetUnavailable(UUID id) {
        for (CameramanManager manager : sessions.values()) manager.scheduleTargetUnavailable(id);
    }

    public void refreshPlayerTarget(Player player) {
        for (CameramanManager manager : sessions.values()) manager.refreshPlayerTarget(player);
    }

    public void stopAllTasks() {
        for (CameramanManager manager : sessions.values()) manager.stopAllTasks();
        if (!sessions.containsValue(fallbackManager)) fallbackManager.stopAllTasks();
    }

    private void persistOperatorIds() {
        plugin.getConfig().set(OPERATORS_PATH, sessions.keySet().stream().map(UUID::toString).toList());
        plugin.getConfig().set("cameraman", null);
        plugin.saveConfig();
    }
}
