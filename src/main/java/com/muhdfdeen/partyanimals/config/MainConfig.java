package com.muhdfdeen.partyanimals.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.nio.file.Path;

public final class MainConfig {

        public static MainConfiguration load(File dataFolder) {
                YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
                Path configFile = new File(dataFolder, "config.yml").toPath();
                return YamlConfigurations.update(configFile, MainConfiguration.class, properties);
        }

        @Configuration
        public static class MainConfiguration {
                @Comment("Enable debug mode for more verbose logging.")
                public boolean debug = false;
        }
}