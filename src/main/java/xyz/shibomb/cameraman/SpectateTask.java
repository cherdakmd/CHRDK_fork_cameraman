package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.BooleanSupplier;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.targets.CameraTarget;

public class SpectateTask extends BukkitRunnable {

    private final Player cameraman;
    private final CameraTarget target;
    private final CameraShot cameraShot;
    private final double smoothing;
    private final BooleanSupplier paused;
    private long tick;

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
        Location current = cameraman.getLocation();
        Location interpolated = CameraInterpolation.lerp(current, desired, smoothing);
        cameraman.teleport(CameraInterpolation.avoidSolidBlocks(cameraman, interpolated));
    }
}
