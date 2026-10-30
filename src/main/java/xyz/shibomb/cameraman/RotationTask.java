package xyz.shibomb.cameraman;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/** Chooses eligible targets with distance weighting and short-term repeat avoidance. */
public class RotationTask extends BukkitRunnable {

    private static final double OTHER_WORLD_DISTANCE_WEIGHT = 256.0;

    private final CameramanManager manager;
    private final Deque<UUID> recentTargets = new ArrayDeque<>();

    public RotationTask(CameramanManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.markRotationTick();
        Player cameraman = manager.getCameraman();
        if (cameraman == null || !cameraman.isOnline() || manager.isCameraPaused()) return;

        if (manager.isMobTargetMode()) {
            List<org.bukkit.entity.LivingEntity> mobs = manager.getPotentialMobTargets();
            selectAndSet(mobs, cameraman);
            return;
        }

        List<Player> targets = manager.getPotentialTargets();
        if (targets.isEmpty()) {
            switchToMobModeIfDue();
            return;
        }

        // Auto-mob mode can keep this task alive as an inactivity monitor, but does not
        // imply that player rotation itself is enabled.
        if (!manager.isRotationMode()) return;
        selectAndSet(targets, cameraman);
    }

    private void switchToMobModeIfDue() {
        if (!manager.isAutoMobTarget()) return;
        long inactiveMillis = System.currentTimeMillis() - manager.getLastPlayerTargetTime();
        if (inactiveMillis < manager.getAutoMobTargetDelay() * 1_000L) return;

        manager.setMobTargetMode(true);
        Player cameraman = manager.getCameraman();
        if (cameraman != null) {
            manager.sendInfoMessage(cameraman, manager.getMessages().get("auto-switched-mob-target"));
        }
    }

    private <T extends Entity> void selectAndSet(List<T> targets, Player cameraman) {
        T selected = selectWeightedTarget(targets, cameraman, manager.getMinRotationDistance());
        if (selected == null) return;
        manager.setTarget(selected);
        remember(selected.getUniqueId(), targets.size());
    }

    private <T extends Entity> T selectWeightedTarget(List<T> targets, Player cameraman, double minDistance) {
        if (targets.isEmpty()) return null;

        List<T> eligible = candidates(targets, cameraman, minDistance, true, true);
        if (eligible.isEmpty()) {
            // If recent history or distance constraints leave no alternative, relax history first,
            // then the minimum distance; never reselect the current target when another exists.
            eligible = candidates(targets, cameraman, minDistance, false, true);
        }
        if (eligible.isEmpty()) {
            eligible = candidates(targets, cameraman, minDistance, false, false);
        }
        if (eligible.isEmpty()) return null;

        double totalWeight = 0.0;
        for (T candidate : eligible) totalWeight += weightFor(candidate, cameraman);
        double choice = ThreadLocalRandom.current().nextDouble(totalWeight);
        for (T candidate : eligible) {
            choice -= weightFor(candidate, cameraman);
            if (choice < 0.0) return candidate;
        }
        return eligible.get(eligible.size() - 1);
    }

    private <T extends Entity> List<T> candidates(List<T> targets, Player cameraman, double minDistance,
            boolean excludeRecent, boolean enforceMinDistance) {
        List<T> eligible = new ArrayList<>();
        UUID currentTargetId = manager.getCurrentTargetId();
        for (T target : targets) {
            UUID targetId = target.getUniqueId();
            if (targetId.equals(currentTargetId)) continue;
            if (excludeRecent && targets.size() > 1 && recentTargets.contains(targetId)) continue;
            if (enforceMinDistance && minDistance > 0.0
                    && target.getWorld().equals(cameraman.getWorld())
                    && cameraman.getLocation().distance(target.getLocation()) < minDistance) {
                continue;
            }
            eligible.add(target);
        }
        return eligible;
    }

    private double weightFor(Entity target, Player cameraman) {
        double distance = target.getWorld().equals(cameraman.getWorld())
                ? cameraman.getLocation().distance(target.getLocation())
                : OTHER_WORLD_DISTANCE_WEIGHT;
        return distanceWeight(distance);
    }

    static double distanceWeight(double distance) {
        if (!Double.isFinite(distance)) distance = OTHER_WORLD_DISTANCE_WEIGHT;
        return 1.0 / (1.0 + Math.max(0.0, distance));
    }

    private void remember(UUID id, int availableCount) {
        recentTargets.remove(id);
        recentTargets.addFirst(id);
        int limit = Math.min(3, Math.max(0, availableCount - 1));
        while (recentTargets.size() > limit) recentTargets.removeLast();
    }
}
