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
                String period = params.substring("votes_".length()).toLowerCase();
                UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(player.getName());
                long startTimestamp = 0L;

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                switch (period) {
                    case "daily":
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "weekly":
                        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "monthly":
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "yearly":
                        cal.set(Calendar.DAY_OF_YEAR, 1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    default:
                        return null;
                }
                return String.valueOf(plugin.getDatabaseManager().getVotesSince(targetUUID, startTimestamp));
            }

            if (params.startsWith("votes_previous_")) {
                String period = params.substring("votes_previous_".length()).toLowerCase();
                UUID targetUUID = plugin.getDatabaseManager().getPlayerUUID(player.getName());

                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);

                long endTimestamp = 0L;
                long startTimestamp = 0L;

                switch (period) {
                    case "daily":
                        endTimestamp = cal.getTimeInMillis();
                        cal.add(Calendar.DAY_OF_MONTH, -1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "weekly":
                        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                        endTimestamp = cal.getTimeInMillis();
                        cal.add(Calendar.WEEK_OF_YEAR, -1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "monthly":
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        endTimestamp = cal.getTimeInMillis();
                        cal.add(Calendar.MONTH, -1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    case "yearly":
                        cal.set(Calendar.DAY_OF_YEAR, 1);
                        endTimestamp = cal.getTimeInMillis();
                        cal.add(Calendar.YEAR, -1);
                        startTimestamp = cal.getTimeInMillis();
                        break;
                    default:
                        return null;
                }

                return String.valueOf(
                        plugin.getDatabaseManager().getVotesBetween(targetUUID, startTimestamp, endTimestamp));
            }
        }

        if (params.startsWith("vote_community_")) {
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
                case "vote_community_current" -> String.valueOf(visualProgress);
                case "vote_community_required" -> String.valueOf(required);
                case "vote_community_percentage" -> {
                    if (required == 0) yield "0%";
                    int percent = (int) ((visualProgress / (double) required) * 100);
                    yield percent + "%";
                }

                case "vote_community_total" -> String.valueOf(rawTotal);

                case "vote_community_remaining" -> {
                    int remaining = required - visualProgress;
                    if (remaining == 0) remaining = required;
                    yield String.valueOf(remaining);
                }

                case "vote_community_goals_met" -> {
                    yield String.valueOf(required > 0 ? rawTotal / required : 0);
                }

                default -> null;
            };
        }

        return null;
    }
}
