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
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class NameTagHandler {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;
    private final PinataManager pinataManager;
    private final ModelEngineHook modelEngineHook;

    public NameTagHandler(PartyAnimals plugin, PinataManager pinataManager, ModelEngineHook modelEngineHook) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
        this.pinataManager = pinataManager;
        this.modelEngineHook = modelEngineHook;
    }

    public void attach(LivingEntity pinata) {
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

        PinataConfiguration pinataConfig = pinataManager.getPinataConfig(pinata);

        Location location = pinata.getLocation().add(0, pinata.getHeight(), 0);
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);

        nameTag.setInterpolationDuration(3);
        nameTag.setTeleportDuration(3);

        int totalSeconds = pinataConfig.timer.timeout.duration;
        String initialTimeStr = "∞";

        if (pinataConfig.timer.timeout.enabled && totalSeconds > 0) {
            initialTimeStr = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
        }

        List<String> lines = pinataConfig.appearance.nameTag.text;
        List<Component> components = new ArrayList<>();

        if (lines != null) {
            for (String line : lines) {
                components.add(messageUtils.parse(
                        null, line, messageUtils.getPinataTags(pinata), messageUtils.tag("timer", initialTimeStr)));
            }
        }

        nameTag.text(Component.join(JoinConfiguration.newlines(), components));
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

        if (modelEngineHook != null) {
            modelEngineHook.addPassenger(pinata, nameTag, "p_mount");
        }

        if (nameTag.getVehicle() == null) {
            pinata.addPassenger(nameTag);
        }

        scheduleNameTagUpdate(pinata, nameTag);
    }

    private void saveNametagUuid(LivingEntity pinata, UUID uuid) {
        pinata.getPersistentDataContainer()
                .set(NamespacedKeys.PINATA_NAMETAG, PersistentDataType.STRING, uuid.toString());
    }

    public void scheduleNameTagUpdate(LivingEntity livingEntity, TextDisplay nameTag) {
        PinataConfiguration pinataConfig = pinataManager.getPinataConfig(livingEntity);
        int updateInterval = Math.max(1, pinataConfig.appearance.nameTag.updateTextInterval);
        final int[] tickCounter = {0};

        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        (task) -> {
                            if (!nameTag.isValid() || !livingEntity.isValid() || livingEntity.isDead()) {
                                task.cancel();
                                if (nameTag.isValid()) nameTag.remove();
                                return;
                            }

                            if (nameTag.getVehicle() == null) {
                                Location target = livingEntity.getLocation().add(0, livingEntity.getHeight(), 0);
                                nameTag.teleport(target);
                            }

                            if (tickCounter[0]++ % updateInterval == 0) {
                                updateText(livingEntity, nameTag, pinataConfig);
                            }
                        },
                        () -> {},
                        1L,
                        1L);
    }

    private void updateText(LivingEntity livingEntity, TextDisplay nameTag, PinataConfiguration pinataConfig) {
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

        List<String> lines = pinataConfig.appearance.nameTag.text;
        List<Component> components = new ArrayList<>();
        if (lines != null) {
            for (String line : lines) {
                components.add(messageUtils.parse(
                        null, line, messageUtils.getPinataTags(livingEntity), messageUtils.tag("timer", timeStr)));
            }
        }
        nameTag.text(Component.join(JoinConfiguration.newlines(), components));
    }
}
