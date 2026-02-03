package org.maboroshi.partyanimals;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.maboroshi.partyanimals.api.event.PartyAnimalsReloadEvent;
import org.maboroshi.partyanimals.command.PartyAnimalsCommand;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.handler.ActionHandler;
import org.maboroshi.partyanimals.handler.EffectHandler;
import org.maboroshi.partyanimals.hook.BetterModelHook;
import org.maboroshi.partyanimals.hook.ModelEngineHook;
import org.maboroshi.partyanimals.hook.PlaceholderAPIHook;
import org.maboroshi.partyanimals.listener.PinataListener;
import org.maboroshi.partyanimals.manager.BossBarManager;
import org.maboroshi.partyanimals.manager.DatabaseManager;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.manager.VoteManager;
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
    private BossBarManager bossBarManager;
    private EffectHandler effectHandler;
    private ActionHandler actionHandler;

    private PinataManager pinataManager;
    private VoteManager voteManager;

    private ModelEngineHook modelEngineHook;
    private BetterModelHook betterModelHook;

    @Override
    public void onEnable() {
        plugin = this;
        this.log = new Logger(this);
        this.configManager = new ConfigManager(this, getDataFolder());

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
        setupHooks();

        this.messageUtils = new MessageUtils(configManager);
        this.bossBarManager = new BossBarManager(this);
        this.effectHandler = new EffectHandler(log);
        this.actionHandler = new ActionHandler(this);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        setupModules();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            PartyAnimalsCommand partyanimalsCommand = new PartyAnimalsCommand(this);
            event.registrar()
                    .register(partyanimalsCommand.createCommand("partyanimals"), "Main command", List.of("pa"));
        });

        new UpdateChecker(this).checkForUpdates();
    }

    private void setupHooks() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            log.info("Hooked into PlaceholderAPI.");
        }

        if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
            this.modelEngineHook = new ModelEngineHook(this);
            log.info("Hooked into ModelEngine.");
        }

        if (getServer().getPluginManager().isPluginEnabled("BetterModel")) {
            this.betterModelHook = new BetterModelHook(this);
            log.info("Hooked into BetterModel.");
        }
    }

    private void setupModules() {
        boolean pinataEnabled = configManager.getMainConfig().modules.pinata.enabled;
        if (pinataEnabled) {
            if (this.pinataManager == null) {
                this.pinataManager = new PinataManager(this, this.modelEngineHook, this.betterModelHook);
                getServer().getPluginManager().registerEvents(new PinataListener(this), this);
                log.info("Pinata module enabled.");
            }
        } else {
            if (this.pinataManager != null) {
                this.pinataManager.cleanup();
                this.pinataManager = null;
                log.info("Pinata module disabled.");
            }
        }

        if (this.voteManager == null) {
            this.voteManager = new VoteManager(this);
        }

        this.voteManager.enable();
    }

    public boolean reload() {
        try {
            if (pinataManager != null) pinataManager.cleanup(false);
            if (voteManager != null) voteManager.disable();
            if (databaseManager != null) databaseManager.disconnect();

            configManager.loadConfig();
            configManager.loadMessages();

            if (databaseManager != null) databaseManager.connect();

            setupModules();

            if (pinataManager != null) reloadPinatas();

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
        if (pinataManager != null) pinataManager.cleanup();
        if (voteManager != null) voteManager.disable();
        if (bossBarManager != null) bossBarManager.removeAll();
        if (databaseManager != null) databaseManager.disconnect();
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

    public EffectHandler getEffectHandler() {
        return effectHandler;
    }

    public ActionHandler getActionHandler() {
        return actionHandler;
    }
}
