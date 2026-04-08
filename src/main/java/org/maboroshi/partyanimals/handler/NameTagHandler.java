package org.maboroshi.partyanimals.handler;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.manager.PinataManager;

public class NameTagHandler {
    private final PinataManager pinataManager;
    private final VanillaNameTagHandler vanillaNameTagHandler;
    private final ModelEngineNameTagHandler modelEngineNameTagHandler;

    public NameTagHandler(PartyAnimals plugin, PinataManager pinataManager, ModelEngineHook modelEngineHook) {
        this.pinataManager = pinataManager;
        this.vanillaNameTagHandler = new VanillaNameTagHandler(plugin, pinataManager);
        this.modelEngineNameTagHandler = new ModelEngineNameTagHandler(plugin, modelEngineHook);
    }

    public void attach(LivingEntity pinata) {
        PinataConfiguration pinataConfig = pinataManager.getPinataConfig(pinata);

        if (modelEngineNameTagHandler.attach(pinata, pinataConfig)) {
            vanillaNameTagHandler.removeAttachedNameTag(pinata);
            return;
        }

        vanillaNameTagHandler.attach(pinata, pinataConfig);
    }

    public void scheduleNameTagUpdate(LivingEntity livingEntity, TextDisplay nameTag) {
        vanillaNameTagHandler.scheduleNameTagUpdate(livingEntity, nameTag);
    }
}
