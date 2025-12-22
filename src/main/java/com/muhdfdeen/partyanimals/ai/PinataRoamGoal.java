package com.muhdfdeen.partyanimals.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Creature;

public class PinataRoamGoal implements Goal<Creature> {
    private final ConfigManager config;
    private final Creature mob;
    private final GoalKey<Creature> key;
    private final double speed;

    public PinataRoamGoal(PartyAnimals plugin, Creature mob) {
        this.config = plugin.getConfiguration();
        this.mob = mob;
        this.key = GoalKey.of(Creature.class, new NamespacedKey(plugin, "pinata_roam"));
        this.speed = config.getMainConfig().pinata.ai().pathfinding().movementSpeedMultiplier();
    }

    @Override
    public boolean shouldActivate() {
        return !mob.getPathfinder().hasPath();
    }

    @Override
    public boolean shouldStayActive() {
        return mob.getPathfinder().hasPath();
    }

    @Override
    public void start() {
        double rangeX = config.getMainConfig().pinata.ai().pathfinding().range().x();
        double rangeY = config.getMainConfig().pinata.ai().pathfinding().range().y();
        double rangeZ = config.getMainConfig().pinata.ai().pathfinding().range().z();

        double x = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeX;
        double y = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeY;
        double z = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * rangeZ;

        Location target = mob.getLocation().add(x, y, z);

        if (target.getBlock().isPassable()) {
            mob.getPathfinder().moveTo(target, speed);
        }
    }

    @Override
    public GoalKey<Creature> getKey() {
        return key;
    }

    @Override
    public EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
