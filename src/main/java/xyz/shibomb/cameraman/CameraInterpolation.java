package xyz.shibomb.cameraman;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/** Small, world-safe interpolation helpers shared by camera movement tasks. */
final class CameraInterpolation {

    private CameraInterpolation() {
    }

    static Location lerp(Location from, Location to, double amount) {
        if (from.getWorld() != to.getWorld()) {
            return to.clone();
        }

        double alpha = Math.max(0.0, Math.min(1.0, amount));
        Vector position = from.toVector().add(to.toVector().subtract(from.toVector()).multiply(alpha));
        Location result = position.toLocation(to.getWorld());
        result.setYaw(lerpAngle(from.getYaw(), to.getYaw(), alpha));
        result.setPitch((float) (from.getPitch() + (to.getPitch() - from.getPitch()) * alpha));
        return result;
    }

    static double smoothStep(double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        return t * t * (3.0 - 2.0 * t);
    }

    private static float lerpAngle(float from, float to, double amount) {
        float difference = (to - from) % 360.0F;
        if (difference < -180.0F) difference += 360.0F;
        if (difference >= 180.0F) difference -= 360.0F;
        return (float) (from + difference * amount);
    }
}
