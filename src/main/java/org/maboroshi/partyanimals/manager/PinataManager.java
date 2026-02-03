package org.maboroshi.partyanimals.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.handler.BehaviorHandler;
import org.maboroshi.partyanimals.handler.CountdownHandler;
import org.maboroshi.partyanimals.handler.HitCooldownHandler;
import org.maboroshi.partyanimals.handler.NameTagHandler;
import org.maboroshi.partyanimals.handler.ReflexHandler;
import org.maboroshi.partyanimals.hook.BetterModelHook;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class PinataManager {
    private final PartyAnimals plugin;
    private final Logger log;
    private final ConfigManager config;
    private final BossBarManager bossBarManager;
    private final MessageUtils messageUtils;
    private final ModelEngineHook modelEngineHook;
    private final BetterModelHook betterModelHook;

    private final PinataFactory pinataFactory;
    private final NameTagHandler nameTagHandler;
    private final BehaviorHandler behaviorHandler;
    private final CountdownHandler countdownHandler;
    private final HitCooldownHandler hitCooldownHandler;
    private final ReflexHandler reflexHandler;

    private final Map<UUID, LivingEntity> activePinatas = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> timeoutTasks = new ConcurrentHashMap<>();

    public PinataManager(PartyAnimals plugin, ModelEngineHook modelEngineHook, BetterModelHook betterModelHook) {
        this.plugin = plugin;
        this.log = plugin.getPluginLogger();
        this.config = plugin.getConfiguration();
        this.bossBarManager = plugin.getBossBarManager();
        this.messageUtils = plugin.getMessageUtils();
        this.modelEngineHook = modelEngineHook;
        this.betterModelHook = betterModelHook;

        this.pinataFactory = new PinataFactory(plugin, modelEngineHook, betterModelHook);
        this.nameTagHandler = new NameTagHandler(plugin);
        this.behaviorHandler = new BehaviorHandler(plugin);
        this.countdownHandler = new CountdownHandler(plugin);
        this.hitCooldownHandler = new HitCooldownHandler(plugin);
        this.reflexHandler = new ReflexHandler(plugin);
    }

    public void spawnPinata(Location location, String templateId) {
        pinataFactory.spawn(location, templateId);
    }

    public void startCountdown(Location location, String templateId) {
        PinataConfiguration pinataConfig = config.getPinataConfig(templateId);

        if (pinataConfig == null) {
            log.warn("Tried to start countdown for invalid pinata template: " + templateId);
            return;
        }

        countdownHandler.start(location, pinataConfig, templateId, (loc, id) -> spawnPinata(loc, id));
    }

    public void activatePinata(LivingEntity pinata) {
        if (pinata == null || pinata.isDead()) return;

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        activePinatas.put(pinata.getUniqueId(), pinata);
        behaviorHandler.apply(pinata, pinataConfig);
        bossBarManager.startTracking(pinata, pinataConfig);
        startTimeoutTask(pinata);

        if (pinataConfig.appearance.nameTag.enabled) {
            boolean tagFound = false;
            if (pinata.getPassengers() != null) {
                for (Entity passenger : pinata.getPassengers()) {
                    if (passenger instanceof TextDisplay textDisplay) {
                        nameTagHandler.scheduleNameTagUpdate(pinata, textDisplay);
                        tagFound = true;
                        break;
                    }
                }
            }

            if (!tagFound) {
                nameTagHandler.attach(pinata);
            }
        }
    }

    public void playAnimation(LivingEntity pinata, String animationId) {
        if (animationId == null || animationId.isEmpty()) return;

        if (modelEngineHook != null) {
            modelEngineHook.playAnimation(pinata, animationId);
        }

        if (betterModelHook != null) {
            betterModelHook.playAnimation(pinata, animationId);
        }
    }

    public void cleanup() {
        cleanup(true);
    }

    public void cleanup(boolean killEntities) {
        log.debug("Running PinataManager cleanup (Kill entities: " + killEntities + ")...");
        bossBarManager.removeAll();

        for (LivingEntity entity : List.copyOf(activePinatas.values())) {
            if (killEntities) {
                safelyRemovePinata(entity);
            }
        }

        activePinatas.clear();
        timeoutTasks.values().forEach(ScheduledTask::cancel);
        timeoutTasks.clear();

        countdownHandler.cancelAll();
    }

    public void safelyRemovePinata(LivingEntity pinata) {
        if (pinata.getPassengers() != null) {
            for (Entity passenger : new ArrayList<>(pinata.getPassengers())) {
                passenger.remove();
            }
        }
        removeActiveBossBar(pinata);
        cleanupGlowTeam(pinata);
        if (pinata.isValid()) {
            pinata.remove();
        }
    }

    public void removeActiveBossBar(LivingEntity pinata) {
        bossBarManager.removePinataBossBar(pinata.getUniqueId());
        activePinatas.remove(pinata.getUniqueId());

        ScheduledTask task = timeoutTasks.remove(pinata.getUniqueId());
        if (task != null) task.cancel();
    }

    private void cleanupGlowTeam(LivingEntity pinata) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = mainBoard.getEntryTeam(pinata.getUniqueId().toString());
        if (team != null && team.getName().startsWith("PA_")) {
            team.removeEntry(pinata.getUniqueId().toString());
        }
    }

    private void startTimeoutTask(LivingEntity pinata) {
        var existing = timeoutTasks.remove(pinata.getUniqueId());
        if (existing != null) existing.cancel();

        PinataConfiguration pinataConfig = getPinataConfig(pinata);

        if (!pinataConfig.timer.timeout.enabled) return;

        long spawnTime = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_SPAWN_TIME, PersistentDataType.LONG, 0L);
        int timeoutSeconds = pinataConfig.timer.timeout.duration;
        if (spawnTime <= 0) spawnTime = System.currentTimeMillis();

        long elapsedMillis = System.currentTimeMillis() - spawnTime;
        long remainingMillis = (timeoutSeconds * 1000L) - elapsedMillis;
        long remainingTicks = remainingMillis / 50;

        if (remainingTicks <= 0) {
            log.debug("Restoring pinata but timeout passed. Removing.");
            safelyRemovePinata(pinata);
            return;
        }

        var task = pinata.getScheduler()
                .runDelayed(
                        plugin,
                        (t) -> {
                            if (pinata.isValid() && isPinata(pinata)) {
                                safelyRemovePinata(pinata);
                                String timeoutMsg = config.getMessageConfig().pinata.events.timeout;
                                messageUtils.send(plugin.getServer(), timeoutMsg);
                            }
                            timeoutTasks.remove(pinata.getUniqueId());
                        },
                        () -> {},
                        remainingTicks);

        timeoutTasks.put(pinata.getUniqueId(), task);
    }

    public PinataConfiguration getPinataConfig(LivingEntity entity) {
        if (entity == null) return config.getPinataConfig("default");
        String id = entity.getPersistentDataContainer().get(NamespacedKeys.PINATA_TEMPLATE, PersistentDataType.STRING);
        PinataConfiguration pc = config.getPinataConfig(id != null ? id : "default");
        return pc != null ? pc : config.getPinataConfig("default");
    }

    public boolean isPinata(LivingEntity pinata) {
        return pinata.getPersistentDataContainer().has(NamespacedKeys.IS_PINATA, PersistentDataType.BOOLEAN);
    }

    public boolean isPinataAlive() {
        return !activePinatas.isEmpty();
    }

    public int getActivePinataCount() {
        return activePinatas.size();
    }

    public LivingEntity getNearestPinata(Location location) {
        if (activePinatas.isEmpty() || location == null) return null;

        LivingEntity nearest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (LivingEntity pinata : activePinatas.values()) {
            if (!pinata.isValid() || !pinata.getWorld().equals(location.getWorld())) continue;

            double distSq = pinata.getLocation().distanceSquared(location);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                nearest = pinata;
            }
        }
        return nearest;
    }

    public int getPinataHealth(LivingEntity pinata) {
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, 0);
    }

    public int getPinataMaxHealth(LivingEntity pinata) {
        if (pinata == null || !pinata.isValid()) return 0;
        return pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, 0);
    }

    public HitCooldownHandler getHitCooldownHandler() {
        return hitCooldownHandler;
    }

    public ReflexHandler getReflexHandler() {
        return reflexHandler;
    }
}
