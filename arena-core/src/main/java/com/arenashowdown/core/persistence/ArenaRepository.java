package com.arenashowdown.core.persistence;

import com.arenashowdown.api.model.Championship;
import com.arenashowdown.api.model.ChampionshipState;
import com.arenashowdown.api.model.RankingEntry;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArenaRepository {
    void createChampionship(Championship championship) throws SQLException;
    Optional<Championship> championship(String id) throws SQLException;
    List<Championship> championships() throws SQLException;
    boolean updateState(String id, ChampionshipState expected, ChampionshipState next) throws SQLException;
    void registerPlayer(String championshipId, UUID playerId, String playerName, String teamId) throws SQLException;
    boolean insertScoreEvent(String eventId, String championshipId, UUID playerId, int points, String source) throws SQLException;
    List<RankingEntry> ranking(String championshipId, int limit) throws SQLException;
}
