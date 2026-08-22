package org.maboroshi.partyanimals.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.MessageConfig;
import org.maboroshi.partyanimals.hook.VotifierHook;
import org.maboroshi.partyanimals.hook.migration.PinataPartyMigration;
import org.maboroshi.partyanimals.util.MessageUtils;

public class VoteCommand {
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageUtils messageUtils;

    public VoteCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageUtils = plugin.getMessageUtils();
    }

    @Command("partyanimals vote check <player>")
    @Command("pa vote check <player>")
    @Permission("partyanimals.vote")
    public void onCheck(
            CommandSourceStack source, @Argument(value = "player", suggestions = "players") String targetName) {
        CommandSender sender = source.getSender();
        MessageConfig msgConfig = config.getMessageConfig();

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            UUID uuid = plugin.getDatabaseManager().getPlayerUUID(targetName);
            int votes = plugin.getDatabaseManager().getVotes(uuid);
            messageUtils.send(
                    sender,
                    msgConfig.vote.check,
                    messageUtils.tag("player", targetName),
                    messageUtils.tag("votes", String.valueOf(votes)));
        });
    }

    @Command("partyanimals vote add <player> <amount>")
    @Command("pa vote add <player> <amount>")
    @Permission("partyanimals.vote.add")
    public void onAdd(
            CommandSourceStack source,
            @Argument(value = "player", suggestions = "players") String targetName,
            @Argument("amount") int amount) {
        handleModify(source.getSender(), targetName, amount, "add");
    }

    @Command("partyanimals vote remove <player> <amount>")
    @Command("pa vote remove <player> <amount>")
    @Permission("partyanimals.vote.remove")
    public void onRemove(
            CommandSourceStack source,
            @Argument(value = "player", suggestions = "players") String targetName,
            @Argument("amount") int amount) {
        handleModify(source.getSender(), targetName, amount, "remove");
    }

    @Command("partyanimals vote set <player> <amount>")
    @Command("pa vote set <player> <amount>")
    @Permission("partyanimals.vote.set")
    public void onSet(
            CommandSourceStack source,
            @Argument(value = "player", suggestions = "players") String targetName,
            @Argument("amount") int amount) {
        handleModify(source.getSender(), targetName, amount, "set");
    }

    @Command("partyanimals vote migrate PinataParty")
    @Command("pa vote migrate PinataParty")
    @Permission("partyanimals.vote.migrate")
    public void onMigrate(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        MessageConfig msgConfig = config.getMessageConfig();

        messageUtils.send(sender, msgConfig.vote.migrateStart);
        new PinataPartyMigration(plugin).migrate();
        messageUtils.send(sender, msgConfig.vote.migrateFinish);
    }

    @Command("partyanimals vote send <player> [service]")
    @Command("pa vote send <player> [service]")
    @Permission("partyanimals.vote.send")
    public void onSend(
            CommandSourceStack source,
            @Argument(value = "player", suggestions = "players") String targetName,
            @Argument("service") String[] serviceArgs) {
        MessageConfig msgConfig = config.getMessageConfig();

        if (!Bukkit.getPluginManager().isPluginEnabled("Votifier")) {
            messageUtils.send(source.getSender(), msgConfig.vote.votifierDisabled);
            return;
        }

        String serviceName =
                (serviceArgs != null && serviceArgs.length > 0) ? String.join(" ", serviceArgs) : "FakeService";

        VotifierHook.sendVote(source.getSender(), targetName, serviceName, messageUtils);
    }

    private void handleModify(CommandSender sender, String targetName, int amount, String action) {
        MessageConfig msgConfig = config.getMessageConfig();

        Bukkit.getAsyncScheduler().runNow(plugin, task -> {
            UUID uuid = plugin.getDatabaseManager().getPlayerUUID(targetName);
            int currentVotes = plugin.getDatabaseManager().getVotes(uuid);
            int finalChange = 0;

            if (action.equals("add")) {
                finalChange = amount;
            } else if (action.equals("remove")) {
                finalChange = -amount;
            } else if (action.equals("set")) {
                finalChange = amount - currentVotes;
            }

            if (finalChange == 0) {
                messageUtils.send(
                        sender,
                        msgConfig.vote.noChange,
                        messageUtils.tag("player", targetName),
                        messageUtils.tag("votes", String.valueOf(currentVotes)));
                return;
            }

            plugin.getDatabaseManager().addVote(uuid, targetName, "Admin", finalChange);
            int newTotal = currentVotes + finalChange;

            messageUtils.send(sender, msgConfig.vote.updateSuccess, messageUtils.tag("player", targetName));
            messageUtils.send(
                    sender,
                    msgConfig.vote.updateDetail,
                    messageUtils.tag("old_votes", String.valueOf(currentVotes)),
                    messageUtils.tag("new_votes", String.valueOf(newTotal)));
        });
    }
}
