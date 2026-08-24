package org.maboroshi.partyanimals.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.objects.SerializableLocation;
import org.maboroshi.partyanimals.util.MessageUtils;

public class PinataCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageUtils messageUtils;

    public PinataCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageUtils = plugin.getMessageUtils();
    }

    @Suggestions("pinataTemplates")
    public List<String> suggestPinataTemplates(CommandContext<CommandSourceStack> context, String input) {
        return new ArrayList<>(config.getPinataConfigs().keySet());
    }

    @Suggestions("spawnPoints")
    public List<String> suggestSpawnPoints(CommandContext<CommandSourceStack> context, String input) {
        if (config.getMainConfig().modules.pinata.spawnPoints != null) {
            return new ArrayList<>(
                    config.getMainConfig().modules.pinata.spawnPoints.keySet());
        }
        return List.of();
    }

    @Command("partyanimals pinata start [template] [location] [passenger]")
    @Command("pa pinata start [template] [location] [passenger]")
    @Permission("partyanimals.pinata.start")
    public void onStart(
            CommandSourceStack source,
            @Argument(value = "template", suggestions = "pinataTemplates") String templateId,
            @Argument(value = "location", suggestions = "spawnPoints") String locationName,
            @Argument(value = "passenger", suggestions = "players") String passengerProfile) {

        String selectedTemplate = (templateId != null && !templateId.isEmpty()) ? templateId : "default";
        Location location = resolveLocation(source, locationName, selectedTemplate, "start");
        if (location == null) return;

        plugin.getPinataManager().startCountdown(location, selectedTemplate, passengerProfile);

        messageUtils.send(
                source.getSender(),
                config.getMessageConfig().pinata.events.starting,
                messageUtils.tagParsed(
                        "location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    @Command("partyanimals pinata spawn [template] [location] [passenger]")
    @Command("pa pinata spawn [template] [location] [passenger]")
    @Permission("partyanimals.pinata.spawn")
    public void onSpawn(
            CommandSourceStack source,
            @Argument(value = "template", suggestions = "pinataTemplates") String templateId,
            @Argument(value = "location", suggestions = "spawnPoints") String locationName,
            @Argument(value = "passenger", suggestions = "players") String passengerProfile) {

        String selectedTemplate = (templateId != null && !templateId.isEmpty()) ? templateId : "default";
        Location location = resolveLocation(source, locationName, selectedTemplate, "spawn");
        if (location == null) return;

        plugin.getPinataManager().spawnPinata(location, selectedTemplate, passengerProfile);

        messageUtils.send(
                source.getSender(),
                config.getMessageConfig().pinata.events.spawned,
                messageUtils.tagParsed(
                        "location", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ()));
    }

    @Command("partyanimals pinata killall")
    @Command("pa pinata killall")
    @Permission("partyanimals.pinata.killall")
    public void onKillAll(CommandSourceStack source) {
        plugin.getPinataManager().cleanup();
        messageUtils.send(source.getSender(), "<prefix> <green>Killed all active pinatas.");
    }

    @Command("partyanimals pinata spawnpoint add <name>")
    @Command("pa pinata spawnpoint add <name>")
    @Permission("partyanimals.pinata.spawnpoint.add")
    public void onAddSpawnPoint(CommandSourceStack source, @Argument("name") String locationName) {
        if (!(source.getSender() instanceof Player player)) {
            messageUtils.send(source.getSender(), config.getMessageConfig().commands.playerOnly);
            return;
        }

        SerializableLocation spawnLocation = new SerializableLocation(player.getLocation());
        config.getMainConfig().modules.pinata.spawnPoints.put(locationName, spawnLocation);
        config.saveConfig();

        messageUtils.send(
                player,
                config.getMessageConfig().pinata.admin.spawnPointAdded,
                messageUtils.tag("location", locationName));
    }

    @Command("partyanimals pinata spawnpoint remove <name>")
    @Command("pa pinata spawnpoint remove <name>")
    @Permission("partyanimals.pinata.spawnpoint.remove")
    public void onRemoveSpawnPoint(
            CommandSourceStack source, @Argument(value = "name", suggestions = "spawnPoints") String locationName) {

        SerializableLocation removed =
                config.getMainConfig().modules.pinata.spawnPoints.remove(locationName);

        if (removed != null) {
            config.saveConfig();
            messageUtils.send(
                    source.getSender(),
                    config.getMessageConfig().pinata.admin.spawnPointRemoved,
                    messageUtils.tag("location", locationName));
        } else {
            messageUtils.send(
                    source.getSender(),
                    config.getMessageConfig().pinata.admin.spawnPointUnknown,
                    messageUtils.tag("location", locationName));
        }
    }

    private Location resolveLocation(
            CommandSourceStack source, String locationName, String templateId, String commandContext) {
        if (locationName != null && !locationName.isEmpty()) {
            var pinataConfig = config.getPinataConfig(templateId);
            if (pinataConfig == null) {
                messageUtils.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.admin.unknownTemplate,
                        messageUtils.tagParsed("template", templateId));
                return null;
            }
            SerializableLocation spawnLocation =
                    config.getMainConfig().modules.pinata.spawnPoints.get(locationName);
            if (spawnLocation == null) {
                messageUtils.send(
                        source.getSender(),
                        config.getMessageConfig().pinata.admin.spawnPointUnknown,
                        messageUtils.tag("location", locationName));
                return null;
            }
            return spawnLocation.toBukkit();
        }
        if (source.getSender() instanceof Player player) {
            return player.getLocation();
        }
        sendUsage(source.getSender(), "/partyanimals pinata " + commandContext + " [template] [location]");
        return null;
    }

    private void sendUsage(CommandSender sender, String usage) {
        messageUtils.send(sender, config.getMessageConfig().commands.usageHelp, messageUtils.tag("usage-help", usage));
    }
}
