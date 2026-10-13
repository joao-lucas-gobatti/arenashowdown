package com.arenashowdown.core.service;

import com.arenashowdown.api.model.*;
import com.arenashowdown.core.persistence.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class ChampionshipManager {
    private final ArenaRepository arena;
    private final OrchestrationRepository orchestration;
    private final Executor executor;
    public ChampionshipManager(ArenaRepository arena,OrchestrationRepository orchestration,Executor executor){this.arena=arena;this.orchestration=orchestration;this.executor=executor;}

    public CompletableFuture<ChampionshipDetails> create(String id,String name,ChampionshipMode mode,List<String> games,TieBreaker tieBreaker,String actor){return query(()->{validateId(id);if(games.isEmpty())throw new IllegalArgumentException("A sequência de minigames não pode ser vazia");var d=new ChampionshipDetails(id.toLowerCase(Locale.ROOT),name,ChampionshipState.WAITING,mode,games,0,tieBreaker,null,Instant.now(),null);orchestration.createChampionship(d,actor);return d;});}
    public CompletableFuture<ChampionshipDetails> details(String id){return query(()->orchestration.championshipDetails(id).orElseThrow(()->new IllegalArgumentException("Campeonato não encontrado: "+id)));}
    public CompletableFuture<Championship> transition(String id,ChampionshipState next,String actor){return query(()->{var current=arena.championship(id).orElseThrow(()->new IllegalArgumentException("Campeonato não encontrado: "+id));if(!current.state().canTransitionTo(next))throw new IllegalStateException("Transição inválida: "+current.state()+" -> "+next);if(!arena.updateState(id,current.state(),next))throw new IllegalStateException("Estado alterado concorrentemente");orchestration.audit(id,actor,"CHAMPIONSHIP_STATE",current.state()+" -> "+next);return new Championship(current.id(),current.name(),next,current.createdAt());});}
    public CompletableFuture<Team> createTeam(String championshipId,String teamId,String name,String actor){return query(()->{var d=required(championshipId);if(d.mode()!=ChampionshipMode.TEAMS)throw new IllegalStateException("Campeonato não é por equipes");if(d.state()!=ChampionshipState.WAITING)throw new IllegalStateException("Equipes só podem ser criadas durante WAITING");validateId(teamId);var team=new Team(championshipId,teamId.toLowerCase(Locale.ROOT),name,Instant.now());orchestration.createTeam(team,actor);return team;});}
    public CompletableFuture<Void> assignTeam(String championshipId,UUID playerId,String teamId,String actor){return query(()->{var d=required(championshipId);if(d.state()!=ChampionshipState.WAITING)throw new IllegalStateException("Atribuições encerradas");orchestration.assignTeam(championshipId,playerId,teamId,actor);return null;});}
    public CompletableFuture<List<TeamRankingEntry>> teamRanking(String id,int limit){return query(()->orchestration.teamRanking(id,Math.max(1,Math.min(100,limit))));}
    public CompletableFuture<Boolean> administrativeScore(String id,UUID playerId,int points,String source,String eventId,String actor){return query(()->{boolean inserted=arena.insertScoreEvent(eventId,id,playerId,points,source);if(inserted)orchestration.audit(id,actor,"ADMIN_SCORE",playerId+" points="+points+" event="+eventId);return inserted;});}
    public CompletableFuture<String> finish(String id,String actor){return query(()->{var d=required(id);if(d.state()!=ChampionshipState.RUNNING&&d.state()!=ChampionshipState.PAUSED)throw new IllegalStateException("Campeonato não está ativo");String champion;if(d.mode()==ChampionshipMode.TEAMS){var ranks=orchestration.teamRanking(id,100);champion=ranks.isEmpty()?null:ranks.getFirst().teamId();}else{var ranks=arena.ranking(id,100);champion=ranks.isEmpty()?null:ranks.getFirst().playerId().toString();}if(!arena.updateState(id,d.state(),ChampionshipState.FINISHED))throw new IllegalStateException("Estado alterado concorrentemente");orchestration.updateChampionshipProgress(id,d.currentRound(),champion,Instant.now());orchestration.audit(id,actor,"CHAMPIONSHIP_FINISHED","champion="+champion);return champion;});}
    private ChampionshipDetails required(String id)throws SQLException{return orchestration.championshipDetails(id).orElseThrow(()->new IllegalArgumentException("Campeonato não encontrado: "+id));}
    private void validateId(String id){if(id==null||!id.matches("[a-zA-Z0-9_-]{2,64}"))throw new IllegalArgumentException("ID inválido");}
    private <T> CompletableFuture<T> query(SqlSupplier<T> op){return CompletableFuture.supplyAsync(()->{try{return op.get();}catch(SQLException e){throw new CompletionException(e);}},executor);}
    @FunctionalInterface private interface SqlSupplier<T>{T get()throws SQLException;}
}
