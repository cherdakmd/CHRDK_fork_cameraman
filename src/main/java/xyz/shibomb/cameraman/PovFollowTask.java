package xyz.shibomb.cameraman;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Keeps a POV camera attached to its target.
 *
 * <p>{@code Player#setSpectatorTarget} fails silently when the entity is in another world, outside the
 * client's entity tracking range, or when the spectator/target teleports, respawns, changes world or
 * dismounts by sneaking. Without a watchdog the camera then stays frozen at its old coordinates. This
 * task re-teleports the camera into range and re-attaches every tick, so POV following survives all of
 * those transitions.</p>
 */
public final class PovFollowTask extends BukkitRunnable {

    /** Distance (blocks) beyond which the client no longer tracks the target entity. */
    static final double TRACKING_DISTANCE = 24.0;
    /** Ticks to wait between re-attach attempts after a failed attempt, to avoid spamming packets. */
    static final long RETRY_INTERVAL_TICKS = 4L;
    /** Failed attach attempts after which the camera is teleported onto the target. */
    static final int FORCE_TELEPORT_ATTEMPTS = 3;
    /** Ticks a target may stay dead before it is treated as unavailable (10 seconds). */
    static final long MAX_DEAD_TICKS = 200L;

    private final Player cameraman;
    private final UUID targetId;
    private final Runnable onTargetUnavailable;
    private long tick;
    private long lastAttemptTick = -1L;
    private int failedAttempts;
    private long deadTicks;

    public PovFollowTask(Player cameraman, UUID targetId, Runnable onTargetUnavailable) {
        this.cameraman = cameraman;
        this.targetId = targetId;
        this.onTargetUnavailable = onTargetUnavailable;
    }

    @Override
    public void run() {
        if (cameraman == null || !cameraman.isOnline() || cameraman.getGameMode() != GameMode.SPECTATOR) {
            cancel();
            return;
        }

        Entity target = resolveTarget();
        if (target == null || !target.isValid() || isUnavailablePlayer(target)) {
            cancel();
            if (onTargetUnavailable != null) onTargetUnavailable.run();
            return;
        }

        if (isDeadPlayer(target)) {
            // The same player becomes spectatable again after respawning, but a target that never
            // respawns (kept dead by another plugin, quit during death) must not freeze the camera
            // on a corpse forever.
            if (exceedsDeadTicks(++deadTicks)) {
                cancel();
                if (onTargetUnavailable != null) onTargetUnavailable.run();
            }
            return;
        }
        deadTicks = 0L;

        if (needsReposition(cameraman, target)) {
            // Teleport first and attach on a later tick: a spectator target set in the same tick as a
            // world/position change is frequently dropped by the server.
            cameraman.teleport(target.getLocation());
            return;
        }

        if (hasTarget(cameraman, target)) {
            failedAttempts = 0;
            return;
        }
        tick++;
        if (!shouldRetry(tick, lastAttemptTick)) return;
        lastAttemptTick = tick;
        cameraman.setSpectatorTarget(target);

        // If attaching keeps failing even though the target is in range, the client may not have the
        // entity loaded at all. Teleporting onto the target forces the chunk and entity to load.
        if (shouldForceTeleport(++failedAttempts)) {
            failedAttempts = 0;
            cameraman.teleport(target.getLocation());
        }
    }

    /** Re-attaching packets are throttled so a stubborn target does not spam the connection. */
    static boolean shouldRetry(long tick, long lastAttemptTick) {
        return lastAttemptTick < 0L || tick - lastAttemptTick >= RETRY_INTERVAL_TICKS;
    }

    /** True once the target has been dead for longer than {@link #MAX_DEAD_TICKS}. */
    static boolean exceedsDeadTicks(long deadTicks) {
        return deadTicks > MAX_DEAD_TICKS;
    }

    /** After repeated failures the camera is teleported onto the target to force the entity to load. */
    static boolean shouldForceTeleport(int failedAttempts) {
        return failedAttempts >= FORCE_TELEPORT_ATTEMPTS;
    }

    static boolean tracksAt(double distanceSquared) {
        if (!Double.isFinite(distanceSquared)) return false;
        return distanceSquared <= TRACKING_DISTANCE * TRACKING_DISTANCE;
    }

    Entity resolveTarget() {
        return Bukkit.getEntity(targetId);
    }

    /** A player leaving the server invalidates the target for good. */
    static boolean isUnavailablePlayer(Entity target) {
        return target instanceof Player player && !player.isOnline();
    }

    /** A dead player cannot be spectated yet, but keeps the camera reserved for the respawn. */
    static boolean isDeadPlayer(Entity target) {
        return target instanceof Player player && player.isDead();
    }

    /** True when the camera is too far away for the client to track the target entity. */
    static boolean needsReposition(Player cameraman, Entity target) {
        if (cameraman.getWorld() == null || target.getWorld() == null) return true;
        if (!cameraman.getWorld().equals(target.getWorld())) return true;
        Location from = cameraman.getLocation();
        Location to = target.getLocation();
        return !tracksAt(from.distanceSquared(to));
    }

    static boolean hasTarget(Player cameraman, Entity target) {
        Entity current = cameraman.getSpectatorTarget();
        return current != null && current.getUniqueId().equals(target.getUniqueId());
    }

    UUID getTargetId() {
        return targetId;
    }
}
