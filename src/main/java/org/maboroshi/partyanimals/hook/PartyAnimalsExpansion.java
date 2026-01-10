package org.maboroshi.partyanimals.hook;

import java.util.Calendar;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.manager.PinataManager;

public class PartyAnimalsExpansion extends PlaceholderExpansion {
    private final PartyAnimals plugin;

    public PartyAnimalsExpansion(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "partyanimals";
    }

    @Override
    public String getAuthor() {
        return plugin.getPluginMeta().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        PinataManager pinataManager = plugin.getPinataManager();

        if (pinataManager != null && params.startsWith("pinata_")) {
            if (params.equals("pinata_count")) {
                return String.valueOf(pinataManager.getActivePinataCount());
            }
            if (params.equals("pinata_any_alive")) {
                return String.valueOf(pinataManager.isPinataAlive());
            }

            if (params.startsWith("pinata_nearest_")) {
                if (player == null) return "";

                LivingEntity pinata = pinataManager.getNearestPinata(player.getLocation());
                String subParam = params.substring("pinata_nearest_".length());

                if (pinata == null) {
                    return switch (subParam) {
                        case "health", "max_health" -> "0";
                        case "alive" -> "false";
                        case "location" -> "N/A";
                        default -> null;
                    };
                }

                return switch (subParam) {
                    case "alive" -> "true";
                    case "health" -> String.valueOf(pinataManager.getPinataHealth(pinata));
                    case "max_health" -> String.valueOf(pinataManager.getPinataMaxHealth(pinata));
                    case "location" -> {
                        Location loc = pinata.getLocation();
                        yield loc.getWorld().getName()
                                + ", "
                                + loc.getBlockX()
                                + ", "
                                + loc.getBlockY()
                                + ", "
                                + loc.getBlockZ();
                    }
                    default -> null;
                };
            }
        }

        if (player != null) {
            if (params.equals("votes")) {
                UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(player.getName());
                return String.valueOf(plugin.getDatabaseManager().getVotes(targetUUID));
            }

            if (params.startsWith("votes_")) {
                String fullParam = params.substring("votes_".length()).toLowerCase();

                boolean isPrevious = fullParam.startsWith("previous_");
                String period = isPrevious ? fullParam.substring("previous_".length()) : fullParam;

                UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(player.getName());

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                switch (period) {
                    case "daily" -> {}
                    case "weekly" -> cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                    case "monthly" -> cal.set(Calendar.DAY_OF_MONTH, 1);
                    case "yearly" -> cal.set(Calendar.DAY_OF_YEAR, 1);
                    default -> {
                        return null;
                    }
                }

                long currentPeriodStart = cal.getTimeInMillis();

                if (isPrevious) {
                    switch (period) {
                        case "daily" -> cal.add(Calendar.DAY_OF_MONTH, -1);
                        case "weekly" -> cal.add(Calendar.WEEK_OF_YEAR, -1);
                        case "monthly" -> cal.add(Calendar.MONTH, -1);
                        case "yearly" -> cal.add(Calendar.YEAR, -1);
                    }
                    long previousPeriodStart = cal.getTimeInMillis();

                    return String.valueOf(plugin.getDatabaseManager()
                            .getVotesBetween(targetUUID, previousPeriodStart, currentPeriodStart));
                } else {
                    return String.valueOf(plugin.getDatabaseManager().getVotesSince(targetUUID, currentPeriodStart));
                }
            }
        }

        if (params.startsWith("community_goal_")) {
            var goalConfig = plugin.getConfiguration().getMainConfig().modules.vote.communityGoal;

            if (!goalConfig.enabled) {
                return "Disabled";
            }

            int rawTotal = plugin.getDatabaseManager().getCommunityGoalProgress();
            int required = goalConfig.votesRequired;

            int visualProgress = (required > 0) ? rawTotal % required : 0;

            if (visualProgress == 0 && rawTotal > 0) {
                visualProgress = required;
            }

            return switch (params) {
                case "community_goal_current" -> String.valueOf(visualProgress);
                case "community_goal_required" -> String.valueOf(required);
                case "community_goal_percentage" -> {
                    if (required == 0) yield "0%";
                    int percent = (int) ((visualProgress / (double) required) * 100);
                    yield percent + "%";
                }

                case "community_goal_total" -> String.valueOf(rawTotal);

                case "community_goal_remaining" -> {
                    int remaining = required - visualProgress;
                    if (remaining == 0) remaining = required;
                    yield String.valueOf(remaining);
                }

                case "community_goal_met_count" -> {
                    yield String.valueOf(required > 0 ? rawTotal / required : 0);
                }

                default -> null;
            };
        }

        return null;
    }
}
