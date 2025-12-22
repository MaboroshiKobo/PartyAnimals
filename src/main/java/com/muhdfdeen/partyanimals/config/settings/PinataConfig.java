package com.muhdfdeen.partyanimals.config.settings;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.muhdfdeen.partyanimals.config.SerializableLocation;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;

public final class PinataConfig {

    public static PinataConfiguration load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();
        Path pinataFile = new File(dataFolder, "modules/pinata.yml").toPath();
        return YamlConfigurations.update(pinataFile, PinataConfiguration.class, properties);
    }

    @Configuration
    public static class Command {
        @Comment("The chance for this reward to be given.")
        public Double chance = 100.0;

        @Comment("Whether the command is triggered server-wide or per-player.")
        public Boolean serverwide = null;

        @Comment("Whether to skip the rest of the commands if this one is given.")
        public Boolean skipRest = null;

        @Comment("Whether to randomize the command execution.")
        public Boolean randomize = null;

        @Comment("Permission required to receive this reward.")
        public String permission = null;

        public List<String> commands = List.of();

        public Command() {}

        public Command(double chance, List<String> commands) {
            this.chance = chance;
            this.commands = commands;
        }

        public Command(double chance, Boolean serverwide, Boolean skipRest, Boolean randomize, String permission, List<String> commands) {
            this.chance = chance;
            this.serverwide = serverwide;
            this.skipRest = skipRest;
            this.randomize = randomize;
            this.permission = permission;
            this.commands = commands;
        }
    }

    public record ScaleSettings(
            @Comment("Minimum scale of the pinata entity.") double min,
            @Comment("Maximum scale of the pinata entity.") double max
    ) {}

    public record Appearance(
            @Comment({"List of entity types that can be used as pinatas.", "If multiple types are provided, one will be chosen at random.", " ", "Available types:" + " https://jd.papermc.io/paper/1.21.1/org/bukkit/entity/EntityType.html"}) List<String> types,
            @Comment("Name of the pinata entity.") String name,
            @Comment({"Scale settings of the pinata entity.", "Scale is fixed when both min and max are the same value."}) ScaleSettings scale,
            @Comment("Whether the pinata should flash red when hit.") boolean damageFlash,
            @Comment("Whether the pinata should have a glowing outline.") boolean glowing,
            @Comment("The color of the glowing outline.") String glowColor
    ) {}

    public record Bar(
            @Comment("Whether to display the bar.") boolean enabled,
            @Comment("Whether the bar is displayed server-wide.") boolean serverwide,
            @Comment("Color of the bar.") String color,
            @Comment("Overlay style of the bar.") String overlay
    ) {}

    public record Health(
            @Comment("Maximum health of the pinata.") int maxHealth,
            @Comment({"Whether the health is multiplied per player.", "If true, the value for multiplier is ignored."}) boolean perPlayer,
            @Comment("Multiplier for the maximum health of the pinata.") int multiplier,
            @Comment("Settings for the health bar.") Bar bar
    ) {}

    public record ItemWhitelist(
            @Comment("Whether to enable item whitelist for hitting the pinata.") boolean enabled,
            @Comment("List of item names that are allowed to hit the pinata.") List<String> items
    ) {}

    public record Interaction(
            @Comment("Permission required to hit the pinata.") String permission,
            ItemWhitelist whitelist
    ) {}

    public record Countdown(
            @Comment("Duration of the countdown in seconds.") int duration,
            @Comment("Settings for the countdown bar.") Bar bar,
            @Comment("Effects triggered at the start of the countdown before the pinata spawns.") Effects start,
            @Comment("Effects triggered during the countdown before the pinata spawns.") Effects mid,
            @Comment("Effects triggered at the end of the countdown before the pinata spawns.") Effects end
    ) {}

    public record Timeout(
            @Comment("Whether the pinata timeout is enabled.") boolean enabled,
            @Comment("Duration of the pinata timeout in seconds.") int duration
    ) {}

    public record HitCooldown(
            @Comment("Whether the hit cooldown is enabled.") boolean enabled,
            @Comment("Duration of the hit cooldown in seconds.") double duration,
            @Comment("If true, the cooldown is applied server-wide. If false, it's per player.") boolean serverwide,
            @Comment({"The type of display to use for the cooldown.", "Available values: ACTION_BAR, CHAT" }) String type
    ) {}

    public record Timer(
            Countdown countdown,
            Timeout timeout,
            HitCooldown hitCooldown
    ) {}

    public record PathfindingRange(double x, double y, double z) {}

    public record PathfindingSettings(
            @Comment("Range within which the pinata can pathfind.") PathfindingRange range,
            @Comment("Movement speed of the pinata.") double speed
    ) {}

    public record AI(
            @Comment({"Whether the pinata AI is enabled.", "If true, the pinata moves around. Otherwise, it remains stationary."}) boolean enabled,
            @Comment("The knockback resistance of the pinata.") double knockbackResistance,
            @Comment("Pathfinding settings for the pinata.") PathfindingSettings pathfinding
    ) {}

    public record SoundEffect(String type, float volume, float pitch) {}

    public record ParticleEffect(String type, int count) {}

    public record Effects(SoundEffect sound, ParticleEffect particle) {}

    public record Event(
            @Comment("Whether this event is enabled.") boolean enabled,
            @Comment("Effects associated with this event.") Effects effects,
            @Comment("Commands associated with this event.") Map<String, Command> commands
    ) {}

    public record Events(
            @Comment("Manage pinata spawn events.") Event spawn,
            @Comment("Manage pinata hit events.") Event hit,
            @Comment("Manage pinata last hit events.") Event lastHit,
            @Comment("Manage pinata death events.") Event death
    ) {}

    @Configuration
    public static class PinataConfiguration {
        public Appearance appearance = new Appearance(List.of("LLAMA"), "<green><bold>Pinata</bold></green>", new ScaleSettings(0.75, 1.25), false, true, "GREEN");
        
        public Health health = new Health(5, true, 5, new Bar(true, true, "RED", "PROGRESS"));
        
        public Interaction interaction = new Interaction("", new ItemWhitelist(false, List.of("STICK", "WHEAT_SEEDS")));
        
        public Timer timer = new Timer(
                new Countdown(10,
                        new Bar(true, true, "YELLOW", "NOTCHED_10"),
                        new Effects(new SoundEffect("block.note_block.bit", 0.5f, 1.0f), new ParticleEffect("FIREWORK", 20)),
                        new Effects(new SoundEffect("block.note_block.bit", 0.5f, 0.8f), new ParticleEffect("CAMPFIRE_COSY_SMOKE", 15)),
                        new Effects(new SoundEffect("block.note_block.pling", 1.0f, 0.8f), new ParticleEffect("HAPPY_VILLAGER", 25))
                ),
                new Timeout(true, 300),
                new HitCooldown(true, 0.75, false, "ACTION_BAR")
        );
        
        public AI ai = new AI(true, 1.0, new PathfindingSettings(new PathfindingRange(10.0, 5.0, 10.0), 1.75));
        
        @Comment("Locations where pinatas can spawn.")
        public Map<String, SerializableLocation> spawnLocations = new HashMap<>(Map.of("spawn", new SerializableLocation()));
        
        public Events events = new Events(
                new Event(true, 
                        new Effects(new SoundEffect("entity.firework_rocket.launch", 1.0f, 1.0f), new ParticleEffect("FIREWORK", 50)),
                        new HashMap<>(Map.of("First Broadcast", new Command(100.0, List.of("broadcast A pinata spawned!"))))
                ),
                new Event(true, 
                        new Effects(new SoundEffect("entity.player.levelup", 0.5f, 1.5f), new ParticleEffect("CRIT", 10)),
                        new HashMap<>(Map.of("VIP Cake", new Command(50.0, false, false, false, "partyanimals.hit.reward1", List.of("give {player} minecraft:cake"))))
                ),
                new Event(true, 
                        new Effects(new SoundEffect("entity.experience_orb.pickup", 0.5f, 1.5f), new ParticleEffect("HEART", 15)),
                        new HashMap<>()
                ),
                new Event(true, 
                        new Effects(new SoundEffect("entity.firework_rocket.blast", 1.0f, 1.0f), new ParticleEffect("EXPLOSION_EMITTER", 1)),
                        new HashMap<>()
                )
        );
    }
}
