package org.maboroshi.partyanimals.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import com.ticxo.modelengine.api.model.bone.BoneBehaviorTypes;
import com.ticxo.modelengine.api.model.bone.type.NameTag;
import com.ticxo.modelengine.api.mount.controller.MountControllerSupplier;
import java.util.function.Consumer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
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
            model.setHitboxScale(scale);
        }

        var attribute = pinata.getAttribute(Attribute.SCALE);
        if (attribute != null) {
            attribute.setBaseValue(scale);
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

        for (ActiveModel model : modeledEntity.getModels().values()) {
            var tagOpt = model.getBone(boneId).flatMap(bone -> bone.getBoneBehavior(BoneBehaviorTypes.NAMETAG));
            if (tagOpt.isPresent()) {
                consumer.accept((NameTag) tagOpt.get());
                return true;
            }
        }

        Log.warn("No valid NAMETAG bone found for entity " + pinata.getUniqueId() + " using id: " + boneId);
        return false;
    }

    public boolean mountPassenger(LivingEntity pinata, Entity passenger) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) {
            Log.debug("MEG mount failed: No ModeledEntity found for " + pinata.getUniqueId());
            return false;
        }

        for (ActiveModel model : modeledEntity.getModels().values()) {
            var mountManagerOpt = model.getMountManager();
            if (mountManagerOpt.isEmpty()) {
                Log.debug("MEG mount failed: No MountManager present on model "
                        + model.getBlueprint().getName());
                continue;
            }

            var mountManager = mountManagerOpt.get();
            Log.debug("Available MEG seats: " + mountManager.getSeats().keySet());

            if (mountManager.getSeats().isEmpty() && mountManager.getDriverBone() == null) {
                Log.debug("MEG mount failed: No seats registered on model.");
                return false;
            }

            mountManager.setCanDrive(false);
            mountManager.setCanRide(true);

            MountControllerSupplier supplier =
                    ModelEngineAPI.getMountControllerTypeRegistry().getDefault();
            if (supplier == null) {
                Log.warn("No default MountControllerSupplier found in ModelEngine registry.");
                return false;
            }

            if (mountManager.getSeat("seat").isPresent() && mountManager.mountPassenger("seat", passenger, supplier)) {
                Log.debug("Successfully mounted to 'seat'.");
                return true;
            }
            if (mountManager.getSeat("p_seat").isPresent()
                    && mountManager.mountPassenger("p_seat", passenger, supplier)) {
                Log.debug("Successfully mounted to 'p_seat'.");
                return true;
            }
            if (mountManager.getSeat("p_mount").isPresent()
                    && mountManager.mountPassenger("p_mount", passenger, supplier)) {
                Log.debug("Successfully mounted to 'p_mount'.");
                return true;
            }
            if (mountManager.mountAvailable(passenger, supplier)) {
                Log.debug("Successfully mounted to first available seat.");
                return true;
            }
        }

        Log.debug("MEG mounting failed; falling back to vanilla passenger.");
        return false;
    }

    public void dismountAll(LivingEntity pinata) {
        ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(pinata.getUniqueId());
        if (modeledEntity == null) return;

        for (ActiveModel model : modeledEntity.getModels().values()) {
            model.getMountManager().ifPresent(mountManager -> mountManager.dismountAll());
        }
    }
}
