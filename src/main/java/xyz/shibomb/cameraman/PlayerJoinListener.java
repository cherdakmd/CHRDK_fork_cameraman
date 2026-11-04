package xyz.shibomb.cameraman;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class PlayerJoinListener implements Listener {

    private final OperatorRegistry operators;

    public PlayerJoinListener(OperatorRegistry operators) {
        this.operators = operators;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        operators.restoreOperator(event.getPlayer());
        operators.playerJoined(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        operators.scheduleTargetUnavailable(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Player deaths are handled by onPlayerDeath: the target stays reserved for the respawn.
        if (event.getEntity() instanceof org.bukkit.entity.Player) return;
        operators.scheduleTargetUnavailable(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // A world change drops the spectator target, so keep following the tracked player.
        operators.refreshCameraAttachment(event.getPlayer());
        operators.refreshPlayerTarget(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Spectator attachments survive teleports poorly; re-attach on the next tick.
        operators.scheduleCameraAttachmentRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        operators.scheduleCameraAttachmentRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        operators.scheduleCameraAttachmentRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // A dead player cannot be spectated, but the same player returns after respawning, so only
        // the camera attachment is refreshed instead of dropping the target.
        operators.scheduleCameraAttachmentRefresh(event.getEntity());
    }
}
