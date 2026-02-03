package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.util.EnumSet;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class PinataFleeGoal implements Goal<Mob> {
    private final PartyAnimals plugin;
    private final Mob mob;
    private Player targetPlayer;
    private final GoalKey<Mob> key;
    private int pauseCooldown = 0;

    public PinataFleeGoal(PartyAnimals plugin, Mob mob) {
        this.plugin = plugin;
        this.mob = mob;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pinata_flee"));
    }

    @Override
    public boolean shouldActivate() {
        targetPlayer = null;
        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);

        double triggerSq = Math.pow(config.behavior.movement.flee.triggerRadius, 2);

        for (Player p : mob.getWorld().getPlayers()) {
            if (!p.isValid()) continue;
            if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) continue;

            if (p.getLocation().distanceSquared(mob.getLocation()) < triggerSq) {
                targetPlayer = p;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldStayActive() {
        if (targetPlayer == null || !targetPlayer.isValid()) return false;

        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        double safetySq = Math.pow(config.behavior.movement.flee.safetyRadius, 2);

        return mob.getLocation().distanceSquared(targetPlayer.getLocation()) < safetySq;
    }

    @Override
    public void start() {
        pauseCooldown = 0;
        runAway();
    }

    @Override
    public void tick() {
        if (mob.getPathfinder().hasPath()) return;
        if (pauseCooldown > 0) {
            pauseCooldown--;
            return;
        }
        runAway();
        if (Math.random() < 0.3) {
            pauseCooldown = 0;
        } else {
            pauseCooldown = 2 + (int) (Math.random() * 5);
        }
    }

    private void runAway() {
        if (targetPlayer == null) return;

        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        double speed = config.behavior.movement.flee.speed;

        Vector direction =
                mob.getLocation().toVector().subtract(targetPlayer.getLocation().toVector());
        if (direction.lengthSquared() < 0.01) direction = new Vector(1, 0, 0);
        else direction.normalize();

        double distance = 4 + (Math.random() * 3);
        Location targetLoc = mob.getLocation().add(direction.multiply(distance));
        targetLoc.setY(mob.getLocation().getY());

        if (isSafeLocation(targetLoc)) {
            mob.getPathfinder().moveTo(targetLoc, speed);
        }
    }

    private boolean isSafeLocation(Location location) {
        if (!location.getBlock().isPassable()) return false;
        double currentHeight = mob.getHeight();
        int blocksToCheck = (int) Math.ceil(currentHeight);
        for (int i = 1; i < blocksToCheck; i++) {
            if (!location.clone().add(0, i, 0).getBlock().isPassable()) return false;
        }
        return true;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE);
    }
}
