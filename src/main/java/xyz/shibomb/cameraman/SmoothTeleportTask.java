package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import xyz.shibomb.cameraman.shots.CameraShot;
import xyz.shibomb.cameraman.targets.CameraTarget;

public class SmoothTeleportTask extends BukkitRunnable {

    private final Player cameraman;
    private final CameraTarget target;
    private final long totalTicks;
    private final Runnable onComplete;
    private final Location startLocation;
    private final Location destination;
    private long currentTick;

    public SmoothTeleportTask(Player cameraman, CameraTarget target, long totalTicks, Runnable onComplete,
            CameraShot cameraShot) {
        this.cameraman = cameraman;
        this.target = target;
        this.totalTicks = Math.max(1L, totalTicks);
        this.onComplete = onComplete;
        this.startLocation = cameraman.getLocation().clone();
        this.destination = cameraShot.getNextLocation(cameraman, target, 0L).clone();
    }

    @Override
    public void run() {
        if (!cameraman.isOnline() || !target.isValid()) {
            cancel();
            return;
        }

        if (currentTick >= totalTicks) {
            cameraman.teleport(destination);
            cancel();
            if (onComplete != null) onComplete.run();
            return;
        }

        double progress = (double) currentTick / totalTicks;
        cameraman.teleport(CameraInterpolation.lerp(startLocation, destination,
                CameraInterpolation.smoothStep(progress)));
        currentTick++;
    }
}
