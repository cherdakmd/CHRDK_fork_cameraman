package xyz.shibomb.cameraman;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.BooleanSupplier;

/** Plays a saved route as a sequence of eased camera moves. */
public final class CameraPathTask extends BukkitRunnable {

    private final Player camera;
    private final List<Location> waypoints;
    private final long ticksPerSegment;
    private final String easing;
    private final BooleanSupplier paused;
    private final String hudMessage;
    private long tick;
    private Location segmentStart;
    private int waypointIndex;
    private long segmentTick;

    public CameraPathTask(Player camera, List<Location> waypoints, long ticksPerSegment, String easing,
            BooleanSupplier paused, String hudMessage) {
        this.camera = camera;
        this.waypoints = waypoints.stream().map(Location::clone).toList();
        this.ticksPerSegment = Math.max(1L, ticksPerSegment);
        this.easing = easing;
        this.paused = paused;
        this.hudMessage = hudMessage;
        this.segmentStart = camera.getLocation().clone();
    }

    @Override
    public void run() {
        if (!camera.isOnline()) {
            cancel();
            return;
        }
        if (waypointIndex >= waypoints.size()) {
            cancel();
            return;
        }
        if (paused != null && paused.getAsBoolean()) return;
        if (hudMessage != null && tick++ % 20L == 0L) {
            camera.sendActionBar(net.kyori.adventure.text.Component.text(hudMessage));
        }

        Location destination = waypoints.get(waypointIndex);
        if (segmentStart.getWorld() != destination.getWorld()) {
            camera.teleport(destination);
            segmentStart = destination.clone();
            waypointIndex++;
            segmentTick = 0L;
            return;
        }

        double progress = (double) segmentTick / ticksPerSegment;
        Location frame = CameraInterpolation.lerp(segmentStart, destination,
                CameraInterpolation.ease(progress, easing));
        camera.teleport(CameraInterpolation.avoidSolidBlocks(camera, frame));
        segmentTick++;
        if (segmentTick >= ticksPerSegment) {
            camera.teleport(CameraInterpolation.avoidSolidBlocks(camera, destination));
            segmentStart = destination.clone();
            waypointIndex++;
            segmentTick = 0L;
        }
    }
}
