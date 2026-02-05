package org.maboroshi.partyanimals.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.MainConfig;
import org.maboroshi.partyanimals.listener.VoteListener;
import org.maboroshi.partyanimals.task.VoteReminder;
import org.maboroshi.partyanimals.util.Logger;

public class VoteManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private VoteListener voteListener;
    private ScheduledTask voteReminderTask;

    public VoteManager(PartyAnimals plugin) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
    }

    public void enable() {
        boolean voteEnabled = plugin.getConfiguration().getMainConfig().modules.vote.enabled;
        boolean hasNuVotifier = Bukkit.getPluginManager().isPluginEnabled("Votifier");

        if (!voteEnabled) return;

        if (!hasNuVotifier) {
            log.warn("Vote module is enabled, but NuVotifier is not installed! Voting features will not work.");
            return;
        }

        if (this.voteListener == null) {
            this.voteListener = new VoteListener(plugin);
            Bukkit.getPluginManager().registerEvents(this.voteListener, plugin);
            log.info("Vote module enabled.");
        }

        startReminder();
    }

    public void disable() {
        if (voteReminderTask != null) {
            voteReminderTask.cancel();
            voteReminderTask = null;
        }
        if (this.voteListener != null) {
            HandlerList.unregisterAll(this.voteListener);
            this.voteListener = null;
            log.info("Vote module disabled.");
        }
    }

    private void startReminder() {
        MainConfig.VoteReminderSettings settings = plugin.getConfiguration().getMainConfig().modules.vote.reminder;
        if (settings.enabled && voteReminderTask == null) {
            long intervalTicks = settings.interval * 20L;
            this.voteReminderTask = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, (task) -> new VoteReminder(plugin).run(), intervalTicks, intervalTicks);
        }
    }
}
