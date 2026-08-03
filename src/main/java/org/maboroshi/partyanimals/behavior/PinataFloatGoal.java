package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class PinataFloatGoal implements Goal<Mob> {
    private static final double[] ANGLES = {45, -45, 90, -90, 135, -135, 180};
    private final Mob mob;
    private final GoalKey<Mob> key;
    private Vector cachedLandDirection = null;
    private int cooldown = 0;

    public PinataFloatGoal(Plugin plugin, Mob mob) {
        this.mob = mob;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pinata_float"));
    }

    @Override
    public boolean shouldActivate() {
        return mob.isInWater() || mob.getEyeLocation().getBlock().isLiquid();
    }

    @Override
    public void start() {
        cooldown = 0;
        cachedLandDirection = null;
    }

    @Override
    public void tick() {
        if (ThreadLocalRandom.current().nextFloat() < 0.8F) {
            Vector velocity = mob.getVelocity();
            if (velocity.getY() < 0.1) {
                velocity.setY(0.15);
            }
            Vector dir = getDirectionToLand();
            dir = avoidWalls(dir);
            velocity.setX(velocity.getX() * 0.5 + dir.getX() * 0.04);
            velocity.setZ(velocity.getZ() * 0.5 + dir.getZ() * 0.04);
            mob.setVelocity(velocity);
            if (dir.getX() != 0 || dir.getZ() != 0) {
                Location loc = mob.getLocation();
                loc.setDirection(dir);
                mob.setRotation(loc.getYaw(), loc.getPitch());
            }
        }
    }

    private Vector avoidWalls(Vector intendedDir) {
        Location eyeLoc = mob.getEyeLocation();
        Vector checkDir = intendedDir.clone().normalize();
        if (isBlocked(eyeLoc, checkDir)) {
            for (double angle : ANGLES) {
                Vector rotated = checkDir.clone().rotateAroundY(Math.toRadians(angle));
                if (!isBlocked(eyeLoc, rotated)) {
                    return rotated;
                }
            }
        }
        return intendedDir;
    }

    private boolean isBlocked(Location origin, Vector direction) {
        int targetX = origin.getBlockX() + (int) Math.round(direction.getX());
        int targetY = origin.getBlockY() + (int) Math.round(direction.getY());
        int targetZ = origin.getBlockZ() + (int) Math.round(direction.getZ());
        return origin.getWorld().getBlockAt(targetX, targetY, targetZ).getType().isSolid();
    }

    private Vector getDirectionToLand() {
        if (cooldown-- > 0 && cachedLandDirection != null) {
            return cachedLandDirection;
        }
        cooldown = 20;

        Location start = mob.getLocation();
        int startX = start.getBlockX();
        int startY = start.getBlockY();
        int startZ = start.getBlockZ();
        int bestX = 0;
        int bestZ = 0;
        int nearestDistSq = Integer.MAX_VALUE;
        int radius = 8;
        for (int y = -1; y <= 1; y++) {
            for (int x = -radius; x <= radius; x += 2) {
                for (int z = -radius; z <= radius; z += 2) {
                    Block b = start.getWorld().getBlockAt(startX + x, startY + y, startZ + z);
                    if (b.getType().isSolid() && !b.isLiquid()) {
                        int distSq = x * x + y * y + z * z;
                        if (distSq < nearestDistSq) {
                            nearestDistSq = distSq;
                            bestX = x;
                            bestZ = z;
                        }
                    }
                }
            }
        }
        if (nearestDistSq != Integer.MAX_VALUE) {
            cachedLandDirection = new Vector(bestX, 0, bestZ);
        } else {
            cachedLandDirection = start.getDirection().setY(0);
        }
        if (cachedLandDirection.lengthSquared() < 0.001) {
            cachedLandDirection = new Vector(1, 0, 0);
        } else {
            cachedLandDirection.normalize();
        }
        return cachedLandDirection;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.JUMP, GoalType.MOVE);
    }
}
