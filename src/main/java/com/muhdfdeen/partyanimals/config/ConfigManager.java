package com.muhdfdeen.partyanimals.config;

import java.io.File;
import java.nio.file.Path;

import com.muhdfdeen.partyanimals.config.MainConfig.MainConfiguration;
import com.muhdfdeen.partyanimals.config.MessageConfig.MessageConfiguration;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurations;

public class ConfigManager {
    private final File dataFolder;

    private MainConfiguration mainConfig;
    private MessageConfiguration messageConfig;

    public ConfigManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void loadConfig() {
        this.mainConfig = MainConfig.load(dataFolder);
    }

    public void loadMessages() {
        this.messageConfig = MessageConfig.load(dataFolder);
    }

    public void saveConfig() {
        Path path = new File(dataFolder, "config.yml").toPath();
        YamlConfigurations.save(
                path,
                MainConfiguration.class,
                mainConfig,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES);
    }

    public void saveMessages() {
        Path path = new File(dataFolder, "messages.yml").toPath();
        YamlConfigurations.save(
                path,
                MessageConfiguration.class,
                messageConfig,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES);
    }

    public MainConfiguration getMainConfig() {
        return mainConfig;
    }

    public MessageConfiguration getMessageConfig() {
        return messageConfig;
    }
}