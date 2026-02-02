package org.maboroshi.partyanimals.hook;

import kr.toxicity.model.api.BetterModel;
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
}
