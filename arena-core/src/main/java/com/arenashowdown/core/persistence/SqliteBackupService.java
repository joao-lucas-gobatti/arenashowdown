package com.arenashowdown.core.persistence;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.*;

/** Creates consistent SQLite snapshots away from the Minecraft server thread. */
public final class SqliteBackupService implements AutoCloseable {
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private final Database database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(Thread.ofPlatform().name("arena-backup-", 0).factory());

    public SqliteBackupService(Database database) { this.database = database; }

    public CompletableFuture<Path> createBackup(Path directory, int retention) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(directory);
                Path target = directory.resolve("arena-" + NAME_TIME.format(Instant.now()) + ".db").toAbsolutePath().normalize();
                String escaped = target.toString().replace("'", "''");
                try (Connection connection = database.connection(); Statement statement = connection.createStatement()) {
                    statement.execute("VACUUM INTO '" + escaped + "'");
                }
                verify(target);
                prune(directory, Math.max(1, retention));
                return target;
            } catch (SQLException | IOException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /** Restores to a destination that is not currently open by a Database instance. */
    public static void restore(Path backup, Path destination) throws SQLException, IOException {
        verify(backup);
        Path absoluteDestination = destination.toAbsolutePath().normalize();
        Files.createDirectories(absoluteDestination.getParent());
        Path temporary = absoluteDestination.resolveSibling(absoluteDestination.getFileName() + ".restore.tmp");
        Files.copy(backup, temporary, StandardCopyOption.REPLACE_EXISTING);
        verify(temporary);
        try {
            Files.move(temporary, absoluteDestination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, absoluteDestination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void verify(Path file) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
            if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) throw new SQLException("Backup SQLite inválido: " + file);
        }
    }

    private static void prune(Path directory, int retention) throws IOException {
        try (var files = Files.list(directory)) {
            var backups = files.filter(path -> path.getFileName().toString().matches("arena-\\d{8}-\\d{6}-\\d{3}\\.db"))
                .sorted(Comparator.comparingLong(SqliteBackupService::modified).reversed()).toList();
            for (int index = retention; index < backups.size(); index++) Files.deleteIfExists(backups.get(index));
        }
    }

    private static long modified(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch (IOException e) { return Long.MIN_VALUE; } }

    @Override public void close() {
        executor.shutdown();
        try { if (!executor.awaitTermination(30, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); executor.shutdownNow(); }
    }
}
