package org.maboroshi.partyanimals.hook;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.animation.AnimationModifier;
import org.bukkit.entity.LivingEntity;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.util.Logger;

public class BetterModelHook {
    private final Logger log;

    public BetterModelHook(PartyAnimals plugin) {
        this.log = plugin.getPluginLogger();
    }

    public boolean applyModel(LivingEntity pinata, String modelId) {
        var model = BetterModel.modelOrNull(modelId);
        if (model == null) {
            log.warn("Failed to apply BetterModel model: " + modelId);
            return false;
        }
        model.getOrCreate(pinata);
        return true;
    }

    public void playAnimation(LivingEntity pinata, String animationId) {
        BetterModel.registry(pinata).ifPresent(registry -> {
            registry.trackers()
                    .forEach(tracker -> tracker.animate(animationId, AnimationModifier.DEFAULT_WITH_PLAY_ONCE));
        });
    }
}
