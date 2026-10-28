package xyz.shibomb.cameraman;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
    private final JavaPlugin plugin;
    private final Map<UUID, ProfilePrompt> pendingProfileNames = new ConcurrentHashMap<>();

    public CameramanMenu(JavaPlugin plugin) {
        this.plugin = plugin;
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
        putAction(inventory, 11, Material.SPIDER_EYE, manager.getMessages().get("menu-mob-target-button"),
                "mob-targets", manager.getMessages().get("menu-mob-target-button-lore"));
        putToggle(inventory, 12, manager, Material.COMPASS, "menu-newcomer", manager.isNewcomerMode(), "newcomer");
        putToggle(inventory, 13, manager, Material.CLOCK, "menu-rotation", manager.isRotationMode(), "rotation",
                manager.getRotationIntervalSeconds());
        putToggle(inventory, 14, manager, Material.SPIDER_EYE, "menu-mobs", manager.isMobTargetMode(), "mobs");
        putToggle(inventory, 15, manager, Material.SLIME_BALL, "menu-auto-mobs", manager.isAutoMobTarget(), "auto-mobs",
                manager.getAutoMobTargetDelay());
        putToggle(inventory, 16, manager, Material.ENDER_PEARL, "menu-smooth", manager.isTeleportSmooth(), "smooth",
                manager.getTeleportSmoothDuration());
        putAction(inventory, 20, Material.SPYGLASS,
                manager.getMessages().get("menu-perspective", manager.getSpectatePerspective().name()), "perspective-player");
        putAction(inventory, 21, Material.ENDER_EYE,
                manager.getMessages().get("menu-mob-perspective", manager.getMobSpectatePerspective().name()), "perspective-mob");
        putAction(inventory, 22, Material.BARRIER, manager.getMessages().get("menu-clear-target"), "clear",
                manager.getMessages().get("menu-clear-target-lore"));
        putAction(inventory, 23, Material.GOLD_INGOT,
                manager.getMessages().get("menu-distance", manager.getSpectateDistance()), "distance",
                manager.getMessages().get("menu-adjust-lore"));
        putAction(inventory, 24, Material.FEATHER,
                manager.getMessages().get("menu-height", manager.getSpectateHeight()), "height",
                manager.getMessages().get("menu-adjust-lore"));
        putAction(inventory, 25, manager.isCameraPaused() ? Material.GRAY_DYE : Material.LIME_DYE,
                manager.getMessages().get(manager.isCameraPaused() ? "menu-resume" : "menu-pause"), "pause");
        putAction(inventory, 26, Material.ARROW, manager.getMessages().get("menu-next-target"), "next-target");
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

    private void openMobTargets(Player viewer, CameramanManager manager, int page) {
        List<LivingEntity> targets = manager.getPotentialMobTargets();
        int pageCount = Math.max(1, (targets.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.min(Math.max(0, page), pageCount - 1);
        MenuHolder holder = new MenuHolder(manager, safePage, true);
        Inventory inventory = Bukkit.createInventory(holder, 54, manager.getMessages().get("menu-mob-targets-title"));
        holder.setInventory(inventory);
        int start = safePage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, targets.size());
        for (int index = start; index < end; index++) {
            LivingEntity target = targets.get(index);
            Material icon = Material.matchMaterial(target.getType().name() + "_SPAWN_EGG");
            ItemStack item = new ItemStack(icon == null ? Material.ENDER_EYE : icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(manager.getMessages().get("menu-mob-target", target.getName()));
                meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
                        target.getUniqueId().toString());
                item.setItemMeta(meta);
            }
            inventory.setItem(index - start, item);
        }
        if (targets.isEmpty()) {
            inventory.setItem(22, namedItem(Material.BARRIER, manager.getMessages().get("menu-no-mobs")));
        }
        putAction(inventory, 45, Material.ARROW, manager.getMessages().get("menu-back"), "home");
        if (safePage > 0) putAction(inventory, 48, Material.ARROW, manager.getMessages().get("menu-previous"), "mobs-prev");
        inventory.setItem(49, namedItem(Material.PAPER, manager.getMessages().get("menu-page", safePage + 1, pageCount)));
        if (safePage + 1 < pageCount) putAction(inventory, 50, Material.ARROW,
                manager.getMessages().get("menu-next"), "mobs-next");
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
                    manager.getMessages().get("menu-profile", profile), "profile:" + profile,
                    manager.getMessages().get("menu-profile-lore"));
        }
        if (profiles.isEmpty()) {
            inventory.setItem(22, namedItem(Material.BARRIER, manager.getMessages().get("menu-no-profiles")));
        }
        putAction(inventory, 45, Material.ARROW, manager.getMessages().get("menu-back"), "home");
        putAction(inventory, 47, Material.WRITABLE_BOOK, manager.getMessages().get("menu-profile-save"),
                "profiles-save", manager.getMessages().get("menu-profile-save-lore"));
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
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingProfileNames.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onProfileNameChat(AsyncPlayerChatEvent event) {
        ProfilePrompt prompt = pendingProfileNames.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String profileName = event.getMessage().trim();
        CameramanManager manager = prompt.manager();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) return;
            if (profileName.equalsIgnoreCase("cancel")) {
                player.sendMessage(manager.getMessages().get("menu-profile-cancelled"));
            } else if (prompt.renameFrom() == null && manager.saveProfile(profileName)) {
                player.sendMessage(manager.getMessages().get("profile-saved", profileName));
            } else if (prompt.renameFrom() != null && manager.renameProfile(prompt.renameFrom(), profileName)) {
                player.sendMessage(manager.getMessages().get("profile-renamed", prompt.renameFrom(), profileName));
            } else {
                player.sendMessage(manager.getMessages().get(prompt.renameFrom() == null
                        ? "profile-invalid-name" : "profile-rename-failed"));
            }
            openProfiles(player, manager);
        });
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
            selectTarget(viewer, holder.manager, targetId, holder.mobPage);
            return;
        }
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;
        CameramanManager manager = holder.manager;
        switch (action) {
            case "home" -> openDashboard(viewer, manager);
            case "menu-target-button" -> openTargets(viewer, manager, 0);
            case "mob-targets" -> openMobTargets(viewer, manager, 0);
            case "targets-prev" -> openTargets(viewer, manager, holder.page - 1);
            case "targets-next" -> openTargets(viewer, manager, holder.page + 1);
            case "mobs-prev" -> openMobTargets(viewer, manager, holder.page - 1);
            case "mobs-next" -> openMobTargets(viewer, manager, holder.page + 1);
            case "profiles" -> openProfiles(viewer, manager);
            case "profiles-save" -> {
                pendingProfileNames.put(viewer.getUniqueId(), new ProfilePrompt(manager, null));
                viewer.closeInventory();
                viewer.sendMessage(manager.getMessages().get("menu-profile-prompt"));
            }
            case "perspective-player" -> {
                manager.setSpectatePerspective(nextPerspective(manager.getSpectatePerspective()));
                openDashboard(viewer, manager);
            }
            case "perspective-mob" -> {
                manager.setMobSpectatePerspective(nextPerspective(manager.getMobSpectatePerspective()));
                openDashboard(viewer, manager);
            }
            case "distance" -> {
                manager.adjustSpectateDistance(event.getClick() == ClickType.RIGHT ? -0.5 : 0.5);
                openDashboard(viewer, manager);
            }
            case "height" -> {
                manager.adjustSpectateHeight(event.getClick() == ClickType.RIGHT ? -0.5 : 0.5);
                openDashboard(viewer, manager);
            }
            case "pause" -> {
                viewer.sendMessage(manager.getMessages().get(manager.toggleCameraPause() ? "menu-paused" : "menu-resumed"));
                openDashboard(viewer, manager);
            }
            case "next-target" -> {
                if (!manager.advanceTarget()) viewer.sendMessage(manager.getMessages().get("menu-no-targets"));
                viewer.closeInventory();
            }
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
                    if (event.getClick() == ClickType.RIGHT) {
                        viewer.sendMessage(manager.deleteProfile(profile)
                                ? manager.getMessages().get("profile-deleted", profile)
                                : manager.getMessages().get("profile-not-found", profile));
                        openProfiles(viewer, manager);
                    } else if (event.getClick() == ClickType.MIDDLE) {
                        pendingProfileNames.put(viewer.getUniqueId(), new ProfilePrompt(manager, profile));
                        viewer.closeInventory();
                        viewer.sendMessage(manager.getMessages().get("menu-profile-rename-prompt", profile));
                    } else {
                        viewer.sendMessage(manager.loadProfile(profile)
                                ? manager.getMessages().get("profile-loaded", profile)
                                : manager.getMessages().get("profile-not-found", profile));
                        openDashboard(viewer, manager);
                    }
                }
            }
        }
    }

    private SpectatePerspective nextPerspective(SpectatePerspective current) {
        SpectatePerspective[] perspectives = SpectatePerspective.values();
        return perspectives[(current.ordinal() + 1) % perspectives.length];
    }

    private void selectTarget(Player viewer, CameramanManager manager, String targetId, boolean mobPage) {
        try {
            UUID uuid = UUID.fromString(targetId);
            Entity target = Bukkit.getEntity(uuid);
            boolean available = target instanceof Player player
                    ? player.isOnline() && manager.getPotentialTargets().contains(player)
                    : target instanceof LivingEntity living && manager.getPotentialMobTargets().contains(living);
            if (!available) {
                viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
                if (mobPage) openMobTargets(viewer, manager, 0);
                else openTargets(viewer, manager, 0);
                return;
            }
            manager.setTarget(target);
            viewer.closeInventory();
        } catch (IllegalArgumentException ignored) {
            viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
        }
    }

    private record ProfilePrompt(CameramanManager manager, String renameFrom) {
    }

    private static final class MenuHolder implements InventoryHolder {
        private final CameramanManager manager;
        private final int page;
        private final boolean mobPage;
        private Inventory inventory;

        MenuHolder(CameramanManager manager, int page) {
            this(manager, page, false);
        }

        MenuHolder(CameramanManager manager, int page, boolean mobPage) {
            this.manager = manager;
            this.page = page;
            this.mobPage = mobPage;
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
