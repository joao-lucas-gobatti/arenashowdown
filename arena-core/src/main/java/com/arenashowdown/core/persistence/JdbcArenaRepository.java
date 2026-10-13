package com.arenashowdown.core.persistence;

import com.arenashowdown.api.model.Championship;
import com.arenashowdown.api.model.ChampionshipState;
import com.arenashowdown.api.model.RankingEntry;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class JdbcArenaRepository implements ArenaRepository {
    private final Database database;
    public JdbcArenaRepository(Database database) { this.database = database; }

    @Override public void createChampionship(Championship c) throws SQLException {
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement("INSERT INTO championships(id,name,state,created_at) VALUES(?,?,?,?)")) {
            ps.setString(1, c.id()); ps.setString(2, c.name()); ps.setString(3, c.state().name()); ps.setString(4, c.createdAt().toString()); ps.executeUpdate();
        }
    }

    @Override public Optional<Championship> championship(String id) throws SQLException {
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement("SELECT id,name,state,created_at FROM championships WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(readChampionship(rs)) : Optional.empty(); }
        }
    }

    @Override public List<Championship> championships() throws SQLException {
        List<Championship> result = new ArrayList<>();
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement("SELECT id,name,state,created_at FROM championships ORDER BY created_at DESC"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(readChampionship(rs));
        }
        return result;
    }

    @Override public boolean updateState(String id, ChampionshipState expected, ChampionshipState next) throws SQLException {
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement("UPDATE championships SET state=? WHERE id=? AND state=?")) {
            ps.setString(1, next.name()); ps.setString(2, id); ps.setString(3, expected.name()); return ps.executeUpdate() == 1;
        }
    }

    @Override public void registerPlayer(String championshipId, UUID playerId, String playerName, String teamId) throws SQLException {
        String sql = "INSERT INTO participants(championship_id,player_id,player_name,team_id,joined_at) VALUES(?,?,?,?,?) ON CONFLICT(championship_id,player_id) DO UPDATE SET player_name=excluded.player_name,team_id=excluded.team_id";
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, championshipId); ps.setString(2, playerId.toString()); ps.setString(3, playerName); ps.setString(4, teamId); ps.setString(5, Instant.now().toString()); ps.executeUpdate();
        }
    }

    @Override public boolean insertScoreEvent(String eventId, String championshipId, UUID playerId, int points, String source) throws SQLException {
        String sql = "INSERT INTO score_events(event_id,championship_id,player_id,points,source,created_at) VALUES(?,?,?,?,?,?) ON CONFLICT(event_id) DO NOTHING";
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1,eventId); ps.setString(2,championshipId); ps.setString(3,playerId.toString()); ps.setInt(4,points); ps.setString(5,source); ps.setString(6,Instant.now().toString()); return ps.executeUpdate() == 1;
        }
    }

    @Override public List<RankingEntry> ranking(String championshipId, int limit) throws SQLException {
        String sql = "SELECT p.player_id,p.player_name,COALESCE(SUM(s.points),0) total FROM participants p LEFT JOIN score_events s ON s.championship_id=p.championship_id AND s.player_id=p.player_id WHERE p.championship_id=? GROUP BY p.player_id,p.player_name ORDER BY total DESC,p.player_name ASC LIMIT ?";
        List<RankingEntry> result = new ArrayList<>();
        try (Connection con = database.connection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1,championshipId); ps.setInt(2,limit);
            try (ResultSet rs = ps.executeQuery()) { while(rs.next()) result.add(new RankingEntry(UUID.fromString(rs.getString(1)),rs.getString(2),rs.getInt(3))); }
        }
        return result;
    }

    private Championship readChampionship(ResultSet rs) throws SQLException {
        return new Championship(rs.getString("id"), rs.getString("name"), ChampionshipState.valueOf(rs.getString("state")), Instant.parse(rs.getString("created_at")));
    }
}
