package xyz.shibomb.cameraman.shots;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import xyz.shibomb.cameraman.targets.CameraTarget;

public class CraneShot implements CameraShot {

    private final long durationTicks;
    private final double minHeight;
    private final double maxHeight;
    private final double distance;

    public CraneShot(double durationSeconds, double minHeight, double maxHeight, double distance) {
        this.durationTicks = FlybyShot.toDurationTicks(durationSeconds);
        double safeMin = Double.isFinite(minHeight) ? minHeight : 1.0;
        double safeMax = Double.isFinite(maxHeight) ? maxHeight : 5.0;
        this.minHeight = Math.min(safeMin, safeMax);
        this.maxHeight = Math.max(safeMin, safeMax);
        this.distance = distance;
    }

    @Override
    public Location getNextLocation(Player cameraman, CameraTarget target, long tick) {
        long cycleTick = tick % (durationTicks * 2L);
        double t;

        if (cycleTick < durationTicks) {
            // Up: 0 -> 1
            t = (double) cycleTick / durationTicks;
        } else {
            // Down: 1 -> 0
            t = 1.0 - ((double) (cycleTick - durationTicks) / durationTicks);
        }

        // Easing
        t = t * t * (3 - 2 * t);

        double currentHeight = minHeight + ((maxHeight - minHeight) * t);

        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().setY(0);
        if (direction.lengthSquared() < 1.0e-12) direction = new Vector(0, 0, 1);
        else direction.normalize();

        // Position: Behind + Height
        Location camLoc = targetLoc.clone().subtract(direction.multiply(distance));
        camLoc.add(0, currentHeight, 0);

        // Look at target
        // We want to look at the target's eyes/center, roughly
        camLoc.setDirection(targetLoc.clone().add(0, 1.0, 0).subtract(camLoc).toVector());

        return camLoc;
    }
}
