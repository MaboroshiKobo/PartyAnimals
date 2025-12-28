package com.muhdfdeen.partyanimals.config;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.muhdfdeen.partyanimals.config.settings.MainConfig;
import com.muhdfdeen.partyanimals.config.settings.MessageConfig;
import com.muhdfdeen.partyanimals.config.settings.PinataConfig;
import com.muhdfdeen.partyanimals.config.settings.MainConfig.MainConfiguration;
import com.muhdfdeen.partyanimals.config.settings.MessageConfig.MessageConfiguration;
import com.muhdfdeen.partyanimals.config.settings.PinataConfig.PinataConfiguration;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.YamlConfigurations;

public class ConfigManager {
    private final File dataFolder;

    private MainConfiguration mainConfig;
    private Map<String, PinataConfiguration> pinataConfigs;
    private MessageConfiguration messageConfig;

    public ConfigManager(File dataFolder) {
        this.dataFolder = dataFolder;
        this.pinataConfigs = new HashMap<>();
    }

    public void loadConfig() {
        this.mainConfig = MainConfig.load(dataFolder);
        loadPinataConfigs();
    }

    private void loadPinataConfigs() {
        pinataConfigs.clear();

        File pinataFolder = new File(dataFolder, "pinatas");
        if (!pinataFolder.exists()) {
            pinataFolder.mkdirs();
        }

        File defaultPinata = new File(pinataFolder, "default.yml");
        if (!defaultPinata.exists()) {
            PinataConfig.load(defaultPinata);
        }

        File[] files = pinataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.contains(" ")) {
                    System.err.println("Pinata config file names cannot contain spaces: " + fileName);
                    continue;
                }
                String id = fileName.substring(0, fileName.lastIndexOf('.'));
                PinataConfiguration config = PinataConfig.load(file);
                pinataConfigs.put(id, config);
            }
        }
    }

    public void loadMessages() {
        this.messageConfig = MessageConfig.load(dataFolder);
    }

    public void saveConfig() {
        Path settingsPath = new File(dataFolder, "config.yml").toPath();
        YamlConfigurations.save(
                settingsPath,
                MainConfiguration.class,
                mainConfig,
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES);

        for (Map.Entry<String, PinataConfiguration> entry : pinataConfigs.entrySet()) {
            Path path = new File(dataFolder, "pinatas/" + entry.getKey() + ".yml").toPath();
            YamlConfigurations.save(
                path,
                PinataConfiguration.class,
                entry.getValue(),
                ConfigLib.BUKKIT_DEFAULT_PROPERTIES
            );
        }
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

    public PinataConfiguration getPinataConfig(String id) {
        return pinataConfigs.get(id);
    }

    public Map<String, PinataConfiguration> getPinataConfigs() {
        return pinataConfigs;
    }

    public MessageConfiguration getMessageConfig() {
        return messageConfig;
    }
}
