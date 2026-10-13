package com.arenashowdown.core.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database implements AutoCloseable {
    private final HikariDataSource dataSource;
    private final Path file;

    private Database(Path file, int busyTimeoutMs) throws SQLException {
        this.file = file.toAbsolutePath().normalize();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + this.file + "?foreign_keys=on&journal_mode=WAL&synchronous=NORMAL&busy_timeout=" + busyTimeoutMs);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        config.setPoolName("ArenaCore-DB");
        config.setConnectionTimeout(5_000);
        dataSource = new HikariDataSource(config);
        migrate();
    }

    public static Database sqlite(Path file) throws SQLException {
        return sqlite(file, 5_000);
    }

    public static Database sqlite(Path file, int busyTimeoutMs) throws SQLException { return new Database(file, busyTimeoutMs); }

    public Connection connection() throws SQLException { return dataSource.getConnection(); }
    public Path file() { return file; }

    private void migrate() throws SQLException {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS championships (id VARCHAR(64) PRIMARY KEY, name VARCHAR(128) NOT NULL, state VARCHAR(20) NOT NULL, created_at VARCHAR(40) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS participants (championship_id VARCHAR(64) NOT NULL, player_id VARCHAR(36) NOT NULL, player_name VARCHAR(16) NOT NULL, team_id VARCHAR(64), joined_at VARCHAR(40) NOT NULL, PRIMARY KEY(championship_id, player_id), FOREIGN KEY(championship_id) REFERENCES championships(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS score_events (event_id VARCHAR(128) PRIMARY KEY, championship_id VARCHAR(64) NOT NULL, player_id VARCHAR(36) NOT NULL, points INTEGER NOT NULL, source VARCHAR(64) NOT NULL, created_at VARCHAR(40) NOT NULL, FOREIGN KEY(championship_id, player_id) REFERENCES participants(championship_id, player_id))");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_score_ranking ON score_events(championship_id, player_id)");
        }
    }

    @Override public void close() { dataSource.close(); }
}
