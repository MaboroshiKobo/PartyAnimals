package org.maboroshi.partyanimals.behavior;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import java.util.EnumSet;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;

public class PinataFreezeGoal implements Goal<Mob> {
    private final PartyAnimals plugin;
    private final Mob mob;
    private final GoalKey<Mob> key;
    private Player watcher;

    public PinataFreezeGoal(PartyAnimals plugin, Mob mob) {
        this.plugin = plugin;
        this.mob = mob;
        this.key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "pinata_freeze"));
    }

    @Override
    public boolean shouldActivate() {
        watcher = null;
        double closestDist = Double.MAX_VALUE;

        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        double radiusSq = Math.pow(config.behavior.movement.freeze.radius, 2);

        for (Player p : mob.getWorld().getPlayers()) {
            if (!p.isValid()) continue;
            double distSq = p.getLocation().distanceSquared(mob.getLocation());
            if (distSq < radiusSq) {
                if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                    if (distSq < closestDist) {
                        closestDist = distSq;
                        watcher = p;
                    }
                }
            }
        }
        return watcher != null;
    }

    @Override
    public boolean shouldStayActive() {
        if (watcher == null || !watcher.isValid()) return false;
        if (watcher.getGameMode() != GameMode.SURVIVAL && watcher.getGameMode() != GameMode.ADVENTURE) return false;

        PinataConfiguration config = plugin.getPinataManager().getPinataConfig(mob);
        double radiusSq = Math.pow(config.behavior.movement.freeze.radius, 2);

        return watcher.getLocation().distanceSquared(mob.getLocation()) < radiusSq;
    }

    @Override
    public void start() {
        mob.getPathfinder().stopPathfinding();
    }

    @Override
    public void tick() {
        if (watcher != null) {
            mob.lookAt(watcher);
        }
    }

    @Override
    public void stop() {
        watcher = null;
    }

    @Override
    public GoalKey<Mob> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
