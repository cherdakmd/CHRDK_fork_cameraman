package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.BooleanSupplier;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.targets.CameraTarget;

/**
 * Moves the camera towards the position requested by a {@link CameraShot}.
 *
 * <p>Wall handling is anchored on the target instead of the previous camera position: the camera is
 * always kept on the target's side of an obstacle, so it can leave a spot that is already inside
 * terrain or far away from the new target. The first frame after a target switch snaps straight to
 * the shot position when the jump is large enough, which prevents a long crawl across the world.</p>
 */
public class SpectateTask extends BukkitRunnable {

    /** Jumps longer than this (in blocks) are snapped instead of smoothly interpolated. */
    static final double SNAP_DISTANCE = 24.0;
    /** Minimum blocks per tick the camera covers while catching up with a distant target. */
    static final double CATCH_UP_STEP = 2.0;

    private final Player cameraman;
    private final CameraTarget target;
    private final CameraShot cameraShot;
    private final double smoothing;
    private final BooleanSupplier paused;
    private long tick;
    private boolean firstFrame = true;

    public SpectateTask(Player cameraman, CameraTarget target, CameraShot cameraShot, double smoothing,
            BooleanSupplier paused) {
        this.cameraman = cameraman;
        this.target = target;
        this.cameraShot = cameraShot;
        this.smoothing = Math.max(0.01, Math.min(1.0, smoothing));
        this.paused = paused;
    }

    @Override
    public void run() {
        if (cameraman == null || !cameraman.isOnline() || target == null || !target.isValid()) {
            cancel();
            return;
        }

        if (paused != null && paused.getAsBoolean()) return;

        Location desired = cameraShot.getNextLocation(cameraman, target, tick++);
        if (desired == null || desired.getWorld() == null) return;
        Location current = cameraman.getLocation();

        boolean first = firstFrame;
        firstFrame = false;
        if (shouldSnap(current, desired, first)) {
            cameraman.teleport(CameraInterpolation.keepInSight(target.getEyeLocation(), desired,
                    cameraman.getEyeHeight()));
            return;
        }

        Location interpolated = CameraInterpolation.lerp(current, desired, smoothing);
        interpolated = ensureCatchUpStep(current, interpolated, desired, CATCH_UP_STEP);
        cameraman.teleport(CameraInterpolation.keepInSight(target.getEyeLocation(), interpolated,
                cameraman.getEyeHeight()));
    }

    /**
     * The camera jumps straight to the shot position when the target is far away, in another world,
     * or when the first frame after a target switch would otherwise crawl across the world.
     */
    static boolean shouldSnap(Location current, Location desired, boolean first) {
        boolean sameWorld = current.getWorld() != null && current.getWorld() == desired.getWorld();
        double distance = sameWorld ? current.distance(desired) : Double.POSITIVE_INFINITY;
        if (shouldSnap(distance, sameWorld)) return true;
        if (!first || !sameWorld) return false;
        // A camera that is buried in terrain would otherwise stay pinned by the wall in front of it.
        return current.getBlock().isSolid() || current.getBlock().isLiquid();
    }

    /** Pure snap decision based on the gap and world relationship of the two positions. */
    static boolean shouldSnap(double distance, boolean sameWorld) {
        if (!sameWorld) return true;
        return !Double.isFinite(distance) || distance > SNAP_DISTANCE;
    }

    /**
     * Guarantees forward progress while closing a large gap, so a low smoothing value cannot leave
     * the camera drifting behind a moving player.
     */
    static Location ensureCatchUpStep(Location current, Location interpolated, Location desired,
            double minStep) {
        if (current.getWorld() != desired.getWorld()) return desired;
        double remaining = current.distance(desired);
        if (!(minStep > 0.0) || remaining <= minStep) return interpolated;
        double step = current.distance(interpolated);
        if (step >= Math.min(minStep, remaining)) return interpolated;
        return CameraInterpolation.lerp(current, desired, Math.min(minStep, remaining) / remaining);
    }
}
