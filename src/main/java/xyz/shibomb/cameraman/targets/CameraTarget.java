package xyz.shibomb.cameraman.targets;

import org.bukkit.Location;

public interface CameraTarget {

    /** Default eye offset used when a target type has no dedicated eye location. */
    double DEFAULT_EYE_HEIGHT = 1.62;

    Location getLocation();

    boolean isValid();

    String getName();

    /**
     * Eye location used as the anchor for framing and line-of-sight checks.
     *
     * @return eye location, or {@code null} when the target is unavailable
     */
    default Location getEyeLocation() {
        Location location = getLocation();
        return location == null ? null : location.clone().add(0.0, DEFAULT_EYE_HEIGHT, 0.0);
    }
}
