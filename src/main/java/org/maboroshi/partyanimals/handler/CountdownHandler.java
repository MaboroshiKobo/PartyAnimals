package org.maboroshi.partyanimals.handler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.manager.BossBarManager;

public class CountdownHandler {
    private final PartyAnimals plugin;
    private final BossBarManager bossBarManager;
    private final EffectHandler effectHandler;
    private final Map<ScheduledTask, UUID> activeCountdowns = new ConcurrentHashMap<>();

    public CountdownHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.bossBarManager = plugin.getBossBarManager();
        this.effectHandler = plugin.getEffectHandler();
    }

    public void start(
            Location location,
            PinataConfiguration pinataConfig,
            String templateId,
            BiConsumer<Location, String> onComplete) {
        double countdownSeconds = pinataConfig.timer.countdown.duration;

        if (countdownSeconds <= 0) {
            onComplete.accept(location, templateId);
            return;
        }

        effectHandler.playEffects(pinataConfig.timer.countdown.start, location, true);

        int totalSeconds = (int) countdownSeconds;
        final int[] ticksRemaining = {(int) (countdownSeconds * 20)};
        final int[] lastSeconds = {totalSeconds};
        final int[] taskDurationTicks = {0};

        UUID countdownId = bossBarManager.createCountdownBossBar(location, pinataConfig, totalSeconds);

        ScheduledTask scheduledTask = Bukkit.getRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        location,
                        (task) -> {
                            ticksRemaining[0]--;
                            int displaySeconds = (int) Math.ceil(ticksRemaining[0] / 20.0);

                            if (ticksRemaining[0] <= 0) {
                                bossBarManager.removeCountdownBossBar(countdownId);
                                effectHandler.playEffects(pinataConfig.timer.countdown.end, location, true);
                                activeCountdowns.remove(task);
                                task.cancel();

                                onComplete.accept(location, templateId);
                                return;
                            }

                            bossBarManager.updateCountdownBar(
                                    countdownId, displaySeconds, totalSeconds, pinataConfig, ++taskDurationTicks[0]);

                            if (displaySeconds != lastSeconds[0]) {
                                effectHandler.playEffects(pinataConfig.timer.countdown.mid, location, true);
                                lastSeconds[0] = displaySeconds;
                            }
                        },
                        1L,
                        1L);
        activeCountdowns.put(scheduledTask, countdownId);
    }

    public void cancelAll() {
        activeCountdowns.keySet().forEach(ScheduledTask::cancel);
        activeCountdowns.clear();
    }
}
