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
        try (Connection con = database.connection()) { con.setAutoCommit(false); try {
            try(PreparedStatement ps=con.prepareStatement("INSERT INTO championships(id,name,state,created_at) VALUES(?,?,?,?)")){ps.setString(1,c.id());ps.setString(2,c.name());ps.setString(3,c.state().name());ps.setString(4,c.createdAt().toString());ps.executeUpdate();}
            try(PreparedStatement ps=con.prepareStatement("INSERT INTO championship_settings(championship_id) VALUES(?)")){ps.setString(1,c.id());ps.executeUpdate();}
            con.commit();
        } catch(SQLException e){con.rollback();throw e;} finally{con.setAutoCommit(true);}
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
        try (Connection con = database.connection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1,eventId); ps.setString(2,championshipId); ps.setString(3,playerId.toString()); ps.setInt(4,points); ps.setString(5,source); ps.setString(6,Instant.now().toString());
                boolean inserted = ps.executeUpdate() == 1;
                con.commit();
                return inserted;
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException rollbackFailure) { e.addSuppressed(rollbackFailure); }
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    @Override public List<RankingEntry> ranking(String championshipId, int limit) throws SQLException {
        String tieBreaker = "MOST_WINS";
        try (Connection con=database.connection();PreparedStatement ps=con.prepareStatement("SELECT tie_breaker FROM championship_settings WHERE championship_id=?")){ps.setString(1,championshipId);try(ResultSet rs=ps.executeQuery()){if(rs.next())tieBreaker=rs.getString(1);}}
        String tieOrder = switch (tieBreaker) {
            case "BEST_LAST_ROUND" -> "COALESCE((SELECT re.position FROM result_entries re JOIN match_results mr ON mr.id=re.result_id JOIN matches m ON m.id=mr.match_id WHERE m.championship_id=p.championship_id AND re.player_id=p.player_id ORDER BY m.round_number DESC LIMIT 1),999999) ASC";
            case "EARLIEST_REGISTRATION" -> "p.joined_at ASC";
            case "NAME" -> "p.player_name ASC";
            default -> "COALESCE((SELECT COUNT(*) FROM result_entries re JOIN match_results mr ON mr.id=re.result_id JOIN matches m ON m.id=mr.match_id WHERE m.championship_id=p.championship_id AND re.player_id=p.player_id AND re.winner=1),0) DESC";
        };
        String sql = "SELECT p.player_id,p.player_name,COALESCE(SUM(s.points),0) total FROM participants p LEFT JOIN score_events s ON s.championship_id=p.championship_id AND s.player_id=p.player_id WHERE p.championship_id=? GROUP BY p.player_id,p.player_name,p.joined_at ORDER BY total DESC,"+tieOrder+",p.player_name ASC LIMIT ?";
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
