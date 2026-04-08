package org.maboroshi.partyanimals.handler;

import com.ticxo.modelengine.api.model.bone.type.NameTag;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Vector3f;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class ModelEngineNameTagHandler {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;
    private final ModelEngineHook modelEngineHook;

    public ModelEngineNameTagHandler(PartyAnimals plugin, ModelEngineHook modelEngineHook) {
        this.plugin = plugin;
        this.messageUtils = plugin.getMessageUtils();
        this.modelEngineHook = modelEngineHook;
    }

    public boolean attach(LivingEntity pinata, PinataConfiguration pinataConfig) {
        if (modelEngineHook == null) return false;
        if (!modelEngineHook.hasModeledEntity(pinata)) return false;

        String modelEngineBoneId = pinataConfig.appearance.nameTag.modelEngineBoneId;
        boolean configured = modelEngineHook.configureNameTag(pinata, modelEngineBoneId, nameTag -> {
            applyNameTagStyle(nameTag, pinataConfig);
            updateNameTagText(nameTag, pinata, pinataConfig);
        });
        if (!configured) return false;

        scheduleModelEngineNameTagUpdate(pinata, pinataConfig, modelEngineBoneId);
        return true;
    }

    private void scheduleModelEngineNameTagUpdate(
            LivingEntity livingEntity, PinataConfiguration pinataConfig, String modelEngineBoneId) {
        int updateInterval = Math.max(1, pinataConfig.appearance.nameTag.updateTextInterval);

        livingEntity
                .getScheduler()
                .runAtFixedRate(
                        plugin,
                        task -> {
                            if (!livingEntity.isValid() || livingEntity.isDead()) {
                                task.cancel();
                                return;
                            }

                            modelEngineHook.configureNameTag(
                                    livingEntity,
                                    modelEngineBoneId,
                                    nameTag -> updateNameTagText(nameTag, livingEntity, pinataConfig));
                        },
                        () -> {},
                        1L,
                        updateInterval);
    }

    private void applyNameTagStyle(NameTag nameTag, PinataConfiguration pinataConfig) {
        nameTag.setVisible(true);
        nameTag.setTextOpacity((byte) 255);
        nameTag.setAlignment(pinataConfig.appearance.nameTag.textAlignment);
        nameTag.setSeeThrough(pinataConfig.appearance.nameTag.seeThrough);
        nameTag.setBillboard(pinataConfig.appearance.nameTag.billboard);
        nameTag.setShadow(pinataConfig.appearance.nameTag.shadow.enabled);
        nameTag.setScale(new Vector3f(
                (float) pinataConfig.appearance.nameTag.transformation.scale.x,
                (float) pinataConfig.appearance.nameTag.transformation.scale.y,
                (float) pinataConfig.appearance.nameTag.transformation.scale.z));

        if (pinataConfig.appearance.nameTag.background.enabled) {
            nameTag.setUseDefaultBackgroundColor(false);
            nameTag.setBackgroundColor(toArgbInt(
                    pinataConfig.appearance.nameTag.background.alpha,
                    pinataConfig.appearance.nameTag.background.red,
                    pinataConfig.appearance.nameTag.background.green,
                    pinataConfig.appearance.nameTag.background.blue));
        } else {
            nameTag.setUseDefaultBackgroundColor(false);
            nameTag.setBackgroundColor(0);
        }
    }

    private void updateNameTagText(NameTag nameTag, LivingEntity livingEntity, PinataConfiguration pinataConfig) {
        nameTag.setComponent(
                buildNameTagComponent(livingEntity, pinataConfig, getCurrentTimerString(livingEntity, pinataConfig)));
    }

    private int toArgbInt(int alpha, int red, int green, int blue) {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
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
