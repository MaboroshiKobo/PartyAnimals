package org.maboroshi.partyanimals.hook;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import kr.toxicity.model.api.tracker.TrackerUpdateAction;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.util.Log;

public class BetterModelHook {

    public BetterModelHook(PartyAnimals plugin) {
    }

    public boolean applyModel(LivingEntity pinata, String modelId) {
        Log.debug("Applying BetterModel model: " + modelId + " to entity: " + pinata.getUniqueId());

        return BetterModel.model(modelId)
                .map(blueprint -> {
                    EntityTracker tracker = blueprint.getOrCreate(BukkitAdapter.adapt(pinata));
                    if (tracker != null) {
                        tracker.forceUpdate(true);
                        return true;
                    } else {
                        Log.warn("Failed to create EntityTracker for: " + modelId + " (Entity might not be ready)");
                        return false;
                    }
                })
                .orElseGet(() -> {
                    Log.warn("Failed to find BetterModel blueprint for: " + modelId);
                    return false;
                });
    }

    public void setGlowing(LivingEntity pinata, boolean glowing, NamedTextColor color) {
        int rgb = (color != null) ? color.value() : 0xFFFFFF;
        Log.debug("Setting glowing: " + glowing + " (Color: " + rgb + ") for entity: " + pinata.getUniqueId());

        BetterModel.registry(BukkitAdapter.adapt(pinata))
                .ifPresentOrElse(
                        registry -> {
                            registry.trackers().forEach(tracker -> {
                                if (glowing) {
                                    tracker.update(
                                            TrackerUpdateAction.glow(true).then(TrackerUpdateAction.glowColor(rgb)));
                                } else {
                                    tracker.update(TrackerUpdateAction.glow(false));
                                }
                            });
                        },
                        () -> {
                            Log.debug("Skipping setGlowing: No Registry found for " + pinata.getUniqueId());
                        });
    }

    public void playAnimation(LivingEntity pinata, String animationId) {
        BetterModel.registry(BukkitAdapter.adapt(pinata))
                .ifPresentOrElse(
                        registry -> {
                            Log.debug("Playing animation: " + animationId + " for entity: " + pinata.getUniqueId());
                            registry.trackers().forEach(tracker -> {
                                tracker.animate(animationId, AnimationModifier.DEFAULT_WITH_PLAY_ONCE);
                            });
                        },
                        () -> {
                            Log.debug("Skipping animation " + animationId + ": No Registry found for "
                                    + pinata.getUniqueId());
                        });
    }
}
