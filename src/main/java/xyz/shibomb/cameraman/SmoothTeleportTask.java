package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.BooleanSupplier;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.targets.CameraTarget;

/**
 * Glides the camera from its current position to the shot position of a new target.
 *
 * <p>Obstruction checks are anchored on the target ({@link CameraInterpolation#keepInSight}), so a
 * transition started behind a wall or on the other side of the world still completes instead of
 * getting pinned in place.</p>
 */
public class SmoothTeleportTask extends BukkitRunnable {

    private final Player cameraman;
    private final CameraTarget target;
    private final long totalTicks;
    private final Runnable onComplete;
    private final Location startLocation;
    private final Location destination;
    private final String easing;
    private final BooleanSupplier paused;
    private long currentTick;

    public SmoothTeleportTask(Player cameraman, CameraTarget target, long totalTicks, Runnable onComplete,
            CameraShot cameraShot, String easing, BooleanSupplier paused) {
        this.cameraman = cameraman;
        this.target = target;
        this.totalTicks = Math.max(1L, totalTicks);
        this.onComplete = onComplete;
        this.startLocation = cameraman.getLocation().clone();
        this.destination = cameraShot.getNextLocation(cameraman, target, 0L).clone();
        this.easing = easing;
        this.paused = paused;
    }

    @Override
    public void run() {
        if (!cameraman.isOnline() || !target.isValid()) {
            cancel();
            return;
        }

        if (paused != null && paused.getAsBoolean()) return;

        if (currentTick >= totalTicks) {
            cameraman.teleport(CameraInterpolation.keepInSight(target.getEyeLocation(), destination,
                    cameraman.getEyeHeight()));
            cancel();
            if (onComplete != null) onComplete.run();
            return;
        }

        double progress = (double) currentTick / totalTicks;
        Location frame = CameraInterpolation.lerp(startLocation, destination,
                CameraInterpolation.ease(progress, easing));
        cameraman.teleport(CameraInterpolation.keepInSight(target.getEyeLocation(), frame,
                cameraman.getEyeHeight()));
        currentTick++;
    }
}
