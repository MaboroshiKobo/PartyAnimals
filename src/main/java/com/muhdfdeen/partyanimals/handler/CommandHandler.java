package com.muhdfdeen.partyanimals.handler;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.muhdfdeen.partyanimals.PartyAnimals;
import com.muhdfdeen.partyanimals.config.settings.PinataConfig;

import me.clip.placeholderapi.PlaceholderAPI;

public class CommandHandler {

    private final PartyAnimals plugin;
    private final boolean hasPAPI;

    public CommandHandler(PartyAnimals plugin) {
        this.plugin = plugin;
        this.hasPAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void process(Player player, Map<String, PinataConfig.Command> commands) {
        if (commands == null || commands.isEmpty())
            return;

        Map<String, PinataConfig.Command> sortedCommands = new TreeMap<>(commands);
        for (PinataConfig.Command command : sortedCommands.values()) {
            if (command.permission != null && !command.permission.isEmpty()) {
                if (player != null && !player.hasPermission(command.permission))
                    continue;
            }

            double roll = ThreadLocalRandom.current().nextDouble(100.0);
            if (roll > command.chance)
                continue;

            if (command.randomize != null && command.randomize && !command.commands.isEmpty()) {
                String randomCmd = command.commands.get(ThreadLocalRandom.current().nextInt(command.commands.size()));
                dispatch(player, randomCmd);
            } else {
                for (String cmd : command.commands) {
                    dispatch(player, cmd);
                }
            }

            if (command.skipRest != null && command.skipRest) {
                break;
            }
        }
    }

    private void dispatch(Player player, String command) {
        if (command == null || command.isEmpty())
            return;

        String parsed = command;

        if (player != null && hasPAPI) {
            parsed = PlaceholderAPI.setPlaceholders(player, parsed);
        }

        if (player != null) {
            parsed = parsed
                    .replace("{player}", player.getName())
                    .replace("{uuid}", player.getUniqueId().toString());
        }

        if (parsed.startsWith("/"))
            parsed = parsed.substring(1);

        final String finalCommand = parsed;

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }
}
