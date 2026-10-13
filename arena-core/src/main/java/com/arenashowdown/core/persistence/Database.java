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
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS championship_settings (championship_id VARCHAR(64) PRIMARY KEY, mode VARCHAR(16) NOT NULL DEFAULT 'INDIVIDUAL', minigame_sequence TEXT NOT NULL DEFAULT '', current_round INTEGER NOT NULL DEFAULT 0, tie_breaker VARCHAR(32) NOT NULL DEFAULT 'MOST_WINS', champion_id VARCHAR(64), completed_at VARCHAR(40), FOREIGN KEY(championship_id) REFERENCES championships(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS teams (championship_id VARCHAR(64) NOT NULL, id VARCHAR(64) NOT NULL, name VARCHAR(128) NOT NULL, created_at VARCHAR(40) NOT NULL, PRIMARY KEY(championship_id,id), FOREIGN KEY(championship_id) REFERENCES championships(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS matches (id VARCHAR(64) PRIMARY KEY, championship_id VARCHAR(64) NOT NULL, round_number INTEGER NOT NULL, minigame VARCHAR(64) NOT NULL, arena VARCHAR(128) NOT NULL, state VARCHAR(20) NOT NULL, reconnect_allowed INTEGER NOT NULL, scheduled_at VARCHAR(40), started_at VARCHAR(40), duration_ms INTEGER, result_id VARCHAR(128), cancellation_reason TEXT, FOREIGN KEY(championship_id) REFERENCES championships(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS match_participants (match_id VARCHAR(64) NOT NULL, player_id VARCHAR(36) NOT NULL, team_id VARCHAR(64), status VARCHAR(20) NOT NULL, disconnected_at VARCHAR(40), PRIMARY KEY(match_id,player_id), FOREIGN KEY(match_id) REFERENCES matches(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS match_results (id VARCHAR(128) PRIMARY KEY, match_id VARCHAR(64) NOT NULL UNIQUE, is_tie INTEGER NOT NULL, recorded_at VARCHAR(40) NOT NULL, FOREIGN KEY(match_id) REFERENCES matches(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS result_entries (result_id VARCHAR(128) NOT NULL, player_id VARCHAR(36) NOT NULL, team_id VARCHAR(64), position INTEGER NOT NULL, points INTEGER NOT NULL, winner INTEGER NOT NULL, PRIMARY KEY(result_id,player_id), FOREIGN KEY(result_id) REFERENCES match_results(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS team_score_events (event_id VARCHAR(128) PRIMARY KEY, championship_id VARCHAR(64) NOT NULL, team_id VARCHAR(64) NOT NULL, points INTEGER NOT NULL, source VARCHAR(64) NOT NULL, created_at VARCHAR(40) NOT NULL, FOREIGN KEY(championship_id,team_id) REFERENCES teams(championship_id,id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, championship_id VARCHAR(64), actor VARCHAR(64) NOT NULL, action VARCHAR(64) NOT NULL, details TEXT NOT NULL, created_at VARCHAR(40) NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS championship_history (championship_id VARCHAR(64) PRIMARY KEY, champion_id VARCHAR(64), mode VARCHAR(16) NOT NULL, rounds INTEGER NOT NULL, completed_at VARCHAR(40) NOT NULL, FOREIGN KEY(championship_id) REFERENCES championships(id))");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_matches_championship ON matches(championship_id,round_number)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_team_score_ranking ON team_score_events(championship_id,team_id)");
            statement.executeUpdate("INSERT OR IGNORE INTO championship_settings(championship_id) SELECT id FROM championships");
        }
    }

    @Override public void close() { dataSource.close(); }
}
