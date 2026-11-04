package xyz.shibomb.cameraman;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Small, world-safe interpolation helpers shared by camera movement tasks. */
final class CameraInterpolation {

    /** Minimal distance the camera keeps from the target when it is pushed away from a wall. */
    static final double MIN_LINE_OF_SIGHT_DISTANCE = 0.25;
    /** Distance kept in front of an obstacle blocking the view of the target. */
    static final double OBSTACLE_MARGIN = 0.4;

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

    /**
     * Keeps a third-person camera position on the target's side of walls.
     *
     * <p>The line of sight is traced from the target towards the requested camera position. Clamping
     * the result onto that ray means the camera can never be dragged into a wall or pinned behind
     * terrain it is trying to leave, which is what happened when the ray started at the camera's old
     * position after a target switch.</p>
     *
     * @param anchorEye target eye location the camera should keep in view
     * @param desired   requested camera position (player feet coordinates)
     * @param eyeHeight camera eye height, used to trace between eye positions
     * @return the closest safe location to {@code desired} that still sees the anchor
     */
    static Location keepInSight(Location anchorEye, Location desired, double eyeHeight) {
        if (anchorEye == null || desired == null || anchorEye.getWorld() == null
                || !anchorEye.getWorld().equals(desired.getWorld())) {
            return desired;
        }

        Vector anchor = anchorEye.toVector();
        Vector goal = desired.toVector().add(new Vector(0.0, eyeHeight, 0.0));
        Vector delta = goal.clone().subtract(anchor);
        double rayLength = delta.length();
        if (rayLength < 0.75) return desired;

        Vector direction = delta.clone().multiply(1.0 / rayLength);
        var hit = anchorEye.getWorld().rayTraceBlocks(anchor.toLocation(anchorEye.getWorld()), direction,
                rayLength, FluidCollisionMode.NEVER, true);
        if (hit == null || hit.getHitPosition() == null) return desired;

        double hitDistance = anchor.distance(hit.getHitPosition());
        double allowed = clampRayDistance(rayLength, hitDistance, OBSTACLE_MARGIN);
        if (allowed >= rayLength) return desired;

        Location result = anchorEye.clone().add(direction.multiply(allowed)).subtract(0.0, eyeHeight, 0.0);
        result.setYaw(desired.getYaw());
        result.setPitch(desired.getPitch());
        return result;
    }

    /**
     * Pure helper behind {@link #keepInSight(Location, Location, double)}.
     *
     * @param rayLength   distance from the target to the requested camera position
     * @param hitDistance distance from the target to the first obstacle, negative or non-finite when unknown
     * @param margin      gap kept in front of the obstacle
     * @return distance from the target the camera is allowed to occupy
     */
    static double clampRayDistance(double rayLength, double hitDistance, double margin) {
        if (!Double.isFinite(rayLength) || rayLength <= 0.0) return 0.0;
        if (!Double.isFinite(hitDistance) || hitDistance < 0.0) return rayLength;
        double safeMargin = Double.isFinite(margin) && margin >= 0.0 ? margin : OBSTACLE_MARGIN;
        double allowed = hitDistance - safeMargin;
        if (allowed < MIN_LINE_OF_SIGHT_DISTANCE) {
            // The obstacle is closer than the margin: stay in front of it instead of jumping through it.
            allowed = Math.min(hitDistance, MIN_LINE_OF_SIGHT_DISTANCE);
        }
        return Math.max(0.0, Math.min(rayLength, allowed));
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
