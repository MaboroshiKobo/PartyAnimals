package org.maboroshi.partyanimals.handler;

import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.behavior.PinataFleeGoal;
import org.maboroshi.partyanimals.behavior.PinataFloatGoal;
import org.maboroshi.partyanimals.behavior.PinataFreezeGoal;
import org.maboroshi.partyanimals.behavior.PinataRoamGoal;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.util.Log;

public class BehaviorHandler {
    private final PartyAnimals plugin;
    private static final Set<String> MOVEMENT_TYPES = Set.of("ACTIVE", "PASSIVE", "STATIONARY");

    public BehaviorHandler(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    public void apply(LivingEntity pinata, PinataConfiguration pinataConfig) {
        if (!pinataConfig.behavior.enabled) {
            pinata.setAI(false);
            return;
        }

        pinata.setAI(true);

        if (pinata instanceof Mob mob) {
            Bukkit.getMobGoals().removeAllGoals(mob);
            Bukkit.getMobGoals().addGoal(mob, 0, new PinataFloatGoal(plugin, mob));

            String rawType = pinataConfig.behavior.movement.type;
            String mode = (rawType != null) ? rawType.toUpperCase() : "ACTIVE";

            if (!MOVEMENT_TYPES.contains(mode)) {
                Log.warn("Unknown movement type '" + mode + "' for pinata " + mob.getUniqueId()
                        + ". Defaulting to ACTIVE.");
                mode = "ACTIVE";
            }

            switch (mode) {
                case "ACTIVE" -> {
                    Bukkit.getMobGoals().addGoal(mob, 1, new PinataFleeGoal(plugin, mob));
                    Bukkit.getMobGoals().addGoal(mob, 2, new PinataFreezeGoal(plugin, mob));
                    Bukkit.getMobGoals().addGoal(mob, 3, new PinataRoamGoal(plugin, mob));
                }
                case "PASSIVE" -> {
                    Bukkit.getMobGoals().addGoal(mob, 2, new PinataFreezeGoal(plugin, mob));
                    Bukkit.getMobGoals().addGoal(mob, 3, new PinataRoamGoal(plugin, mob));
                }
                case "STATIONARY" -> {
                    Bukkit.getMobGoals().addGoal(mob, 2, new PinataFreezeGoal(plugin, mob));
                }
            }
        }

        var knockbackAttribute = pinata.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackAttribute != null) {
            knockbackAttribute.setBaseValue(pinataConfig.behavior.knockbackResistance);
        }
    }
}
