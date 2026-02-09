package org.maboroshi.partyanimals.manager;

import de.tr7zw.changeme.nbtapi.NBT;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.api.event.pinata.PinataSpawnEvent;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataVariant;
import org.maboroshi.partyanimals.handler.ActionHandler;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.hook.BetterModelHook;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class PinataFactory {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final Logger log;
    private final MessageUtils messageUtils;
    private final PinataManager pinataManager;
    private final EffectHandler effectHandler;
    private final ActionHandler actionHandler;
    private final ModelEngineHook modelEngineHook;
    private final BetterModelHook betterModelHook;

    public PinataFactory(
            PartyAnimals plugin,
            PinataManager pinataManager,
            ModelEngineHook modelEngineHook,
            BetterModelHook betterModelHook) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.log = plugin.getPluginLogger();
        this.messageUtils = plugin.getMessageUtils();
        this.pinataManager = pinataManager;
        this.effectHandler = plugin.getEffectHandler();
        this.actionHandler = plugin.getActionHandler();

        this.modelEngineHook = modelEngineHook;
        this.betterModelHook = betterModelHook;
    }

    public void spawn(Location location, String templateId) {
        log.debug("Attempting to spawn pinata with template: " + templateId + " at " + location);
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);
        if (pinataConfig == null) {
            log.error("Cannot spawn pinata! Template '" + templateId + "' not found.");
            return;
        }

        Map.Entry<String, PinataVariant> variantEntry = pick(pinataConfig.appearance.variants);
        String variantId = variantEntry.getKey();
        PinataVariant variant = variantEntry.getValue();
        log.debug("Selected variant: " + variantId);

        String chosenType;
        if (variant.types == null || variant.types.isEmpty()) {
            chosenType = "LLAMA";
        } else {
            chosenType = variant.types.get(ThreadLocalRandom.current().nextInt(variant.types.size()));
        }
        EntityType type;
        try {
            type = EntityType.valueOf(chosenType);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type '" + chosenType + "' in template " + templateId + ". Defaulting to LLAMA.");
            type = EntityType.LLAMA;
        }

        double minScale = variant.scale.min;
        double maxScale = variant.scale.max;
        final double finalScale =
                (minScale >= maxScale) ? minScale : ThreadLocalRandom.current().nextDouble(minScale, maxScale);

        int baseHealth = pinataConfig.health.baseHealth;
        int calculatedHealth = pinataConfig.health.perPlayer
                ? baseHealth * Math.max(1, plugin.getServer().getOnlinePlayers().size())
                : baseHealth;
        final int finalHealth = Math.min(calculatedHealth, pinataConfig.health.maxHealth);

        Location spawnLocation = location.clone();
        spawnLocation.setPitch(0);

        final PinataVariant selectedVariant = variant;
        final String selectedVariantId = variantId;

        LivingEntity pinata = (LivingEntity) location.getWorld().spawn(spawnLocation, type.getEntityClass(), entity -> {
            if (entity instanceof LivingEntity livingEntity) {
                configureData(livingEntity, selectedVariant, selectedVariantId, templateId, finalHealth);
            }
        });

        if (pinata == null) {
            log.warn("Failed to spawn pinata entity.");
            return;
        }

        applyVisuals(pinata, pinataConfig, selectedVariant, finalScale);

        var event = new PinataSpawnEvent(pinata, spawnLocation);
        plugin.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            log.debug("Pinata spawn event was cancelled by an API event; removing entity.");
            pinata.remove();
            return;
        }

        pinata.getScheduler()
                .runDelayed(
                        plugin,
                        (task) -> {
                            if (pinata.isValid()) {
                                pinataManager.activatePinata(pinata);
                            }
                        },
                        () -> {},
                        10L);

        effectHandler.playEffects(pinataConfig.events.spawn.effects, location, false);
        actionHandler.process(null, pinataConfig.events.spawn.actions.values(), cmd -> {
            return messageUtils.parsePinataPlaceholders(pinata, cmd);
        });

        log.debug("Pinata spawned successfully. Entity ID: " + pinata.getEntityId() + " UUID: " + pinata.getUniqueId());

        String spawnMessage = config.getMessageConfig().pinata.events.spawnedNaturally;
        messageUtils.send(
                plugin.getServer(),
                spawnMessage,
                messageUtils.tagParsed("pinata", selectedVariant.name),
                messageUtils.tagParsed(
                        "location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    private void configureData(
            LivingEntity livingEntity, PinataVariant variant, String variantId, String templateId, int health) {
        log.debug("Configuring entity data " + livingEntity.getUniqueId());

        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_TEMPLATE, PersistentDataType.STRING, templateId);
        livingEntity.getPersistentDataContainer().set(NamespacedKeys.IS_PINATA, PersistentDataType.BOOLEAN, true);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_NAME, PersistentDataType.STRING, variant.name);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_VARIANT, PersistentDataType.STRING, variantId);
        livingEntity.getPersistentDataContainer().set(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, health);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, health);
        livingEntity
                .getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_SPAWN_TIME, PersistentDataType.LONG, System.currentTimeMillis());
        livingEntity.setMaximumNoDamageTicks(0);
        livingEntity.setSilent(true);
        livingEntity.setInvulnerable(false);
        livingEntity.setRemoveWhenFarAway(false);
        livingEntity.setMaximumAir(100000);
        livingEntity.setRemainingAir(100000);

        if (variant.nbt != null && !variant.nbt.isEmpty() && !variant.nbt.equals("{}")) {
            try {
                NBT.modify(livingEntity, nbt -> {
                    var customNbt = NBT.parseNBT(variant.nbt);
                    nbt.mergeCompound(customNbt);
                });
            } catch (Exception e) {
                log.warn("Failed to apply NBT to pinata variant: " + variant + ". Error: " + e.getMessage());
            }
        }
    }

    private void applyVisuals(
            LivingEntity livingEntity, PinataConfiguration pinataConfig, PinataVariant variant, double scale) {

        boolean modelApplied = false;
        if (variant.model != null && !variant.model.isEmpty()) {
            log.debug("Applying custom model: " + variant.model);
            if (modelEngineHook != null && modelEngineHook.applyModel(livingEntity, variant.model)) {
                log.debug("ModelEngine model applied.");
                modelApplied = true;
                modelEngineHook.setScale(livingEntity, scale);
            } else if (betterModelHook != null && betterModelHook.applyModel(livingEntity, variant.model)) {
                log.debug("BetterModel model applied.");
                modelApplied = true;
                var scaleAttr = livingEntity.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) scaleAttr.setBaseValue(scale);
            } else {
                log.warn("Failed to apply model " + variant.model + " (Hooks missing or returned false)");
            }
        }

        if (!modelApplied) {
            log.debug("No custom model applied. Setting vanilla scale: " + scale);
            var scaleAttr = livingEntity.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) scaleAttr.setBaseValue(scale);
        }

        livingEntity.setInvisible(modelApplied);

        if (livingEntity instanceof Mob mob) mob.setTarget(null);

        boolean shouldGlow = pinataConfig.appearance.glowing;

        if (shouldGlow) {
            String colorName = pinataConfig.appearance.glowColor;
            NamedTextColor glowColor = NamedTextColor.NAMES.value(colorName.toLowerCase());

            if (glowColor != null) {
                Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
                String teamName = "PA_" + glowColor.toString().toUpperCase();
                Team team = mainBoard.getTeam(teamName);
                if (team == null) team = mainBoard.registerNewTeam(teamName);
                team.color(glowColor);
                team.addEntry(livingEntity.getUniqueId().toString());

                if (modelApplied) {
                    livingEntity.setGlowing(false);
                    if (modelEngineHook != null) {
                        modelEngineHook.setGlowing(livingEntity, true, glowColor);
                    }
                    if (betterModelHook != null) {
                        betterModelHook.setGlowing(livingEntity, true, glowColor);
                    }
                } else {
                    livingEntity.setGlowing(true);
                }
            } else {
                log.warn("Invalid glow color name: " + colorName);
            }
        }
    }

    private Map.Entry<String, PinataVariant> pick(Map<String, PinataVariant> variants) {
        if (variants == null || variants.isEmpty()) return null;

        double totalWeight =
                variants.values().stream().mapToDouble(v -> v.weight).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (Map.Entry<String, PinataVariant> entry : variants.entrySet()) {
            random -= entry.getValue().weight;
            if (random <= 0) {
                return entry;
            }
        }

        return variants.entrySet().iterator().next();
    }
}
