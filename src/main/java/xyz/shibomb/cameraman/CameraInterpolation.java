package xyz.shibomb.cameraman;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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

    static Location avoidSolidBlocks(Player camera, Location desired) {
        Location start = camera.getEyeLocation();
        Location end = desired.clone().add(0.0, camera.getEyeHeight(), 0.0);
        if (start.getWorld() != end.getWorld()) return desired;
        Vector delta = end.toVector().subtract(start.toVector());
        double distance = delta.length();
        if (distance < 0.05) return desired;
        var hit = start.getWorld().rayTraceBlocks(start, delta.normalize(), distance,
                FluidCollisionMode.NEVER, true);
        if (hit == null || hit.getHitPosition() == null) return desired;
        Location safe = hit.getHitPosition().toLocation(start.getWorld())
                .subtract(delta.multiply(0.3)).subtract(0.0, camera.getEyeHeight(), 0.0);
        safe.setYaw(desired.getYaw());
        safe.setPitch(desired.getPitch());
        return safe;
    }

    static double smoothStep(double amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        return t * t * (3.0 - 2.0 * t);
    }

    static double ease(double amount, String easing) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        return switch (easing == null ? "SMOOTHSTEP" : easing.toUpperCase(java.util.Locale.ROOT)) {
            case "LINEAR" -> t;
            case "EASE_IN_OUT" -> t * t * (3.0 - 2.0 * t);
            default -> smoothStep(t);
        };
    }

    private static float lerpAngle(float from, float to, double amount) {
        float difference = (to - from) % 360.0F;
        if (difference < -180.0F) difference += 360.0F;
        if (difference >= 180.0F) difference -= 360.0F;
        return (float) (from + difference * amount);
    }
}
