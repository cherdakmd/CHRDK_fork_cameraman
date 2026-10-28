package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.targets.CameraTarget;

public class SpectateTask extends BukkitRunnable {

    private final Player cameraman;
    private final CameraTarget target;
    private final CameraShot cameraShot;
    private final double smoothing;
    private long tick;

    public SpectateTask(Player cameraman, CameraTarget target, CameraShot cameraShot, double smoothing) {
        this.cameraman = cameraman;
        this.target = target;
        this.cameraShot = cameraShot;
        this.smoothing = Math.max(0.01, Math.min(1.0, smoothing));
    }

    @Override
    public void run() {
        if (cameraman == null || !cameraman.isOnline() || target == null || !target.isValid()) {
            cancel();
            return;
        }

        Location desired = cameraShot.getNextLocation(cameraman, target, tick++);
        Location current = cameraman.getLocation();
        cameraman.teleport(CameraInterpolation.lerp(current, desired, smoothing));
    }
}
