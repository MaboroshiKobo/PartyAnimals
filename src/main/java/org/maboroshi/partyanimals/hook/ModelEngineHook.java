package org.maboroshi.partyanimals.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.type.Mount;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.util.Logger;

public class ModelEngineHook {
    private final Logger log;

    public ModelEngineHook(PartyAnimals plugin) {
        this.log = plugin.getPluginLogger();
    }

    public boolean applyModel(LivingEntity pinata, String modelId) {
        log.debug("Applying ModelEngine model: " + modelId + " to entity: " + pinata.getUniqueId());
        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(pinata);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel == null) {
            log.warn("Failed to create ActiveModel for: " + modelId);
            return false;
        }
        modeledEntity.addModel(activeModel, false);
        return true;
    }

    public void playAnimation(LivingEntity pinata, String animationId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            log.debug("Skipping animation " + animationId + ": No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }
        log.debug("Playing animation: " + animationId + " for entity: " + pinata.getUniqueId());
        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getAnimationHandler().playAnimation(animationId, 0, 0, 1, false);
        }
    }

    public void setScale(LivingEntity pinata, double scale) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            log.debug("Skipping setScale: No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }
        log.debug("Setting scale to: " + scale + " for entity: " + pinata.getUniqueId());
        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.setScale(scale);
        }
    }

    public void setGlowing(LivingEntity pinata, boolean glowing, NamedTextColor color) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            log.debug("Skipping setGlowing: No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }

        int rgb = color.value();
        log.debug("Setting glowing: " + glowing + " (Color: " + rgb + ") for entity: " + pinata.getUniqueId());

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.setGlowing(glowing);
            model.setGlowColor(rgb);
        }
    }

    public void addPassenger(LivingEntity pinata, Entity passenger, String boneId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) return;

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getBone(boneId).ifPresent(bone -> {
                bone.getBoneBehavior(BoneBehaviorTypes.MOUNT).ifPresent(behavior -> {
                    if (behavior instanceof Mount mount) {
                        mount.addPassenger(passenger);
                    }
                });
            });
            return;
        }
    }
}
