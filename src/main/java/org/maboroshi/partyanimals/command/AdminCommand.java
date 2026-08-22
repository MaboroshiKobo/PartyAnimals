package org.maboroshi.partyanimals.command;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.ConfigManager;
import org.maboroshi.partyanimals.config.settings.MessageConfig;
import org.maboroshi.partyanimals.util.Log;
import org.maboroshi.partyanimals.util.MessageUtils;

public class AdminCommand {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final PartyAnimals plugin;
    private final ConfigManager config;
    private final MessageUtils messageUtils;

    public AdminCommand(PartyAnimals plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.messageUtils = plugin.getMessageUtils();
    }

    @Command("partyanimals")
    @Command("partyanimals about")
    @Command("partyanimals version")
    @Command("pa")
    @Command("pa about")
    @Command("pa version")
    public void onAbout(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        MessageConfig msgConfig = config.getMessageConfig();

        String version = plugin.getPluginMeta().getVersion();
        String authors = String.join(", ", plugin.getPluginMeta().getAuthors());

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.parsed("prefix", msgConfig.prefix),
                Placeholder.parsed("version", version),
                Placeholder.parsed("authors", authors));

        sender.sendMessage(MINI_MESSAGE.deserialize(msgConfig.commands.about, placeholders));
    }

    @Command("partyanimals reload")
    @Command("pa reload")
    @Permission("partyanimals.reload")
    public void onReload(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        MessageConfig msgConfig = config.getMessageConfig();

        if (plugin.reload()) {
            Log.info("Configuration reloaded by " + sender.getName());
            messageUtils.send(sender, msgConfig.commands.reloadSuccess);
        } else {
            Log.warn("Failed to reload configuration by " + sender.getName());
            messageUtils.send(sender, msgConfig.commands.reloadFail);
        }
    }

    @Command("partyanimals help")
    @Command("pa help")
    public void onHelp(CommandSourceStack source) {
        CommandSender sender = source.getSender();
        MessageConfig msgConfig = config.getMessageConfig();

        messageUtils.send(sender, msgConfig.help.header);

        sendHelpLine(sender, "pa reload", msgConfig.help.reload, "partyanimals.reload");
        sendHelpLine(
                sender,
                "pa pinata start [template] [location]",
                msgConfig.help.pinataStart,
                "partyanimals.pinata.start");
        sendHelpLine(
                sender,
                "pa pinata spawn [template] [location]",
                msgConfig.help.pinataSpawn,
                "partyanimals.pinata.spawn");
        sendHelpLine(sender, "pa pinata killall", msgConfig.help.pinataKillall, "partyanimals.pinata.killall");
        sendHelpLine(
                sender,
                "pa pinata spawnpoint add <name>",
                msgConfig.help.spawnPointAdd,
                "partyanimals.pinata.spawnpoint.add");
        sendHelpLine(
                sender,
                "pa pinata spawnpoint remove <name>",
                msgConfig.help.spawnPointRemove,
                "partyanimals.pinata.spawnpoint.remove");

        sendHelpLine(sender, "pa vote check <player>", msgConfig.help.voteCheck, "partyanimals.vote");
        sendHelpLine(sender, "pa vote <add|remove|set>", msgConfig.help.voteModify, "partyanimals.vote.add");
        sendHelpLine(sender, "pa vote send <player> [service]", msgConfig.help.voteSend, "partyanimals.vote.send");
        sendHelpLine(sender, "pa vote migrate <plugin>", msgConfig.help.voteMigrate, "partyanimals.vote.migrate");
    }

    private void sendHelpLine(CommandSender sender, String command, String description, String permission) {
        if (permission != null && !sender.hasPermission(permission)) {
            return;
        }
        String format = config.getMessageConfig().help.entry;
        messageUtils.send(
                sender, format, messageUtils.tag("command", command), messageUtils.tag("description", description));
    }
}
