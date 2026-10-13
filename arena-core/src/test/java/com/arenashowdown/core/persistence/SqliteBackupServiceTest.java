package com.arenashowdown.core.persistence;

import com.arenashowdown.api.model.Championship;
import com.arenashowdown.api.model.ChampionshipState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SqliteBackupServiceTest {
    @TempDir Path temporaryDirectory;

    @Test void backupCanRestorePersistentData() throws Exception {
        Path original = temporaryDirectory.resolve("arena.db");
        Path recovered = temporaryDirectory.resolve("recovered.db");
        Path backup;

        try (Database database = Database.sqlite(original); SqliteBackupService backups = new SqliteBackupService(database)) {
            JdbcArenaRepository repository = new JdbcArenaRepository(database);
            repository.createChampionship(new Championship("recovery", "Recovery Cup", ChampionshipState.WAITING, Instant.now()));
            backup = backups.createBackup(temporaryDirectory.resolve("backups"), 3).join();
            assertTrue(backup.toFile().isFile());
        }

        SqliteBackupService.restore(backup, recovered);
        try (Database restoredDatabase = Database.sqlite(recovered)) {
            var restored = new JdbcArenaRepository(restoredDatabase).championship("recovery");
            assertTrue(restored.isPresent());
            assertEquals("Recovery Cup", restored.orElseThrow().name());
        }
    }

    @Test void invalidBackupIsRejected() throws Exception {
        Path invalid = temporaryDirectory.resolve("invalid.db");
        java.nio.file.Files.writeString(invalid, "not a sqlite database");
        assertThrows(java.sql.SQLException.class, () -> SqliteBackupService.restore(invalid, temporaryDirectory.resolve("target.db")));
    }
}
