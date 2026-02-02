package org.maboroshi.partyanimals;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.maboroshi.partyanimals.api.event.PartyAnimalsReloadEvent;
import org.maboroshi.partyanimals.command.PartyAnimalsCommand;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.handler.ActionHandler;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.handler.HitCooldownHandler;
import org.maboroshi.partyanimals.handler.ReflexHandler;
import org.maboroshi.partyanimals.hook.BetterModelHook;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.hook.PlaceholderAPIHook;
import org.maboroshi.partyanimals.listener.PinataListener;
import org.maboroshi.partyanimals.listener.VoteListener;
import org.maboroshi.partyanimals.manager.BossBarManager;
import org.maboroshi.partyanimals.manager.DatabaseManager;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.task.VoteReminder;
import org.maboroshi.partyanimals.util.Logger;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;
import org.maboroshi.partyanimals.util.UpdateChecker;

public final class PartyAnimals extends JavaPlugin {
    private static PartyAnimals plugin;
    private ConfigManager configManager;
    private Logger log;
    private MessageUtils messageUtils;
    private DatabaseManager databaseManager;
    private PinataManager pinataManager;
    private BossBarManager bossBarManager;
    private HitCooldownHandler hitCooldownHandler;
    private EffectHandler effectHandler;
    private ActionHandler actionHandler;
    private ReflexHandler reflexHandler;
    private VoteListener voteListener;
    private ScheduledTask voteReminderTask;

    private ModelEngineHook modelEngineHook;
    private BetterModelHook betterModelHook;

    @Override
    public void onEnable() {
        plugin = this;
        this.configManager = new ConfigManager(this, getDataFolder());
        this.log = new Logger(this);

        try {
            configManager.loadConfig();
            configManager.loadMessages();
        } catch (Exception e) {
            getLogger().severe("Failed to load configuration: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, 28389);

        NamespacedKeys.load(this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            log.info("Hooked into PlaceholderAPI.");
        }

        if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
            this.modelEngineHook = new ModelEngineHook(this);
            log.info("Hooked into ModelEngine.");
        } else {
            this.modelEngineHook = null;
        }

        if (getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            this.betterModelHook = new BetterModelHook(this);
            log.info("Hooked into BetterModel.");
        } else {
            this.betterModelHook = null;
        }

        this.messageUtils = new MessageUtils(configManager);
        this.bossBarManager = new BossBarManager(this);
        this.effectHandler = new EffectHandler(log);
        this.actionHandler = new ActionHandler(this);

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        setupModules();

        this.reflexHandler = new ReflexHandler(this);

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar()
                    .register(partyanimalsCommand.createCommand("partyanimals"), "Main command", List.of("pa"));
        });

        new UpdateChecker(this).checkForUpdates();
    }

    private void setupModules() {
        boolean pinataEnabled = configManager.getMainConfig().modules.pinata.enabled;

        if (pinataEnabled) {
            if (this.pinataManager == null) {
                this.pinataManager = new PinataManager(this, this.modelEngineHook, this.betterModelHook);
                this.hitCooldownHandler = new HitCooldownHandler(this);
                this.reflexHandler = new ReflexHandler(this);
                getServer().getPluginManager().registerEvents(new PinataListener(this), this);
                log.info("Pinata module enabled.");
            }
        } else {
            if (this.pinataManager != null) {
                this.pinataManager.cleanup();
                this.pinataManager = null;
                this.hitCooldownHandler = null;
                this.reflexHandler = null;
                log.info("Pinata module disabled.");
            }
        }

        boolean voteEnabled = configManager.getMainConfig().modules.vote.enabled;
        boolean hasNuVotifier = getServer().getPluginManager().isPluginEnabled("Votifier");

        if (voteEnabled && hasNuVotifier) {
            if (this.voteListener == null) {
                this.voteListener = new VoteListener(this);
                getServer().getPluginManager().registerEvents(this.voteListener, this);
                log.info("Vote module enabled.");
            }
            var reminderSettings = configManager.getMainConfig().modules.vote.reminder;
            if (reminderSettings.enabled && voteReminderTask == null) {
                long intervalTicks = reminderSettings.interval * 20L;
                this.voteReminderTask = getServer()
                        .getGlobalRegionScheduler()
                        .runAtFixedRate(
                                this,
                                (task) -> {
                                    new VoteReminder(this).run();
                                },
                                intervalTicks,
                                intervalTicks);
            }
        } else {
            if (voteReminderTask != null) {
                voteReminderTask.cancel();
                voteReminderTask = null;
            }
            if (this.voteListener != null) {
                HandlerList.unregisterAll(this.voteListener);
                this.voteListener = null;
                log.info("Vote module disabled.");
            }

            if (voteEnabled && !hasNuVotifier) {
                log.warn("Vote module is enabled, but NuVotifier is not installed! Voting features will not work.");
            }
        }
    }

    public boolean reload() {
        try {
            if (pinataManager != null) {
                pinataManager.cleanup(false);
            }

            if (databaseManager != null) {
                databaseManager.disconnect();
            }

            configManager.loadConfig();
            configManager.loadMessages();

            if (databaseManager != null) {
                databaseManager.connect();
            }

            setupModules();

            if (pinataManager != null) {
                reloadPinatas();
            }

            getServer().getPluginManager().callEvent(new PartyAnimalsReloadEvent());

            return true;
        } catch (Exception e) {
            log.warn("Failed to reload configuration: " + e.getMessage());
            return false;
        }
    }

    private void reloadPinatas() {
        for (World world : getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (pinataManager.isPinata(entity)) {
                    pinataManager.activatePinata(entity);
                }
            }
        }
        log.info("Reloaded pinata entities and tasks.");
    }

    @Override
    public void onDisable() {
        if (pinataManager != null) {
            pinataManager.cleanup();
        }
        if (bossBarManager != null) {
            bossBarManager.removeAll();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
    }

    public static PartyAnimals getPlugin() {
        return plugin;
    }

    public Logger getPluginLogger() {
        return log;
    }

    public ConfigManager getConfiguration() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public PinataManager getPinataManager() {
        return pinataManager;
    }

    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }

    public HitCooldownHandler getHitCooldownHandler() {
        return hitCooldownHandler;
    }

    public EffectHandler getEffectHandler() {
        return effectHandler;
    }

    public ActionHandler getActionHandler() {
        return actionHandler;
    }

    public ReflexHandler getReflexHandler() {
        return reflexHandler;
    }
}
