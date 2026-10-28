package xyz.shibomb.cameraman;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public class RotationTask extends BukkitRunnable {

    private final CameramanManager manager;
    private final Deque<UUID> recentTargets = new ArrayDeque<>();
    private int currentIndex = 0;

    public RotationTask(CameramanManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        Player cameraman = manager.getCameraman();
        if (cameraman == null || !cameraman.isOnline() || manager.isCameraPaused())
            return;

        double minDistance = manager.getMinRotationDistance();

        if (manager.isMobTargetMode()) {
            List<org.bukkit.entity.LivingEntity> mobs = manager.getPotentialMobTargets();
            if (mobs.isEmpty()) {
                return;
            }

            // Find next valid target
            int attempts = 0;
            // Loop at most mobs.size() times to find a valid target
            while (attempts < mobs.size()) {
                if (currentIndex >= mobs.size()) {
                    currentIndex = 0;
                }

                org.bukkit.entity.LivingEntity candidate = mobs.get(currentIndex);
                currentIndex++; // Always advance index for next time
                attempts++;
                if (wasRecentlySelected(candidate.getUniqueId(), mobs.size())) continue;

                // If minDistance is set (> 0), check distance
                if (minDistance > 0.0) {
                    try {
                        // Check world first
                        if (cameraman.getWorld().equals(candidate.getWorld())) {
                            if (cameraman.getLocation().distance(candidate.getLocation()) < minDistance) {
                                // Too close, skip
                                continue;
                            }
                        }
                        // If different worlds, distance check is N/A (or effectively infinite), so
                        // valid.
                    } catch (IllegalArgumentException e) {
                        // Different worlds
                    }
                }

                // Found valid target
                manager.setTarget(candidate);
                remember(candidate.getUniqueId(), mobs.size());
                return;
            }

            // If distance or history excluded every candidate, still make progress.
            org.bukkit.entity.LivingEntity fallback = mobs.get(currentIndex % mobs.size());
            manager.setTarget(fallback);
            remember(fallback.getUniqueId(), mobs.size());
            currentIndex++;

        } else {
            List<Player> targets = manager.getPotentialTargets();
            if (targets.isEmpty()) {
                if (manager.isAutoMobTarget()) {
                    long timeSinceLastPlayer = System.currentTimeMillis() - manager.getLastPlayerTargetTime();
                    if (timeSinceLastPlayer > manager.getAutoMobTargetDelay() * 1000) {
                        manager.setMobTargetMode(true);
                        if (manager.getCameraman() != null) {
                            manager.sendInfoMessage(manager.getCameraman(),
                                    manager.getMessages().get("auto-switched-mob-target"));
                        }
                    }
                }
                return;
            }

            // Find next valid target
            int attempts = 0;
            while (attempts < targets.size()) {
                if (currentIndex >= targets.size()) {
                    currentIndex = 0;
                }

                Player candidate = targets.get(currentIndex);
                currentIndex++;
                attempts++;
                if (wasRecentlySelected(candidate.getUniqueId(), targets.size())) continue;

                if (minDistance > 0.0) {
                    try {
                        if (cameraman.getWorld().equals(candidate.getWorld())) {
                            if (cameraman.getLocation().distance(candidate.getLocation()) < minDistance) {
                                // Too close
                                continue;
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // Different worlds
                    }
                }

                manager.setTarget(candidate);
                remember(candidate.getUniqueId(), targets.size());
                return;
            }

            // Fallback if distance or history excluded every candidate.
            Player fallback = targets.get(currentIndex % targets.size());
            manager.setTarget(fallback);
            remember(fallback.getUniqueId(), targets.size());
            currentIndex++;
        }
    }

    private boolean wasRecentlySelected(UUID id, int availableCount) {
        return availableCount > 1 && recentTargets.contains(id);
    }

    private void remember(UUID id, int availableCount) {
        recentTargets.remove(id);
        recentTargets.addFirst(id);
        int limit = Math.min(3, Math.max(0, availableCount - 1));
        while (recentTargets.size() > limit) recentTargets.removeLast();
    }
}
