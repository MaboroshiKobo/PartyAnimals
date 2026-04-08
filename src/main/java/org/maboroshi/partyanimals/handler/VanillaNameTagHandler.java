package org.maboroshi.partyanimals.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class VanillaNameTagHandler {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;
    private final PinataManager pinataManager;

    public VanillaNameTagHandler(PartyAnimals plugin, PinataManager pinataManager) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
        this.pinataManager = pinataManager;
    }

    public void attach(LivingEntity pinata, PinataConfiguration pinataConfig) {
        String existingUuidStr =
                pinata.getPersistentDataContainer().get(NamespacedKeys.PINATA_NAMETAG, PersistentDataType.STRING);

        if (existingUuidStr != null) {
            Entity existing = Bukkit.getEntity(UUID.fromString(existingUuidStr));
            if (existing instanceof TextDisplay textDisplay && existing.isValid()) {
                scheduleNameTagUpdate(pinata, textDisplay);
                return;
            }
        }

        if (pinata.getPassengers() != null) {
            for (Entity passenger : pinata.getPassengers()) {
                if (passenger instanceof TextDisplay display) {
                    saveNametagUuid(pinata, display.getUniqueId());
                    scheduleNameTagUpdate(pinata, display);
                    return;
                }
            }
        }

        Location location = pinata.getLocation().add(0, pinata.getHeight(), 0);
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);
        nameTag.text(buildNameTagComponent(pinata, pinataConfig, getCurrentTimerString(pinata, pinataConfig)));
        nameTag.setAlignment(pinataConfig.appearance.nameTag.textAlignment);
        nameTag.setDefaultBackground(false);

        if (pinataConfig.appearance.nameTag.background.enabled) {
            nameTag.setBackgroundColor(Color.fromARGB(
                    pinataConfig.appearance.nameTag.background.alpha,
                    pinataConfig.appearance.nameTag.background.red,
                    pinataConfig.appearance.nameTag.background.green,
                    pinataConfig.appearance.nameTag.background.blue));
        } else {
            nameTag.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        }

        nameTag.setShadowed(pinataConfig.appearance.nameTag.shadow.enabled);
        nameTag.setShadowRadius(pinataConfig.appearance.nameTag.shadow.radius);
        nameTag.setShadowStrength(pinataConfig.appearance.nameTag.shadow.strength);
        nameTag.setBillboard(pinataConfig.appearance.nameTag.billboard);
        nameTag.setSeeThrough(pinataConfig.appearance.nameTag.seeThrough);

        Transformation nameTransform = nameTag.getTransformation();
        float scaleX = (float) pinataConfig.appearance.nameTag.transformation.scale.x;
        float scaleY = (float) pinataConfig.appearance.nameTag.transformation.scale.y;
        float scaleZ = (float) pinataConfig.appearance.nameTag.transformation.scale.z;
        float transX = (float) pinataConfig.appearance.nameTag.transformation.translation.x;
        float transY = (float) pinataConfig.appearance.nameTag.transformation.translation.y;
        float transZ = (float) pinataConfig.appearance.nameTag.transformation.translation.z;

        nameTransform.getTranslation().set(transX, transY, transZ);
        nameTransform.getScale().set(scaleX, scaleY, scaleZ);
        nameTag.setTransformation(nameTransform);

        saveNametagUuid(pinata, nameTag.getUniqueId());

        if (nameTag.getVehicle() == null) {
            pinata.addPassenger(nameTag);
        }

        scheduleNameTagUpdate(pinata, nameTag);
    }

    public void removeAttachedNameTag(LivingEntity pinata) {
        String existingUuidStr =
                pinata.getPersistentDataContainer().get(NamespacedKeys.PINATA_NAMETAG, PersistentDataType.STRING);

        if (existingUuidStr != null) {
            Entity existing = null;
            try {
                existing = Bukkit.getEntity(UUID.fromString(existingUuidStr));
            } catch (IllegalArgumentException ignored) {
            }
            if (existing instanceof TextDisplay textDisplay && existing.isValid()) {
                textDisplay.remove();
            }
        }

        pinata.getPersistentDataContainer().remove(NamespacedKeys.PINATA_NAMETAG);
    }

    public void scheduleNameTagUpdate(LivingEntity livingEntity, TextDisplay nameTag) {
        PinataConfiguration pinataConfig = pinataManager.getPinataConfig(livingEntity);
        int updateInterval = Math.max(1, pinataConfig.appearance.nameTag.updateTextInterval);

        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        task -> {
                            if (!nameTag.isValid() || !livingEntity.isValid() || livingEntity.isDead()) {
                                task.cancel();
                                if (nameTag.isValid()) nameTag.remove();
                                return;
                            }

                            if (nameTag.getVehicle() == null) {
                                livingEntity.addPassenger(nameTag);
                            }

                            updateText(livingEntity, nameTag, pinataConfig);
                        },
                        () -> {},
                        1L,
                        updateInterval);
    }

    private void saveNametagUuid(LivingEntity pinata, UUID uuid) {
        pinata.getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_NAMETAG, PersistentDataType.STRING, uuid.toString());
    }

    private void updateText(LivingEntity livingEntity, TextDisplay nameTag, PinataConfiguration pinataConfig) {
        nameTag.text(
                buildNameTagComponent(livingEntity, pinataConfig, getCurrentTimerString(livingEntity, pinataConfig)));
    }

    private Component buildNameTagComponent(
            LivingEntity livingEntity, PinataConfiguration pinataConfig, String timerText) {
        List<String> lines = pinataConfig.appearance.nameTag.text;
        List<Component> components = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                components.add(messageUtils.parse(
                        null, line, messageUtils.getPinataTags(livingEntity), messageUtils.tag("timer", timerText)));
            }
        }
        return Component.join(JoinConfiguration.newlines(), components);
    }

    private String getCurrentTimerString(LivingEntity livingEntity, PinataConfiguration pinataConfig) {
        String timeStr = "∞";
        if (pinataConfig.timer.timeout.enabled && pinataConfig.timer.timeout.duration > 0) {
            long spawnTime = livingEntity
                    .getPersistentDataContainer()
                    .getOrDefault(
                            NamespacedKeys.PINATA_SPAWN_TIME, PersistentDataType.LONG, System.currentTimeMillis());
            int totalTimeout = pinataConfig.timer.timeout.duration;
            int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
            timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
        }
        return timeStr;
    }
}
