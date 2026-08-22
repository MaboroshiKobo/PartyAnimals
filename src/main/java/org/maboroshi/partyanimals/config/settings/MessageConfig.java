package org.maboroshi.partyanimals.config.settings;

import de.exlll.configlib.Comment;
import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import java.io.File;
import java.nio.file.Path;

@Configuration
public class MessageConfig {

    public static MessageConfig load(File dataFolder) {
        YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
                .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
                .build();
        Path messagesFile = new File(dataFolder, "messages.yml").toPath();
        return YamlConfigurations.update(messagesFile, MessageConfig.class, properties);
    }

    @Comment("The global prefix used across messages. Use <prefix> in other messages to include it.")
    public String prefix =
            "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals</bold></gradient> <dark_gray>➟</dark_gray>";

    @Comment("General plugin notifications.")
    public GeneralMessages general = new GeneralMessages();

    @Comment("Core plugin commands and administrative execution feedback.")
    public CommandMessages commands = new CommandMessages();

    @Comment("Help menu headers, layout formats, and command descriptions.")
    public HelpMessages help = new HelpMessages();

    @Comment("Messages and announcements for the pinata module.")
    public PinataMessages pinata = new PinataMessages();

    @Comment("Messages for the vote tracking, modification, and migration systems.")
    public VoteMessages vote = new VoteMessages();

    @Configuration
    public static class GeneralMessages {
        @Comment("Broadcasted when an update is available for PartyAnimals.")
        public String updateAvailable =
                "<prefix> <gray>New version available: <green><latest_version></green> (Current: <red><current_version></red>)</gray>";
    }

    @Configuration
    public static class CommandMessages {
        @Comment("Feedback sent when a player attempts an action without proper permissions.")
        public String noPermission = "<prefix> <red>You do not have permission for this.</red>";

        @Comment("Message sent when a non-player executes a player-only command.")
        public String playerOnly = "<prefix> <red>This command is for players only.</red>";

        @Comment("Syntax error feedback indicating how to properly use the command.")
        public String usageHelp = "<prefix> <red>Usage: <gray><usage-help></gray></red>";

        @Comment("Feedback sent when the targeted player could not be located in the database or server.")
        public String playerNotFound = "<prefix> <red>Player <dark_red><player></dark_red> not found.</red>";

        @Comment("Information display for the about/version command.")
        public String about =
                "<prefix> Running version <green><version></green> developed by <green><authors></green>.";

        @Comment("Message displayed to confirm successful configuration reload.")
        public String reloadSuccess = "<prefix> <green>Configuration reloaded successfully.</green>";

        @Comment("Feedback sent when a configuration reload encounters parsing errors.")
        public String reloadFail = "<prefix> <red>Reload failed! Check console for errors.</red>";
    }

    @Configuration
    public static class HelpMessages {
        @Comment("The header shown at the top of the command help directory.")
        public String header = "<gradient:#51CF66:#2f9e44>🪅 <bold>Party Animals</bold> Commands</gradient>";

        @Comment("Format for each individual help line entry.")
        public String entry =
                "<gray>-</gray> <click:suggest_command:\"/<command>\"><white>/<command></white> <dark_gray>»</dark_gray> <gray><description></gray></click>";

        @Comment("Description for the reload command.")
        public String reload = "Reload plugin configuration";

        @Comment("Description for starting a pinata countdown.")
        public String pinataStart = "Start pinata countdown";

        @Comment("Description for spawning a pinata immediately.")
        public String pinataSpawn = "Spawn pinata immediately";

        @Comment("Description for removing all active pinatas.")
        public String pinataKillall = "Remove all active pinatas";

        @Comment("Description for adding a new spawn point.")
        public String spawnPointAdd = "Save current location as spawn point";

        @Comment("Description for removing an existing spawn point.")
        public String spawnPointRemove = "Remove a saved spawn point";

        @Comment("Description for checking player votes.")
        public String voteCheck = "Check player vote count";

        @Comment("Description for modifying vote balances.")
        public String voteModify = "Modify player vote data";

        @Comment("Description for simulating incoming votes.")
        public String voteSend = "Simulate a vote transaction";

        @Comment("Description for migrating third-party vote data.")
        public String voteMigrate = "Migrate data from another plugin";
    }

    @Configuration
    public static class PinataMessages {
        @Comment("Broadcasts and public announcements.")
        public PinataEvents events = new PinataEvents();

        @Comment("Feedback messages sent to individual interacting players.")
        public PinataGameplay gameplay = new PinataGameplay();

        @Comment("Administrative command responses for pinata management.")
        public PinataAdmin admin = new PinataAdmin();
    }

    @Configuration
    public static class PinataEvents {
        @Comment("Broadcast when the spawn countdown begins.")
        public String starting =
                "<prefix> <gray>Started countdown for a pinata to spawn at <white><location></white>.</gray>";

        @Comment("Broadcast when an event pinata is spawned via command or event.")
        public String spawned =
                "<prefix> <gray>A pinata has <yellow>spawned</yellow> at <white><location></white>!</gray>";

        @Comment("Broadcast when a pinata spawns naturally.")
        public String spawnedNaturally = "<prefix> <gray>A pinata has spawned at <white><location></white>!</gray>";

        @Comment("Broadcast when a pinata is fully defeated.")
        public String defeated = "<prefix> <gray>The pinata has been <green>defeated</green>!</gray>";

        @Comment("Broadcast when a pinata times out and disappears.")
        public String timeout = "<prefix> <gray>The pinata has <red>escaped</red>!</gray>";
    }

    @Configuration
    public static class PinataGameplay {
        @Comment("Notification sent when a valid hit registers on the pinata.")
        public String hitSuccess = "<prefix> <gray>You landed a hit on the pinata!</gray>";

        @Comment("Warning sent when trying to hit the pinata with an unauthorized weapon.")
        public String hitWrongItem = "<prefix> <gray>You must use <red><item></red> to hit this pinata!</gray>";

        @Comment("Warning sent when attacking during hit cooldown.")
        public String hitCooldown = "<prefix> <red><bold>Too fast!</bold></red> <gray>Please wait a moment.</gray>";

        @Comment("Warning sent when a player lacks permission to hit the pinata.")
        public String hitNoPermission = "<prefix> <gray>You are <red>not allowed</red> to hit this pinata.</gray>";

        @Comment("Broadcast highlighting the player who delivered the final blow.")
        public String lastHit = "<prefix> <gray><white><player></white> dealt the final blow!</gray>";
    }

    @Configuration
    public static class PinataAdmin {
        @Comment("Error when referencing a nonexistent pinata template.")
        public String unknownTemplate = "<prefix> <red>Unknown pinata template: <white><template></white></red>";

        @Comment("Confirmation when a new spawn point is saved.")
        public String spawnPointAdded =
                "<prefix> <gray><white><location></white> has been <green>added</green> as a spawn point.</gray>";

        @Comment("Confirmation when a spawn point is removed.")
        public String spawnPointRemoved =
                "<prefix> <gray><white><location></white> has been <red>removed</red> as a spawn point.</gray>";

        @Comment("Error when referencing an unknown spawn point name.")
        public String spawnPointUnknown =
                "<prefix> <gray>The spawn point <white><location></white> does not exist.</gray>";
    }

    @Configuration
    public static class VoteMessages {
        @Comment("Response when querying a player's total vote balance.")
        public String check = "<prefix> <white><player></white> has <aqua><votes></aqua> votes.";

        @Comment("Confirmation header sent after modifying a player's vote count.")
        public String updateSuccess = "<prefix> <green>Updated votes for <white><player></white>.</green>";

        @Comment("Detailed tally breakdown sent after modifying a player's vote count.")
        public String updateDetail =
                "<prefix> <gray>Old: <yellow><old_votes></yellow> -> New: <aqua><new_votes></aqua></gray>";

        @Comment("Notification sent when a vote modification results in no net change.")
        public String noChange = "<prefix> <yellow>No changes made. <player> already has <votes> votes.</yellow>";

        @Comment("Error displayed when attempting to simulate votes without Votifier loaded.")
        public String votifierDisabled = "<prefix> <red>Votifier is not enabled on this server.</red>";

        @Comment("Notice when triggering a simulated vote.")
        public String voteTriggered =
                "<prefix> <gray>Triggered vote event for <white><player></white> via <white><service></white></gray>";

        @Comment("Notice when starting migration from PinataParty.")
        public String migrateStart = "<prefix> <yellow>Starting PinataParty migration...</yellow>";

        @Comment("Notice when the migration task concludes.")
        public String migrateFinish =
                "<prefix> <yellow>Migration process finished. Check console for results.</yellow>";
    }
}
