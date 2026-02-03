package org.maboroshi.partyanimals.handler;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class NameTagHandler {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;

    public NameTagHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
    }

    public void attach(LivingEntity pinata) {
        if (pinata.getPassengers() != null) {
            for (Entity passenger : pinata.getPassengers()) {
                if (passenger instanceof TextDisplay) {
                    return;
                }
            }
        }

        PinataConfiguration pinataConfig = plugin.getPinataManager().getPinataConfig(pinata);

        Location location = pinata.getLocation();
        TextDisplay nameTag = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);

        nameTag.setPersistent(false);

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

        pinata.addPassenger(nameTag);

        scheduleNameTagUpdate(pinata, nameTag);
    }

    public void scheduleNameTagUpdate(LivingEntity livingEntity, TextDisplay nameTag) {
        PinataConfiguration pinataConfig = plugin.getPinataManager().getPinataConfig(livingEntity);
        int interval = pinataConfig.appearance.nameTag.updateTextInterval;
        if (interval <= 0) return;

        long intervalTicks = (long) interval;
        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        (task) -> {
                            if (!nameTag.isValid() || !livingEntity.isValid()) {
                                task.cancel();
                                if (nameTag.isValid()) nameTag.remove();
                                return;
                            }

                            String timeStr = "∞";
                            if (pinataConfig.timer.timeout.enabled && pinataConfig.timer.timeout.duration > 0) {
                                long spawnTime = livingEntity
                                        .getPersistentDataContainer()
                                        .getOrDefault(
                                                NamespacedKeys.PINATA_SPAWN_TIME,
                                                PersistentDataType.LONG,
                                                System.currentTimeMillis());
                                int totalTimeout = pinataConfig.timer.timeout.duration;
                                int remaining = Math.max(
                                        0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
                                timeStr = String.format("%02d:%02d", remaining / 60, remaining % 60);
                            }

                            List<String> lines = pinataConfig.appearance.nameTag.text;
                            List<Component> components = new ArrayList<>();
                            if (lines != null) {
                                for (String line : lines) {
                                    components.add(messageUtils.parse(
                                            null,
                                            line,
                                            messageUtils.getPinataTags(livingEntity),
                                            messageUtils.tag("timer", timeStr)));
                                }
                            }
                            nameTag.text(Component.join(JoinConfiguration.newlines(), components));
                        },
                        () -> {},
                        intervalTicks,
                        intervalTicks);
    }
}
