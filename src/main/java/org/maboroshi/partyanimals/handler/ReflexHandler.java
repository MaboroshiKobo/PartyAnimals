package org.maboroshi.partyanimals.handler;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataVariant;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class ReflexHandler {
    private final PartyAnimals plugin;
    private final PinataManager pinataManager;
    private final EffectHandler effectHandler;
    private final ActionHandler actionHandler;
    private final ModelEngineHook modelEngineHook;

    public ReflexHandler(PartyAnimals plugin, PinataManager pinataManager, ModelEngineHook modelEngineHook) {
        this.plugin = plugin;
        this.pinataManager = pinataManager;
        this.effectHandler = plugin.getEffectHandler();
        this.actionHandler = plugin.getActionHandler();
        this.modelEngineHook = modelEngineHook;
    }

    public void onDamage(LivingEntity pinata, Player attacker, PinataConfiguration config) {
        if (!config.behavior.enabled) return;

        var shockwave = config.behavior.reflexes.shockwave;
        if (shockwave.enabled && shouldTrigger(shockwave.chance)) {
            pinataManager.playAnimation(pinata, shockwave.animation);
            effectHandler.playEffects(shockwave.effects, pinata.getLocation(), false);
            pinata.getNearbyEntities(shockwave.radius, shockwave.radius, shockwave.radius)
                    .forEach(entity -> {
                        if (entity instanceof Player player && !player.equals(pinata)) {
                            Vector direction = player.getLocation()
                                    .toVector()
                                    .subtract(pinata.getLocation().toVector());
                            if (direction.lengthSquared() < 0.01) {
                                direction = new Vector(0, shockwave.verticalBoost, 0);
                            } else {
                                direction
                                        .normalize()
                                        .multiply(shockwave.strength)
                                        .setY(shockwave.verticalBoost);
                            }
                            player.setVelocity(direction);
                        }
                    });
            if (!shockwave.actions.isEmpty()) {
                actionHandler.process(attacker, shockwave.actions.values(), cmd -> plugin.getMessageUtils()
                        .parsePinataPlaceholders(pinata, cmd));
            }
        }

        var morph = config.behavior.reflexes.morph;
        if (morph.enabled && shouldTrigger(morph.chance)) {
            pinataManager.playAnimation(pinata, morph.animation);
            effectHandler.playEffects(morph.effects, pinata.getLocation(), false);
            if (morph.type.equalsIgnoreCase("AGE")) {
                if (pinata instanceof Ageable ageable) {
                    ageable.setBaby();
                    pinata.getScheduler()
                            .runDelayed(
                                    plugin,
                                    (task) -> {
                                        if (pinata.isValid()) {
                                            ageable.setAdult();
                                        }
                                    },
                                    null,
                                    morph.duration);
                }
            } else if (morph.type.equalsIgnoreCase("SCALE")) {
                var scaleAttribute = pinata.getAttribute(Attribute.SCALE);
                if (scaleAttribute != null) {
                    double minMorph = Math.min(morph.scale.min, morph.scale.max);
                    double maxMorph = Math.max(morph.scale.min, morph.scale.max);
                    double morphScale = (minMorph == maxMorph)
                            ? minMorph
                            : ThreadLocalRandom.current().nextDouble(minMorph, maxMorph);

                    scaleAttribute.setBaseValue(morphScale);
                    if (modelEngineHook != null) modelEngineHook.setScale(pinata, morphScale);

                    pinata.getScheduler()
                            .runDelayed(
                                    plugin,
                                    (task) -> {
                                        if (pinata.isValid()) {
                                            String variantId = pinata.getPersistentDataContainer()
                                                    .get(NamespacedKeys.PINATA_VARIANT, PersistentDataType.STRING);
                                            PinataVariant variant = config.appearance.variants.get(variantId);

                                            double originalScale = (variant != null) ? variant.scale.max : 1.0;

                                            scaleAttribute.setBaseValue(originalScale);
                                            if (modelEngineHook != null)
                                                modelEngineHook.setScale(pinata, originalScale);
                                        }
                                    },
                                    null,
                                    morph.duration);
                }
            }

            if (!morph.actions.isEmpty()) {
                actionHandler.process(attacker, morph.actions.values(), cmd -> plugin.getMessageUtils()
                        .parsePinataPlaceholders(pinata, cmd));
            }
        }

        var blink = config.behavior.reflexes.blink;
        if (blink.enabled && shouldTrigger(blink.chance)) {
            pinataManager.playAnimation(pinata, blink.animation);
            effectHandler.playEffects(blink.effects, pinata.getLocation(), false);

            Location location = pinata.getLocation();
            double x = location.getX() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            double z = location.getZ() + (ThreadLocalRandom.current().nextDouble() - 0.5) * blink.distance * 2;
            Location target = findSafeY(pinata.getWorld(), x, z, location.getY(), 8);

            if (target != null) {
                target.setYaw(location.getYaw());
                target.setPitch(location.getPitch());
                pinata.teleportAsync(target).thenAccept(success -> {
                    if (success) {
                        if (!blink.actions.isEmpty()) {
                            actionHandler.process(attacker, blink.actions.values(), cmd -> plugin.getMessageUtils()
                                    .parsePinataPlaceholders(pinata, cmd));
                        }
                    }
                });
            }
        }

        var leap = config.behavior.reflexes.leap;
        if (leap.enabled && shouldTrigger(leap.chance)) {
            double blinkDistance = config.behavior.reflexes.blink.distance;
            double thresholdSq = blinkDistance * blinkDistance;
            thresholdSq = Math.max(thresholdSq, 25.0);
            if (pinata.getLocation().distanceSquared(attacker.getLocation()) > thresholdSq) {
                return;
            }
            effectHandler.playEffects(leap.effects, pinata.getLocation(), false);
            pinataManager.playAnimation(pinata, leap.animation);
            pinata.setVelocity(new Vector(0, leap.strength, 0));
            if (!leap.actions.isEmpty()) {
                actionHandler.process(attacker, leap.actions.values(), cmd -> plugin.getMessageUtils()
                        .parsePinataPlaceholders(pinata, cmd));
            }
        }

        var sugarRush = config.behavior.reflexes.sugarRush;
        if (sugarRush.enabled && shouldTrigger(sugarRush.chance)) {
            effectHandler.playEffects(sugarRush.effects, pinata.getLocation(), false);
            pinataManager.playAnimation(pinata, sugarRush.animation);
            pinata.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, sugarRush.duration, sugarRush.amplifier));
            if (!sugarRush.actions.isEmpty()) {
                actionHandler.process(attacker, sugarRush.actions.values(), cmd -> plugin.getMessageUtils()
                        .parsePinataPlaceholders(pinata, cmd));
            }
        }

        var dazzle = config.behavior.reflexes.dazzle;
        if (dazzle.enabled && shouldTrigger(dazzle.chance)) {
            effectHandler.playEffects(dazzle.effects, attacker.getEyeLocation(), false);
            pinataManager.playAnimation(pinata, dazzle.animation);
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, dazzle.duration, 0));
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, dazzle.duration, 0));
            if (!dazzle.actions.isEmpty()) {
                actionHandler.process(attacker, dazzle.actions.values(), cmd -> plugin.getMessageUtils()
                        .parsePinataPlaceholders(pinata, cmd));
            }
        }
    }

    public boolean shouldTrigger(double chance) {
        return ThreadLocalRandom.current().nextDouble(100.0) < chance;
    }

    private Location findSafeY(org.bukkit.World world, double x, double z, double startY, int verticalRange) {
        Location target = new Location(world, x, startY, z);
        if (isSafeLocation(target)) return target;

        for (int i = 1; i <= verticalRange; i++) {
            target.setY(startY + i);
            if (isSafeLocation(target)) return target;

            target.setY(startY - i);
            if (isSafeLocation(target)) return target;
        }

        return null;
    }

    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        if (feet.getRelative(BlockFace.DOWN).isPassable()) return false;
        if (!feet.isPassable()) return false;
        if (!feet.getRelative(BlockFace.UP).isPassable()) return false;
        return true;
    }
}
