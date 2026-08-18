package org.maboroshi.partyanimals.manager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.maboroshi.partyanimals.PartyAnimals;
import org.maboroshi.partyanimals.config.settings.MainConfig.DatabaseSettings;
import org.maboroshi.partyanimals.config.settings.MainConfig.PoolSettings;
import org.maboroshi.partyanimals.util.Log;

public class DatabaseManager {
    private final PartyAnimals plugin;
    private HikariDataSource dataSource;
    private boolean sqlite;

    private String votesTable;
    private String rewardsTable;
    private String serverDataTable;

    public DatabaseManager(PartyAnimals plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        DatabaseSettings settings = plugin.getConfiguration().getMainConfig().database;
        PoolSettings pool = settings.pool;

        this.votesTable = settings.tablePrefix + "votes";
        this.rewardsTable = settings.tablePrefix + "offline_rewards";
        this.serverDataTable = settings.tablePrefix + "server_data";

        HikariConfig config = new HikariConfig();
        config.setPoolName("PartyAnimals-Pool");
        config.setConnectionTimeout(pool.connectionTimeout);
        config.setMaximumPoolSize(pool.maximumPoolSize);
        config.setLeakDetectionThreshold(pool.leakDetectionThreshold);

        String type = settings.type.toLowerCase();
        this.sqlite = !type.equals("mariadb") && !type.equals("mysql");

        if (type.equals("mariadb")) {
            config.setDriverClassName("org.mariadb.jdbc.Driver");
            config.setJdbcUrl("jdbc:mariadb://" + settings.host + ":" + settings.port + "/" + settings.database);
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            Log.info("Connecting to MariaDB database...");

        } else if (type.equals("mysql")) {
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl("jdbc:mysql://"
                    + settings.host
                    + ":"
                    + settings.port
                    + "/"
                    + settings.database
                    + "?useSSL=false&autoReconnect=true");
            config.setUsername(settings.username);
            config.setPassword(settings.password);
            Log.info("Connecting to MySQL database...");

        } else {
            config.setDriverClassName("org.sqlite.JDBC");
            config.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath());
            Log.info("Connecting to SQLite database...");
        }

        try {
            this.dataSource = new HikariDataSource(config);
            initializeTables();
            Log.info("Successfully connected to the database.");
        } catch (Exception e) {
            Log.error("Failed to connect to database! Please check your config.yml.");
            Log.error("Error: " + e.getMessage());
        }
    }

    public void initializeTables() {
        String autoInc = isSQLite() ? "AUTOINCREMENT" : "AUTO_INCREMENT";

        String createVotesTable = "CREATE TABLE IF NOT EXISTS "
                + votesTable
                + " ("
                + "id INTEGER PRIMARY KEY "
                + autoInc
                + ", "
                + "uuid VARCHAR(36) NOT NULL, "
                + "username VARCHAR(16) NOT NULL, "
                + "amount INTEGER NOT NULL DEFAULT 1, "
                + "service VARCHAR(64) NOT NULL, "
                + "timestamp BIGINT NOT NULL"
                + ");";

        String createRewardsTable = "CREATE TABLE IF NOT EXISTS "
                + rewardsTable
                + " ("
                + "id INTEGER PRIMARY KEY "
                + autoInc
                + ", "
                + "uuid VARCHAR(36) NOT NULL, "
                + "command TEXT NOT NULL"
                + ");";

        String createServerDataTable = "CREATE TABLE IF NOT EXISTS "
                + serverDataTable
                + " ("
                + "setting_key VARCHAR(64) PRIMARY KEY, "
                + "value TEXT"
                + ");";

        String indexName = votesTable + "_uuid_idx";
        String createIndex = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + votesTable + "(uuid);";

        try (Connection connection = getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(createVotesTable);
            statement.execute(createRewardsTable);
            statement.execute(createServerDataTable);
            statement.execute(createIndex);

            migrateDatabase(connection);

            Log.info(
                    "Database tables initialized (" + votesTable + ", " + rewardsTable + ", " + serverDataTable + ").");
        } catch (SQLException e) {
            Log.error("Failed to create database tables: " + e.getMessage());
        }
    }

    private void migrateDatabase(Connection connection) {
        if (isSQLite()) return;

        try (Statement statement = connection.createStatement()) {
            String alterSql = "ALTER TABLE " + votesTable + " MODIFY COLUMN timestamp BIGINT NOT NULL;";
            statement.execute(alterSql);
        } catch (SQLException e) {
            Log.debug("Migration check skipped: " + e.getMessage());
        }
    }

    public void addVote(UUID uuid, String username, String service, int amount) {
        String sql =
                "INSERT INTO " + votesTable + " (uuid, username, service, amount, timestamp) VALUES (?, ?, ?, ?, ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, username);
            statement.setString(3, service);
            statement.setInt(4, amount);
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
            Log.debug("Saved vote for " + username);
        } catch (SQLException e) {
            Log.error("Failed to save vote: " + e.getMessage());
        }
    }

    public enum VoteResult {
        SUCCESS_REWARD,
        SUCCESS_NO_REWARD,
        FAIL_IGNORED
    }

    public VoteResult processVote(
            UUID uuid, String username, String service, int amount, int limit, boolean countExcess) {
        Connection connection = null;
        try {
            connection = getConnection();

            if (!isSQLite()) {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }
            connection.setAutoCommit(false);

            long startOfDay = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();

            int votesToday = 0;
            String countSql = "SELECT SUM(amount) FROM " + votesTable + " WHERE uuid = ? AND timestamp >= ?;";
            try (PreparedStatement countStmt = connection.prepareStatement(countSql)) {
                countStmt.setString(1, uuid.toString());
                countStmt.setLong(2, startOfDay);
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        votesToday = rs.getInt(1);
                    }
                }
            }

            boolean underLimit = (limit <= 0) || (votesToday < limit);
            boolean shouldInsert = underLimit || countExcess;

            VoteResult result;

            if (shouldInsert) {
                String insertSql = "INSERT INTO " + votesTable
                        + " (uuid, username, service, amount, timestamp) VALUES (?, ?, ?, ?, ?);";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setString(2, username);
                    insertStmt.setString(3, service);
                    insertStmt.setInt(4, amount);
                    insertStmt.setLong(5, System.currentTimeMillis());
                    insertStmt.executeUpdate();
                }

                result = underLimit ? VoteResult.SUCCESS_REWARD : VoteResult.SUCCESS_NO_REWARD;
            } else {
                result = VoteResult.FAIL_IGNORED;
            }

            connection.commit();
            return result;

        } catch (SQLException e) {
            Log.error("Failed to process atomic vote: " + e.getMessage());
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ex) {
                    Log.warn("Rollback failed after vote transaction error: " + ex.getMessage());
                }
            }
            return VoteResult.FAIL_IGNORED;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException e) {
                    Log.warn("Failed to reset close SQL connection: " + e.getMessage());
                }
            }
        }
    }

    public int getVotes(UUID uuid) {
        if (uuid == null) return 0;
        String sql = "SELECT SUM(amount) FROM " + votesTable + " WHERE uuid = ?;";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            Log.error("Failed to get votes: " + e.getMessage());
        }
        return 0;
    }

    public UUID getPlayerUUID(String playerName) {
        String sql = "SELECT uuid FROM " + votesTable + " WHERE LOWER(username) = LOWER(?) LIMIT 1;";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerName);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            Log.error("Database fallback lookup failed for: " + playerName);
        }

        Player onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) return onlinePlayer.getUniqueId();

        try {
            var profile = Bukkit.createProfile(playerName);
            if (profile.getId() != null) return profile.getId();
        } catch (Exception ignored) {
        }

        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }

    public void queueRewards(UUID uuid, String command) {
        String sql = "INSERT INTO " + rewardsTable + " (uuid, command) VALUES (?, ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, command);
            statement.executeUpdate();
            Log.info("Queued reward for " + uuid);
        } catch (SQLException e) {
            Log.error("Failed to queue reward: " + e.getMessage());
        }
    }

    public List<String> retrieveRewards(UUID uuid) {
        List<String> commands = new ArrayList<>();
        String selectSql = "SELECT command FROM " + rewardsTable + " WHERE uuid = ?;";
        String deleteSql = "DELETE FROM " + rewardsTable + " WHERE uuid = ?;";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement selectStatement = connection.prepareStatement(selectSql)) {
                    selectStatement.setString(1, uuid.toString());
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        while (resultSet.next()) {
                            commands.add(resultSet.getString("command"));
                        }
                    }
                }

                if (!commands.isEmpty()) {
                    try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql)) {
                        deleteStatement.setString(1, uuid.toString());
                        deleteStatement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            Log.error("Failed to retrieve rewards: " + e.getMessage());
        }
        return commands;
    }

    public int getCommunityGoalProgress() {
        String sql = "SELECT value FROM " + serverDataTable + " WHERE setting_key = 'community_vote_count';";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
        } catch (SQLException e) {
            Log.error("Failed to get community goal progress: " + e.getMessage());
        }
        return 0;
    }

    public int incrementCommunityGoalProgress() {
        String updateSql =
                "UPDATE " + serverDataTable + " SET value = value + 1 WHERE setting_key = 'community_vote_count';";
        String selectSql = "SELECT value FROM " + serverDataTable + " WHERE setting_key = 'community_vote_count';";

        try (Connection connection = getConnection()) {
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                int rowsAffected = updateStmt.executeUpdate();

                if (rowsAffected == 0) {
                    setCommunityGoalProgress(1);
                    return 1;
                }
            }

            try (PreparedStatement selectStmt = connection.prepareStatement(selectSql);
                    ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    return Integer.parseInt(rs.getString("value"));
                }
            }
        } catch (SQLException e) {
            Log.error("Failed to atomic increment community goal: " + e.getMessage());
        }
        return 0;
    }

    private void setCommunityGoalProgress(int value) {
        String sql = "REPLACE INTO " + serverDataTable + " (setting_key, value) VALUES ('community_vote_count', ?);";
        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, String.valueOf(value));
            statement.executeUpdate();
        } catch (SQLException e) {
            Log.error("Failed to update community goal: " + e.getMessage());
        }
    }

    public int getVotesSince(UUID uuid, long timestamp) {
        if (uuid == null) return 0;

        String sql = "SELECT SUM(amount) FROM " + votesTable + " WHERE uuid = ? AND timestamp >= ?;";

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setLong(2, timestamp);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            Log.error("Failed to get votes since timestamp: " + e.getMessage());
        }
        return 0;
    }

    public int getVotesBetween(UUID uuid, long start, long end) {
        if (uuid == null) return 0;

        String sql = "SELECT SUM(amount) FROM " + votesTable + " WHERE uuid = ? AND timestamp >= ? AND timestamp < ?;";

        try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, uuid.toString());
            statement.setLong(2, start);
            statement.setLong(3, end);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e) {
            Log.error("Failed to get votes between timestamps: " + e.getMessage());
        }
        return 0;
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Log.info("Database connection pool closed.");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private boolean isSQLite() {
        return this.sqlite;
    }
}
