package xyz.shibomb.cameraman.shots;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import xyz.shibomb.cameraman.SpectatePerspective;
import xyz.shibomb.cameraman.targets.CameraTarget;
import xyz.shibomb.cameraman.targets.EntityTarget;

/** Calculates a moving camera position; SpectateTask performs temporal smoothing. */
public class DynamicFollowShot implements CameraShot {

    private final double distance;
    private final double height;
    private final SpectatePerspective perspective;

    public DynamicFollowShot(double distance, double height, SpectatePerspective perspective) {
        this.distance = distance;
        this.height = height;
        this.perspective = perspective;
    }

    @Override
    public Location getNextLocation(Player cameraman, CameraTarget target, long tick) {
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.getDirection().setY(0);
        if (direction.lengthSquared() < 1.0e-12) direction = new Vector(0, 0, 1);
        else direction.normalize();
        Location idealLoc;

        if (perspective == SpectatePerspective.DYNAMIC_FRONT || perspective == SpectatePerspective.FRONT) {
            idealLoc = targetLoc.clone().add(direction.clone().multiply(distance));
            idealLoc.add(0, height, 0);
            idealLoc.setDirection(targetLoc.clone().subtract(idealLoc).toVector());
        } else if (perspective == SpectatePerspective.DYNAMIC_POV || perspective == SpectatePerspective.POV) {
            if (target instanceof EntityTarget entityTarget && entityTarget.getEntity() instanceof LivingEntity livingEntity) {
                idealLoc = livingEntity.getEyeLocation();
            } else {
                idealLoc = targetLoc.clone().add(0, 1.62, 0);
            }
            idealLoc.subtract(0, cameraman.getEyeHeight(), 0).add(0, 0.5, 0);
            idealLoc.setDirection(targetLoc.getDirection());
        } else {
            idealLoc = targetLoc.clone().subtract(direction.multiply(distance));
            idealLoc.add(0, height, 0);
            idealLoc.setDirection(targetLoc.clone().subtract(idealLoc).toVector());
        }

        return idealLoc;
    }
}
