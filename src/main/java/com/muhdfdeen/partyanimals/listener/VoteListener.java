package com.muhdfdeen.partyanimals.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.ConfigManager;
import com.muhdfdeen.partyanimals.config.settings.MainConfig.VoteEvent;
import com.muhdfdeen.partyanimals.handler.EffectHandler;
import com.muhdfdeen.partyanimals.handler.RewardHandler;
import com.muhdfdeen.partyanimals.util.Logger;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;

public class VoteListener implements Listener {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final Logger log;
    private final EffectHandler effectHandler;
    private final RewardHandler rewardHandler;

    public VoteListener(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.log = plugin.getPluginLogger();
        this.effectHandler = plugin.getEffectHandler();
        this.rewardHandler = plugin.getRewardHandler();
    }

    @EventHandler
    public void onVoteEvent(VotifierEvent event) {
        if (!config.getMainConfig().modules.vote().enabled()) return;
        Vote vote = event.getVote();
        String serviceName = vote.getServiceName();
        String playerName = vote.getUsername();
        String address = vote.getAddress();
        String timeStamp = vote.getTimeStamp();
        log.debug("Received vote from " + playerName + " via " + serviceName + " at " + timeStamp + " (IP: " + address + ")");

        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.getScheduler().run(plugin, (task) -> {
                VoteEvent voteEvent = config.getMainConfig().modules.vote().events().vote();
                if (!voteEvent.enabled()) return;

                effectHandler.playEffects(voteEvent.effects(), player.getLocation(), false);
                rewardHandler.process(player, voteEvent.rewards().values());
            }, null);
        } else {
            if (config.getMainConfig().modules.vote().offline().enabled()) {
                // Queue offline rewards
            }
            // Immediately send offline rewards
        }
    }
}
