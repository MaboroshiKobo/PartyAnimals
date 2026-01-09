package org.maboroshi.partyanimals.util;

import org.bukkit.Bukkit;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.MainConfig.VoteReminderSettings;

public class VoteReminder implements Runnable {
    private final PartyAnimals plugin;

    public VoteReminder(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        VoteReminderSettings settings = plugin.getConfiguration().getMainConfig().modules.vote.reminder;

        if (!settings.enabled) return;

        Bukkit.getOnlinePlayers().forEach(player -> {
            plugin.getEffectHandler().playEffects(settings.effects, player.getLocation(), false);
            settings.message.forEach(msg -> {
                plugin.getMessageUtils().send(player, msg);
            });
        });
    }
}
