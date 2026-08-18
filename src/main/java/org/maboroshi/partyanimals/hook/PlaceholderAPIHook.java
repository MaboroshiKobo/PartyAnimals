package org.maboroshi.partyanimals.hook;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.manager.PinataManager;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final PartyAnimals plugin;

    public PlaceholderAPIHook(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "partyanimals";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
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
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        PinataManager pinataManager = plugin.getPinataManager();

        if (pinataManager != null && params.startsWith("pinata_")) {
            if (params.equals("pinata_count")) {
                return String.valueOf(pinataManager.getActivePinataCount());
            }
            if (params.equals("pinata_any_alive")) {
                return String.valueOf(pinataManager.isPinataAlive());
            }

            if (params.startsWith("pinata_nearest_")) {
                if (!(offlinePlayer instanceof Player player)) return "";

                LivingEntity pinata = pinataManager.getNearestPinata(player.getLocation());
                String subParam = params.substring("pinata_nearest_".length());

                if (pinata == null) {
                    return switch (subParam) {
                        case "health", "max_health" -> "0";
                        case "alive" -> "false";
                        case "location", "name" -> "N/A";
                        default -> null;
                    };
                }

                return switch (subParam) {
                    case "alive" -> "true";
                    case "health" -> String.valueOf(pinataManager.getPinataHealth(pinata));
                    case "max_health" -> String.valueOf(pinataManager.getPinataMaxHealth(pinata));
                    case "name" ->
                        pinata.getPersistentDataContainer()
                                .getOrDefault(NamespacedKeys.PINATA_NAME, PersistentDataType.STRING, "Unknown");
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

        if (offlinePlayer != null) {
            if (params.equals("votes")) {
                return String.valueOf(plugin.getDatabaseManager().getVotes(offlinePlayer.getUniqueId()));
            }

            if (params.startsWith("votes_")) {
                String fullParam = params.substring("votes_".length()).toLowerCase();
                boolean isPrevious = fullParam.startsWith("previous_");
                String period = isPrevious ? fullParam.substring("previous_".length()) : fullParam;

                ZonedDateTime now = LocalDate.now().atStartOfDay(ZoneId.systemDefault());

                ZonedDateTime currentPeriodStart =
                        switch (period) {
                            case "daily" -> now;
                            case "weekly" -> now.with(DayOfWeek.MONDAY);
                            case "monthly" -> now.withDayOfMonth(1);
                            case "yearly" -> now.withDayOfYear(1);
                            default -> null;
                        };

                if (currentPeriodStart == null) {
                    return null;
                }

                long startMillis = currentPeriodStart.toInstant().toEpochMilli();

                if (isPrevious) {
                    ZonedDateTime previousPeriodStart =
                            switch (period) {
                                case "daily" -> currentPeriodStart.minusDays(1);
                                case "weekly" -> currentPeriodStart.minusWeeks(1);
                                case "monthly" -> currentPeriodStart.minusMonths(1);
                                case "yearly" -> currentPeriodStart.minusYears(1);
                                default -> currentPeriodStart;
                            };

                    long prevMillis = previousPeriodStart.toInstant().toEpochMilli();
                    return String.valueOf(plugin.getDatabaseManager()
                            .getVotesBetween(offlinePlayer.getUniqueId(), prevMillis, startMillis));
                } else {
                    return String.valueOf(
                            plugin.getDatabaseManager().getVotesSince(offlinePlayer.getUniqueId(), startMillis));
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
                case "community_goal_met_count" -> String.valueOf(required > 0 ? rawTotal / required : 0);
                default -> null;
            };
        }

        return null;
    }
}
