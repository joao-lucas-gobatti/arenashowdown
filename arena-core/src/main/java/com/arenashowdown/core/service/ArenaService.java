package com.arenashowdown.core.service;

import com.arenashowdown.api.ArenaApi;
import com.arenashowdown.api.model.*;
import com.arenashowdown.core.persistence.ArenaRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public final class ArenaService implements ArenaApi, AutoCloseable {
    private final ArenaRepository repository;
    private final ExecutorService databaseExecutor;

    public ArenaService(ArenaRepository repository) {
        this(repository, Executors.newFixedThreadPool(2, Thread.ofPlatform().name("arena-db-", 0).factory()));
    }
    public ArenaService(ArenaRepository repository, ExecutorService databaseExecutor) { this.repository = repository; this.databaseExecutor = databaseExecutor; }

    public CompletableFuture<Championship> createChampionship(String id, String name) {
        return query(() -> { validateId(id); var c = new Championship(id.toLowerCase(Locale.ROOT), name, ChampionshipState.WAITING, Instant.now()); repository.createChampionship(c); return c; });
    }
    @Override public CompletableFuture<Championship> championship(String id) { return query(() -> repository.championship(id).orElseThrow(() -> new IllegalArgumentException("Campeonato não encontrado: " + id))); }
    public CompletableFuture<List<Championship>> championships() { return query(repository::championships); }
    public CompletableFuture<Championship> transition(String id, ChampionshipState next) {
        return query(() -> { var current = repository.championship(id).orElseThrow(() -> new IllegalArgumentException("Campeonato não encontrado: " + id)); if (!current.state().canTransitionTo(next)) throw new IllegalStateException("Transição inválida: " + current.state() + " -> " + next); if (!repository.updateState(id,current.state(),next)) throw new IllegalStateException("Estado alterado concorrentemente"); return new Championship(current.id(),current.name(),next,current.createdAt()); });
    }
    @Override public CompletableFuture<Void> registerPlayer(String id, UUID playerId, String name, String teamId) { return query(() -> { var c=repository.championship(id).orElseThrow(() -> new IllegalArgumentException("Campeonato não encontrado: "+id)); if(c.state()!=ChampionshipState.WAITING) throw new IllegalStateException("Inscrições encerradas"); repository.registerPlayer(id,playerId,name,teamId); return null; }); }
    @Override public CompletableFuture<Boolean> awardPoints(String id, UUID playerId, int points, String source, String eventId) { return query(() -> { if(points==0) throw new IllegalArgumentException("Pontos não podem ser zero"); var c=repository.championship(id).orElseThrow(() -> new IllegalArgumentException("Campeonato não encontrado: "+id)); if(c.state()!=ChampionshipState.RUNNING) throw new IllegalStateException("Campeonato não está em execução"); return repository.insertScoreEvent(eventId,id,playerId,points,source); }); }
    @Override public CompletableFuture<List<RankingEntry>> ranking(String id, int limit) { return query(() -> repository.ranking(id,Math.max(1,Math.min(limit,100)))); }
    private void validateId(String id) { if(id==null || !id.matches("[a-zA-Z0-9_-]{2,64}")) throw new IllegalArgumentException("ID deve ter 2-64 caracteres: letras, números, _ ou -"); }
    private <T> CompletableFuture<T> query(SqlSupplier<T> operation) { return CompletableFuture.supplyAsync(() -> { try { return operation.get(); } catch (SQLException e) { throw new CompletionException(e); } },databaseExecutor); }
    @Override public void close() { databaseExecutor.shutdown(); try { if(!databaseExecutor.awaitTermination(5,TimeUnit.SECONDS)) databaseExecutor.shutdownNow(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); databaseExecutor.shutdownNow(); } }
    @FunctionalInterface private interface SqlSupplier<T> { T get() throws SQLException; }
}
