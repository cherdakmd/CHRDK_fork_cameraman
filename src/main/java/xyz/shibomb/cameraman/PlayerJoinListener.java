package xyz.shibomb.cameraman;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final CameramanManager manager;

    public PlayerJoinListener(CameramanManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        manager.restoreCameraman(event.getPlayer());
        
        Player cameraman = manager.getCameraman();
        if (cameraman != null && cameraman.getUniqueId().equals(event.getPlayer().getUniqueId())) {
            return; // Don't target self
        }

        if (manager.isNewcomerMode()) {
            manager.setTarget(event.getPlayer());
        } else if (manager.isMobTargetMode()) {
            // If Newcomer Mode is off, but we are in Mob Target Mode, 
            // we should probably disable it so RotationTask can pick up the new player.
            // But only if we want to prioritize players.
            // Let's disable it to be safe and prioritize players.
            manager.setMobTargetMode(false);
            Player cameramanPlayer = manager.getCameraman();
            if (cameramanPlayer != null) {
                cameramanPlayer.sendMessage(manager.getMessages().get("mob-mode-disabled-join"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.scheduleTargetUnavailable(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        manager.scheduleTargetUnavailable(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        manager.refreshPlayerTarget(event.getPlayer());
    }
}
