package xyz.shibomb.cameraman;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class CameramanMenu implements Listener {

    private final NamespacedKey targetKey;

    public CameramanMenu(JavaPlugin plugin) {
        this.targetKey = new NamespacedKey(plugin, "camera-target");
    }

    public void open(Player viewer, CameramanManager manager) {
        MenuHolder holder = new MenuHolder(manager);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                manager.getMessages().get("menu-title"));
        holder.setInventory(inventory);

        List<Player> targets = manager.getPotentialTargets();
        for (int slot = 0; slot < Math.min(targets.size(), inventory.getSize()); slot++) {
            Player target = targets.get(slot);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(manager.getMessages().get("menu-target", target.getName()));
                meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
                        target.getUniqueId().toString());
                head.setItemMeta(meta);
            }
            inventory.setItem(slot, head);
        }

        if (targets.isEmpty()) {
            inventory.setItem(22, namedItem(Material.BARRIER, manager.getMessages().get("menu-no-targets")));
        }
        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        CameramanManager manager = holder.manager;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player viewer)
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ItemStack item = event.getCurrentItem();
        if (item == null || !(item.getItemMeta() instanceof SkullMeta meta)) {
            return;
        }
        String targetId = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
        if (targetId == null) {
            return;
        }
        try {
            UUID uuid = UUID.fromString(targetId);
            Player target = Bukkit.getPlayer(uuid);
            if (target == null || !target.isOnline()) {
                viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
                viewer.closeInventory();
                return;
            }
            manager.setTarget(target);
            viewer.closeInventory();
        } catch (IllegalArgumentException ignored) {
            viewer.sendMessage(manager.getMessages().get("menu-target-gone"));
        }
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class MenuHolder implements InventoryHolder {
        private final CameramanManager manager;
        private Inventory inventory;

        MenuHolder(CameramanManager manager) {
            this.manager = manager;
        }

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
