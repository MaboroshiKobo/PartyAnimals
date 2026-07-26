package org.maboroshi.partyanimals.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.type.NameTag;
import java.util.function.Consumer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.maboroshi.partyanimals.util.Log;

public class ModelEngineHook {
    public boolean applyModel(LivingEntity pinata, String modelId) {
        Log.debug("Applying ModelEngine model: " + modelId + " to entity: " + pinata.getUniqueId());
        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(pinata);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel == null) {
            Log.warn("Failed to create ActiveModel for: " + modelId);
            return false;
        }
        modeledEntity.addModel(activeModel, false);
        return true;
    }

    public void playAnimation(LivingEntity pinata, String animationId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            Log.debug("Skipping animation " + animationId + ": No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }
        Log.debug("Playing animation: " + animationId + " for entity: " + pinata.getUniqueId());
        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getAnimationHandler().playAnimation(animationId, 0, 0, 1, false);
        }
    }

    public void setScale(LivingEntity pinata, double scale) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            Log.debug("Skipping setScale: No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }
        Log.debug("Setting scale to: " + scale + " for entity: " + pinata.getUniqueId());
        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.setScale(scale);
        }
    }

    public void setGlowing(LivingEntity pinata, boolean glowing, NamedTextColor color) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            Log.debug("Skipping setGlowing: No ModeledEntity found for " + pinata.getUniqueId());
            return;
        }

        int rgb = color.value();
        Log.debug("Setting glowing: " + glowing + " (Color: " + rgb + ") for entity: " + pinata.getUniqueId());

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.setGlowing(glowing);
            model.setGlowColor(rgb);
        }
    }

    public boolean hasModeledEntity(LivingEntity pinata) {
        return ModelEngineAPI.getModeledEntity(pinata.getUniqueId()) != null;
    }

    public boolean configureNameTag(LivingEntity pinata, String boneId, Consumer<NameTag> consumer) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            Log.debug("NAMETAG skipped: no modeled entity for " + pinata.getUniqueId());
            return false;
        }

        for (var modelEntry : modeledEntity.getModels().entrySet()) {
            ActiveModel model = modelEntry.getValue();
            var boneOpt = model.getBone(boneId);
            if (boneOpt.isEmpty()) {
                continue;
            }

            var behaviorOpt = boneOpt.get().getBoneBehavior(BoneBehaviorTypes.NAMETAG);
            if (behaviorOpt.isEmpty()) {
                continue;
            }

            NameTag nameTag = (NameTag) behaviorOpt.get();
            consumer.accept(nameTag);
            return true;
        }

        Log.warn("No valid NAMETAG bone found for entity " + pinata.getUniqueId() + " using id: " + boneId);
        return false;
    }
}
