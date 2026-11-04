package xyz.shibomb.cameraman.targets;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class EntityTarget implements CameraTarget {
    private final Entity entity;

    public EntityTarget(Entity entity) {
        this.entity = entity;
    }

    @Override
    public Location getLocation() {
        return entity == null ? null : entity.getLocation();
    }

    @Override
    public Location getEyeLocation() {
        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity.getEyeLocation();
        }
        Location location = getLocation();
        return location == null ? null : location.clone().add(0.0, DEFAULT_EYE_HEIGHT, 0.0);
    }

    @Override
    public boolean isValid() {
        if (entity == null || !entity.isValid()) return false;
        // Players keep their entity handle across deaths, respawns and world changes, so only an
        // offline player actually invalidates the target. Death is temporary and the camera
        // re-attaches to the same player after the respawn.
        return !(entity instanceof Player player) || player.isOnline();
    }

    @Override
    public String getName() {
        return entity == null ? "" : entity.getName();
    }

    public Entity getEntity() {
        return entity;
    }
}
