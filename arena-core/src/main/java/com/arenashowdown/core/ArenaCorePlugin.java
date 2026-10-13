package com.arenashowdown.core;

import com.arenashowdown.api.ArenaApi;
import com.arenashowdown.core.command.ArenaCommand;
import com.arenashowdown.core.persistence.*;
import com.arenashowdown.core.service.ArenaService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;

public final class ArenaCorePlugin extends JavaPlugin {
    private Database database;
    private ArenaService service;
    private SqliteBackupService backupService;

    @Override public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        saveBundledYaml("minigames.yml");
        saveBundledYaml("scoring.yml");
        try {
            database = createDatabase();
            service = new ArenaService(new JdbcArenaRepository(database));
            backupService = new SqliteBackupService(database);
            getServer().getServicesManager().register(ArenaApi.class, service, this, ServicePriority.Normal);
            ArenaCommand command = new ArenaCommand(this, service, getConfig().getInt("ranking.default-limit", 10));
            var pluginCommand = getCommand("arena");
            if (pluginCommand == null) throw new IllegalStateException("Comando arena ausente do plugin.yml");
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            scheduleStartupBackup();
            getLogger().info("ArenaCore habilitado com persistência SQLite.");
        } catch (Exception e) {
            getLogger().severe("Falha ao iniciar ArenaCore: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private Database createDatabase() throws Exception {
        Path file = getDataFolder().toPath().resolve(getConfig().getString("database.sqlite-file", "arena.db"));
        java.nio.file.Files.createDirectories(file.getParent());
        return Database.sqlite(file, getConfig().getInt("database.busy-timeout-ms", 5_000));
    }

    private void saveBundledYaml(String name) {
        if (!getDataFolder().toPath().resolve(name).toFile().exists()) saveResource(name, false);
    }

    private void scheduleStartupBackup() {
        if (!getConfig().getBoolean("backup.enabled", true) || !getConfig().getBoolean("backup.on-startup", true)) return;
        Path directory = getDataFolder().toPath().resolve(getConfig().getString("backup.directory", "backups"));
        int retention = getConfig().getInt("backup.retention", 10);
        backupService.createBackup(directory, retention).whenComplete((path, failure) -> {
            if (failure == null) getLogger().info("Backup SQLite verificado: " + path.getFileName());
            else getLogger().severe("Falha no backup SQLite: " + failure.getMessage());
        });
    }

    @Override public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (backupService != null) backupService.close();
        if (service != null) service.close();
        if (database != null) database.close();
    }
}
