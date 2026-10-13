package com.arenashowdown.core.service;

import com.arenashowdown.api.minigame.*;
import com.arenashowdown.api.model.*;
import com.arenashowdown.core.persistence.OrchestrationRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class MatchManager {
    private final OrchestrationRepository repository;
    private final Executor executor;
    private final GameDispatcher dispatcher;
    private final ScorePolicy scorePolicy;
    private final Map<String,MinigameAdapter> adapters=new ConcurrentHashMap<>();
    public MatchManager(OrchestrationRepository repository,Executor executor){this(repository,executor,Runnable::run,(game,position,submitted)->submitted);}
    public MatchManager(OrchestrationRepository repository,Executor executor,GameDispatcher dispatcher,ScorePolicy scorePolicy){this.repository=repository;this.executor=executor;this.dispatcher=dispatcher;this.scorePolicy=scorePolicy;}
    public void registerMinigame(MinigameAdapter adapter){if(adapters.putIfAbsent(adapter.id().toLowerCase(Locale.ROOT),adapter)!=null)throw new IllegalStateException("Minigame já registrado: "+adapter.id());}
    public void unregisterMinigame(String id){adapters.remove(id.toLowerCase(Locale.ROOT));}
    public Optional<MinigameAdapter> minigame(String id){return Optional.ofNullable(adapters.get(id.toLowerCase(Locale.ROOT)));}

    public CompletableFuture<ArenaMatch> create(String id,String championshipId,int round,String minigame,String arena,List<UUID> players,boolean reconnectAllowed,Instant scheduledAt,String actor){return query(()->{if(players.isEmpty())throw new IllegalArgumentException("Partida sem participantes");var championship=repository.championshipDetails(championshipId).orElseThrow(()->new IllegalArgumentException("Campeonato não encontrado"));if(championship.state()!=ChampionshipState.RUNNING)throw new IllegalStateException("Campeonato não está em execução");if(round<1||championship.minigames().isEmpty()||!championship.minigames().get((round-1)%championship.minigames().size()).equalsIgnoreCase(minigame))throw new IllegalArgumentException("Minigame fora da sequência da rodada");var match=new ArenaMatch(id,championshipId,round,minigame,arena,MatchState.WAITING,players,List.of(),reconnectAllowed,scheduledAt,null,null,null);repository.createMatch(match,actor);return repository.match(id).orElseThrow();});}
    public CompletableFuture<ArenaMatch> transition(String id,MatchState next,String actor,String reason){return query(()->{var current=required(id);if(!current.state().canTransitionTo(next))throw new IllegalStateException("Transição de partida inválida: "+current.state()+" -> "+next);Instant start=next==MatchState.RUNNING?Instant.now():null;if(!repository.updateMatchState(id,current.state(),next,start,reason))throw new IllegalStateException("Estado da partida alterado concorrentemente");repository.audit(current.championshipId(),actor,"MATCH_STATE",id+": "+current.state()+" -> "+next);return required(id);}).thenApply(updated->{minigame(updated.minigame()).ifPresent(adapter->dispatcher.dispatch(()->{if(next==MatchState.PREPARING)adapter.prepare(new MinigameContext(updated));else if(next==MatchState.RUNNING)adapter.start();else if(next==MatchState.CANCELLED){adapter.cancel(reason);adapter.reset();}}));return updated;});}
    public CompletableFuture<Boolean> finish(String id,MatchResult result,String actor){return query(()->{var match=required(id);if(match.state()!=MatchState.RUNNING)throw new IllegalStateException("Partida não está em execução");if(!result.matchId().equals(id))throw new IllegalArgumentException("Resultado pertence a outra partida");Set<UUID> expected=new HashSet<>(match.participants());for(ResultEntry entry:result.entries())if(!expected.contains(entry.playerId()))throw new IllegalArgumentException("Resultado contém participante externo");int best=result.entries().stream().mapToInt(ResultEntry::position).min().orElse(Integer.MAX_VALUE);List<ResultEntry>scored=result.entries().stream().map(e->new ResultEntry(e.playerId(),e.teamId(),e.position(),scorePolicy.points(match.minigame(),e.position(),e.points()),result.tie()?e.position()==best:e.winner())).toList();MatchResult official=new MatchResult(result.id(),result.matchId(),scored,result.tie(),result.recordedAt());return repository.finalizeMatch(official,actor);}).thenApply(inserted->{if(inserted)repositoryAdapterFinish(id,result);return inserted;});}
    public CompletableFuture<ArenaMatch> restartRound(String oldMatchId,String newMatchId,String actor){return query(()->{var old=required(oldMatchId);if(old.state()!=MatchState.FINISHED&&old.state()!=MatchState.CANCELLED)throw new IllegalStateException("Finalize ou cancele a partida antes de reiniciar");var replacement=new ArenaMatch(newMatchId,old.championshipId(),old.round(),old.minigame(),old.arena(),MatchState.WAITING,old.participants(),old.teams(),old.reconnectAllowed(),Instant.now(),null,null,null);repository.createMatch(replacement,actor);return required(newMatchId);});}
    public CompletableFuture<ArenaMatch> match(String id){return query(()->required(id));}
    public CompletableFuture<List<ArenaMatch>> calendar(String championshipId){return query(()->repository.matches(championshipId));}
    public CompletableFuture<Optional<ArenaMatch>> activeMatch(UUID playerId){return query(()->repository.activeMatch(playerId));}
    public CompletableFuture<Void> disconnected(String matchId,UUID playerId){return query(()->{var m=required(matchId);if(m.state()==MatchState.RUNNING)repository.updateParticipantStatus(matchId,playerId,m.reconnectAllowed()?ParticipantStatus.DISCONNECTED:ParticipantStatus.ABANDONED,Instant.now());return null;});}
    public CompletableFuture<Boolean> reconnected(String matchId,UUID playerId){return query(()->{var m=required(matchId);if(m.state()!=MatchState.RUNNING||!m.reconnectAllowed())return false;repository.updateParticipantStatus(matchId,playerId,ParticipantStatus.CONNECTED,null);return true;});}
    public CompletableFuture<Optional<ParticipantStatus>> participantStatus(String matchId,UUID playerId){return query(()->repository.participantStatus(matchId,playerId));}
    public CompletableFuture<Void> abandon(String matchId,UUID playerId,String actor){return query(()->{var m=required(matchId);repository.updateParticipantStatus(matchId,playerId,ParticipantStatus.ABANDONED,Instant.now());repository.audit(m.championshipId(),actor,"PLAYER_ABANDONED",matchId+":"+playerId);return null;});}
    private ArenaMatch required(String id)throws SQLException{return repository.match(id).orElseThrow(()->new IllegalArgumentException("Partida não encontrada: "+id));}
    private void repositoryAdapterFinish(String id,MatchResult result){try{ArenaMatch match=required(id);minigame(match.minigame()).ifPresent(adapter->dispatcher.dispatch(()->{adapter.finish(result);adapter.reset();}));}catch(SQLException e){throw new CompletionException(e);}}
    private <T> CompletableFuture<T> query(SqlSupplier<T> op){return CompletableFuture.supplyAsync(()->{try{return op.get();}catch(SQLException e){throw new CompletionException(e);}},executor);}
    @FunctionalInterface private interface SqlSupplier<T>{T get()throws SQLException;}
    @FunctionalInterface public interface GameDispatcher{void dispatch(Runnable action);}
}
