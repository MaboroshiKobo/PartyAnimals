package org.maboroshi.partyanimals.util;

import org.bukkit.Bukkit;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;

public class Logger {
    private final PartyAnimals plugin;
    private final MessageUtils messageUtils;

    public Logger(PartyAnimals plugin, MessageUtils messageUtils) {
        this.plugin = plugin;
        this.messageUtils = messageUtils;
    }

    private ConfigManager getConfig() {
        return plugin.getConfiguration();
    }

    private void log(String colorTag, String message) {
        ConfigManager config = getConfig();
        String prefix;
        if (config != null && config.getMessageConfig() != null) prefix = config.getMessageConfig().prefix;
        else prefix = "<color:#51CF66><bold>Party Animals</bold> ➟ </color>";
        messageUtils.send(Bukkit.getConsoleSender(), prefix + colorTag + message);
    }

    public void debug(String message) {
        ConfigManager config = getConfig();
        if (config != null && config.getMainConfig().debug) log("<gray>[DEBUG] </gray>", message);
    }

    public void info(String message) {
        log("", message);
    }

    public void warn(String message) {
        log("<yellow>", message);
    }

    public void error(String message) {
        log("<red>", message);
    }
}
