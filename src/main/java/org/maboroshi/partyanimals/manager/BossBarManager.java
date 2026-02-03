package org.maboroshi.partyanimals.manager;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.PinataConfig.PinataConfiguration;
import org.maboroshi.partyanimals.util.MessageUtils;
import org.maboroshi.partyanimals.util.NamespacedKeys;

public class BossBarManager {
    private final MessageUtils messageUtils;

    private final Map<UUID, BossBar> pinataBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> bossBarTasks = new ConcurrentHashMap<>();

    private final Map<UUID, BossBar> countdownBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Location> countdownLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> countdownGlobalSettings = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> countdownRainbowSettings = new ConcurrentHashMap<>();

    public BossBarManager(PartyAnimals plugin) {
        this.messageUtils = plugin.getMessageUtils();
    }

    public void startTracking(LivingEntity pinata, PinataConfiguration config) {
        if (bossBarTasks.containsKey(pinata.getUniqueId())) return;

        int currentHealth = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, config.health.baseHealth);
        int maxHealth = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, currentHealth);
        int timeout = config.timer.timeout.duration;

        createPinataBossBar(pinata, currentHealth, maxHealth, timeout, config);

        ScheduledTask task = pinata.getScheduler()
                .runAtFixedRate(
                        PartyAnimals.getPlugin(),
                        (t) -> {
                            if (!pinata.isValid()) {
                                if (pinata.isDead()) removePinataBossBar(pinata.getUniqueId());
                                t.cancel();
                                bossBarTasks.remove(pinata.getUniqueId());
                                return;
                            }

                            if (!hasPinataBossBar(pinata.getUniqueId())) {
                                t.cancel();
                                bossBarTasks.remove(pinata.getUniqueId());
                                return;
                            }

                            updatePinataBossBar(pinata, config);
                        },
                        () -> {},
                        20L,
                        20L);

        bossBarTasks.put(pinata.getUniqueId(), task);
    }

    public UUID createCountdownBossBar(Location location, PinataConfiguration pinataConfig, int totalSeconds) {
        UUID uuid = UUID.randomUUID();
        var barSettings = pinataConfig.timer.countdown.bar;

        if (!barSettings.enabled) {
            return uuid;
        }

        boolean isRainbow = barSettings.color.equalsIgnoreCase("RAINBOW");

        BossBar bossBar = BossBar.bossBar(
                messageUtils.parse(null, barSettings.text, messageUtils.tag("countdown", totalSeconds)),
                1.0f,
                isRainbow ? BossBar.Color.PINK : BossBar.Color.valueOf(barSettings.color),
                barSettings.overlay);

        countdownBossBars.put(uuid, bossBar);
        countdownLocations.put(uuid, location);
        countdownGlobalSettings.put(uuid, barSettings.global);
        countdownRainbowSettings.put(uuid, isRainbow);

        updateViewerList(bossBar, barSettings.global, location.getWorld());

        return uuid;
    }

    public void updateCountdownBar(
            UUID uuid, int remainingSeconds, int totalSeconds, PinataConfiguration pinataConfig, int tickCounter) {
        BossBar bossBar = countdownBossBars.get(uuid);
        if (bossBar == null) return;
        var countdownBarSettings = pinataConfig.timer.countdown.bar;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) remainingSeconds / totalSeconds));
        bossBar.progress(progress);

        bossBar.name(
                messageUtils.parse(null, countdownBarSettings.text, messageUtils.tag("countdown", remainingSeconds)));

        if (countdownRainbowSettings.getOrDefault(uuid, false) && tickCounter % 5 == 0) {
            int next = (bossBar.color().ordinal() + 1) % BossBar.Color.values().length;
            bossBar.color(BossBar.Color.values()[next]);
        }

        Location location = countdownLocations.get(uuid);
        if (location != null) {
            updateViewerList(bossBar, countdownGlobalSettings.get(uuid), location.getWorld());
        }
    }

    public void removeCountdownBossBar(UUID uuid) {
        BossBar bar = countdownBossBars.remove(uuid);
        countdownLocations.remove(uuid);
        countdownGlobalSettings.remove(uuid);
        countdownRainbowSettings.remove(uuid);

        hideBar(bar);
    }

    public void createPinataBossBar(
            LivingEntity pinata, int health, int maxHealth, int timeout, PinataConfiguration pinataConfig) {
        if (!pinataConfig.health.bar.enabled) return;

        String rawMsg = pinataConfig.health.bar.text;
        Component barName = messageUtils.parse(null, rawMsg, messageUtils.getPinataTags(pinata));

        BossBar bossBar = BossBar.bossBar(
                barName, 1.0f, BossBar.Color.valueOf(pinataConfig.health.bar.color), pinataConfig.health.bar.overlay);

        pinataBossBars.put(pinata.getUniqueId(), bossBar);
        updateViewerList(bossBar, pinataConfig.health.bar.global, pinata.getWorld());
    }

    private void updatePinataBossBar(LivingEntity pinata, PinataConfiguration config) {
        int currentHealth = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_HEALTH, PersistentDataType.INTEGER, 0);
        int maxHealth = pinata.getPersistentDataContainer()
                .getOrDefault(NamespacedKeys.PINATA_MAX_HEALTH, PersistentDataType.INTEGER, 1);

        updatePinataBossBar(pinata, currentHealth, maxHealth, NamespacedKeys.PINATA_SPAWN_TIME, config);
    }

    public void updatePinataBossBar(
            LivingEntity pinata,
            int currentHealth,
            int maxHealth,
            NamespacedKey spawnTimeKey,
            PinataConfiguration pinataConfig) {

        BossBar bossBar = pinataBossBars.get(pinata.getUniqueId());

        if (bossBar == null || !pinataConfig.health.bar.enabled) return;

        float progress = Math.max(0.0f, Math.min(1.0f, (float) currentHealth / maxHealth));
        bossBar.progress(progress);

        String timeStr = "∞";
        if (pinataConfig.timer.timeout.enabled) {
            long spawnTime = pinata.getPersistentDataContainer()
                    .getOrDefault(spawnTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
            int totalTimeout = pinataConfig.timer.timeout.duration;
            int remaining = Math.max(0, totalTimeout - (int) ((System.currentTimeMillis() - spawnTime) / 1000));
            timeStr = formatTime(remaining, pinataConfig);
        }

        bossBar.name(messageUtils.parse(
                null,
                pinataConfig.health.bar.text,
                messageUtils.getPinataTags(pinata),
                messageUtils.tag("timer", timeStr)));

        updateViewerList(bossBar, pinataConfig.health.bar.global, pinata.getWorld());
    }

    public boolean hasPinataBossBar(UUID uuid) {
        return pinataBossBars.containsKey(uuid);
    }

    public void removePinataBossBar(UUID uuid) {
        BossBar bar = pinataBossBars.remove(uuid);
        hideBar(bar);

        ScheduledTask task = bossBarTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public Map<UUID, BossBar> getPinataBossBars() {
        return pinataBossBars;
    }

    public void removeAll() {
        pinataBossBars.values().forEach(this::hideBar);
        pinataBossBars.clear();

        bossBarTasks.values().forEach(ScheduledTask::cancel);
        bossBarTasks.clear();

        countdownBossBars.values().forEach(this::hideBar);
        countdownBossBars.clear();
        countdownLocations.clear();
        countdownGlobalSettings.clear();
        countdownRainbowSettings.clear();
    }

    public void handleJoin(Player player) {
        pinataBossBars.forEach((uuid, bar) -> {
            LivingEntity livingEntity = (LivingEntity) Bukkit.getEntity(uuid);
            if (livingEntity != null) {
                if (player.getWorld().equals(livingEntity.getWorld())) {
                    player.showBossBar(bar);
                }
            }
        });

        countdownBossBars.forEach((uuid, bar) -> {
            Location location = countdownLocations.get(uuid);
            boolean global = countdownGlobalSettings.getOrDefault(uuid, true);

            if (global || (location != null && location.getWorld().equals(player.getWorld()))) {
                player.showBossBar(bar);
            }
        });
    }

    private void updateViewerList(BossBar bar, boolean global, World world) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean inSameWorld = p.getWorld().equals(world);
            if (global || inSameWorld) p.showBossBar(bar);
            else p.hideBossBar(bar);
        }
    }

    private void hideBar(BossBar bar) {
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }
    }

    private String formatTime(int seconds, PinataConfiguration pinataConfig) {
        if (!pinataConfig.timer.timeout.enabled || seconds <= 0) return "∞";
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
