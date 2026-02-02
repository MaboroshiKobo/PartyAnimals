package org.maboroshi.partyanimals.hook;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import org.bukkit.entity.LivingEntity;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.util.Logger;

public class ModelEngineHook {
    private final Logger log;

    public ModelEngineHook(PartyAnimals plugin) {
        this.log = plugin.getPluginLogger();
    }

    public boolean applyModel(LivingEntity pinata, String modelId) {
        ModeledEntity modeledEntity = ModelEngineAPI.getOrCreateModeledEntity(pinata);
        ActiveModel activeModel = ModelEngineAPI.createActiveModel(modelId);
        if (activeModel == null) {
            log.warn("Failed to apply ModelEngine model: " + modelId);
            return false;
        }
        modeledEntity.addModel(activeModel, true);
        return true;
    }
}
