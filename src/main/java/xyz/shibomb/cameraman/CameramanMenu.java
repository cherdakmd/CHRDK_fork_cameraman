package xyz.shibomb.cameraman;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CameramanMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private final NamespacedKey targetKey;
    private final NamespacedKey actionKey;

    public CameramanMenu(JavaPlugin plugin) {
        this.targetKey = new NamespacedKey(plugin, "camera-target");
        this.actionKey = new NamespacedKey(plugin, "camera-menu-action");
    }

    /** Opens the camera control panel. */
    public void open(Player viewer, CameramanManager manager) {
        openDashboard(viewer, manager);
    }

    private void openDashboard(Player viewer, CameramanManager manager) {
        MenuHolder holder = new MenuHolder(manager, 0);
        Inventory inventory = Bukkit.createInventory(holder, 36, manager.getMessages().get("menu-title"));
        holder.setInventory(inventory);

        putAction(inventory, 10, Material.PLAYER_HEAD, manager.getMessages().get("menu-target-button"),
                "menu-target-button", manager.getMessages().get("menu-target-button-lore"));
        putToggle(inventory, 12, manager, Material.COMPASS, "menu-newcomer", manager.isNewcomerMode(), "newcomer");
        putToggle(inventory, 13, manager, Material.CLOCK, "menu-rotation", manager.isRotationMode(), "rotation",
                manager.getRotationIntervalSeconds());
        putToggle(inventory, 14, manager, Material.SPIDER_EYE, "menu-mobs", manager.isMobTargetMode(), "mobs");
        putToggle(inventory, 15, manager, Material.SLIME_BALL, "menu-auto-mobs", manager.isAutoMobTarget(), "auto-mobs",
                manager.getAutoMobTargetDelay());
        putToggle(inventory, 16, manager, Material.ENDER_PEARL, "menu-smooth", manager.isTeleportSmooth(), "smooth",
                manager.getTeleportSmoothDuration());
        putAction(inventory, 22, Material.BARRIER, manager.getMessages().get("menu-clear-target"), "clear",
                manager.getMessages().get("menu-clear-target-lore"));
        putAction(inventory, 30, Material.WRITABLE_BOOK, manager.getMessages().get("menu-profiles-button"), "profiles");
        putAction(inventory, 31, Material.BOOK, manager.getMessages().get("menu-help"), "help",
                manager.getMessages().get("menu-help-lore"));
        viewer.openInventory(inventory);
    }

    private void openTargets(Player viewer, CameramanManager manager, int page) {
        List<Player> targets = manager.getPotentialTargets();
        int pageCount = Math.max(1, (targets.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(Math.max(0, page), pageCount - 1);
        MenuHolder holder = new MenuHolder(manager, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, manager.getMessages().get("menu-targets-title"));
        holder.setInventory(inventory);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, targets.size());
        for (int index = start; index < end; index++) {
            Player target = targets.get(index);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(manager.getMessages().get("menu-target", target.getName()));
                meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
                        target.getUniqueId().toString());
                head.setItemMeta(meta);
            }
            inventory.setItem(index - start, head);
        }
        if (targets.isEmpty()) {
            inventory.setItem(22, namedItem(Material.BARRIER, manager.getMessages().get("menu-no-targets")));
        }

        putAction(inventory, 45, Material.ARROW, manager.getMessages().get("menu-back"), "home");
        if (safePage > 0) {
            putAction(inventory, 48, Material.ARROW, manager.getMessages().get("menu-previous"), "targets-prev");
        }
        inventory.setItem(49, namedItem(Material.PAPER, manager.getMessages().get("menu-page", safePage + 1, pageCount)));
        if (safePage + 1 < pageCount) {
            putAction(inventory, 50, Material.ARROW, manager.getMessages().get("menu-next"), "targets-next");
        }
        viewer.openInventory(inventory);
    }

    private void openProfiles(Player viewer, CameramanManager manager) {
        MenuHolder holder = new MenuHolder(manager, 0);
        Inventory inventory = Bukkit.createInventory(holder, 54, manager.getMessages().get("menu-profiles-title"));
        holder.setInventory(inventory);
        List<String> profiles = manager.getProfileNames();
        int limit = Math.min(PAGE_SIZE, profiles.size());
        for (int index = 0; index < limit; index++) {
            String profile = profiles.get(index);
            putAction(inventory, index, Material.BOOK,
                    manager.getMessages().get("menu-profile", profile), "profile:" + profile);
        }
        if (profiles.isEmpty()) {
            inventory.setItem(22, namedItem(Material.BARRIER, manager.getMessages().get("menu-no-profiles")));
        }
        putAction(inventory, 45, Material.ARROW, manager.getMessages().get("menu-back"), "home");
        viewer.openInventory(inventory);
    }

    private void putToggle(Inventory inventory, int slot, CameramanManager manager, Material material, String labelKey,
            boolean enabled, String action, Object... details) {
        String state = manager.getMessages().get(enabled ? "menu-enabled" : "menu-disabled");
        Object[] arguments = new Object[details.length + 1];
        arguments[0] = state;
        System.arraycopy(details, 0, arguments, 1, details.length);
        String label = manager.getMessages().get(labelKey, arguments);
        putAction(inventory, slot, enabled ? Material.LIME_DYE : material, label, action,
                manager.getMessages().get("menu-toggle-lore"));
    }

    private void putAction(Inventory inventory, int slot, Material material, String name, String action, String... lore) {
        ItemStack item = namedItem(material, name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (lore.length > 0) meta.setLore(List.of(lore));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)
                || event.getClickedInventory() != event.getView().getTopInventory()) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String targetId = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
        if (targetId != null) {
            selectTarget(viewer, holder.manager, targetId);
            return;
        }
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        CameramanManager manager = holder.manager;
        switch (action) {
            case "home" -> openDashboard(viewer, manager);
            case "menu-target-button" -> openTargets(viewer, manager, 0);
            case "targets-prev" -> openTargets(viewer, manager, holder.page - 1);
            case "targets-next" -> openTargets(viewer, manager, holder.page + 1);
            case "profiles" -> openProfiles(viewer, manager);
            case "newcomer" -> {
                manager.setNewcomerMode(!manager.isNewcomerMode());
                openDashboard(viewer, manager);
            }
            case "rotation" -> {
                if (event.getClick() == ClickType.RIGHT) {
                    long[] intervals = {15, 30, 60, 120};
                    long current = manager.getRotationIntervalSeconds();
                    long next = intervals[0];
                    for (int i = 0; i < intervals.length; i++) {
                        if (intervals[i] > current) {
                            next = intervals[i];
                            break;
                        }
                        if (i == intervals.length - 1) next = intervals[0];
                    }
                    manager.setRotationMode(manager.isRotationMode(), next);
                } else {
                    manager.setRotationMode(!manager.isRotationMode(), manager.getRotationIntervalSeconds());
                }
                openDashboard(viewer, manager);
            }
            case "mobs" -> {
                manager.setMobTargetMode(!manager.isMobTargetMode());
                openDashboard(viewer, manager);
            }
            case "auto-mobs" -> {
                manager.setAutoMobTarget(!manager.isAutoMobTarget(), manager.getAutoMobTargetDelay());
                openDashboard(viewer, manager);
            }
            case "smooth" -> {
                manager.setTeleportSmooth(!manager.isTeleportSmooth(), manager.getTeleportSmoothDuration());
                openDashboard(viewer, manager);
            }
            case "clear" -> {
                manager.clearTarget();
                viewer.closeInventory();
            }
            case "help" -> viewer.sendMessage(manager.getMessages().get("menu-help-message"));
            default -> {
                if (action.startsWith("profile:")) {
                    String profile = action.substring("profile:".length());
                    viewer.sendMessage(manager.loadProfile(profile)
                            ? manager.getMessages().get("profile-loaded", profile)
                            : manager.getMessages().get("profile-not-found", profile));
                    openDashboard(viewer, manager);
                }
            }
        }
    }

    private void selectTarget(Player viewer, CameramanManager manager, String targetId) {
        try {
            UUID uuid = UUID.fromString(targetId);
            Player target = Bukkit.getPlayer(uuid);
            if (target == null || !target.isOnline() || !manager.getPotentialTargets().contains(target)) {
                viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
                openTargets(viewer, manager, 0);
                return;
            }
            manager.setTarget(target);
            viewer.closeInventory();
        } catch (IllegalArgumentException ignored) {
            viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private final CameramanManager manager;
        private final int page;
        private Inventory inventory;

        MenuHolder(CameramanManager manager, int page) {
            this.manager = manager;
            this.page = page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}
