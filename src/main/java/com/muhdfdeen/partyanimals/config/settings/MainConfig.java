package com.muhdfdeen.partyanimals.config.settings;

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

        public record Modules(
                @Comment("Enable or disable the Pinata module.")
                boolean pinata,
                @Comment("Enable or disable the Vote module.")
                boolean vote
        ) {}

        @Configuration
        public static class MainConfiguration {
                @Comment("Enable debug mode for more verbose logging.")
                public boolean debug = false;

                @Comment("Toggle various modules on or off.")
                public Modules modules = new Modules(
                        true,
                        false
                );
        }
}