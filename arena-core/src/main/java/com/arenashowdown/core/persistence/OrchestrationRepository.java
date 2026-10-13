package com.arenashowdown.core.persistence;

import com.arenashowdown.api.model.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public interface OrchestrationRepository {
    void createChampionship(ChampionshipDetails details, String actor) throws SQLException;
    Optional<ChampionshipDetails> championshipDetails(String id) throws SQLException;
    void updateChampionshipProgress(String id, int round, String championId, Instant completedAt) throws SQLException;
    void createTeam(Team team, String actor) throws SQLException;
    List<Team> teams(String championshipId) throws SQLException;
    void assignTeam(String championshipId, UUID playerId, String teamId, String actor) throws SQLException;
    void createMatch(ArenaMatch match, String actor) throws SQLException;
    Optional<ArenaMatch> match(String id) throws SQLException;
    List<ArenaMatch> matches(String championshipId) throws SQLException;
    Optional<ArenaMatch> activeMatch(UUID playerId) throws SQLException;
    boolean updateMatchState(String id, MatchState expected, MatchState next, Instant startedAt, String reason) throws SQLException;
    void updateParticipantStatus(String matchId, UUID playerId, ParticipantStatus status, Instant disconnectedAt) throws SQLException;
    Optional<ParticipantStatus> participantStatus(String matchId, UUID playerId) throws SQLException;
    List<ParticipantInfo> participants(String championshipId) throws SQLException;
    boolean finalizeMatch(MatchResult result, String actor) throws SQLException;
    List<TeamRankingEntry> teamRanking(String championshipId, int limit) throws SQLException;
    void audit(String championshipId, String actor, String action, String details) throws SQLException;
}
