package xyz.shibomb.cameraman;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        operators.scheduleTargetUnavailable(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        operators.refreshPlayerTarget(event.getPlayer());
    }
}
