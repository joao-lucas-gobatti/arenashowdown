package com.arenashowdown.core;

import com.arenashowdown.api.ArenaApi;
import com.arenashowdown.core.command.ArenaCommand;
import com.arenashowdown.core.persistence.*;
import com.arenashowdown.core.service.ArenaService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;

public final class ArenaCorePlugin extends JavaPlugin {
    private Database database;
    private ArenaService service;

    @Override public void onEnable() {
        saveDefaultConfig();
        try {
            database = createDatabase();
            service = new ArenaService(new JdbcArenaRepository(database));
            getServer().getServicesManager().register(ArenaApi.class, service, this, ServicePriority.Normal);
            ArenaCommand command = new ArenaCommand(this, service, getConfig().getInt("ranking.default-limit", 10));
            var pluginCommand = getCommand("arena");
            if (pluginCommand == null) throw new IllegalStateException("Comando arena ausente do plugin.yml");
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            getLogger().info("ArenaCore habilitado com persistência " + getConfig().getString("database.type", "sqlite") + ".");
        } catch (Exception e) {
            getLogger().severe("Falha ao iniciar ArenaCore: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private Database createDatabase() throws Exception {
        String type = getConfig().getString("database.type", "sqlite").toLowerCase();
        if (type.equals("sqlite")) {
            Path file = getDataFolder().toPath().resolve(getConfig().getString("database.sqlite-file", "arena.db"));
            java.nio.file.Files.createDirectories(file.getParent());
            return Database.sqlite(file);
        }
        if (type.equals("postgresql")) {
            ConfigurationSection pg = getConfig().getConfigurationSection("database.postgres");
            if (pg == null) throw new IllegalArgumentException("Configuração database.postgres ausente");
            String url = "jdbc:postgresql://" + pg.getString("host") + ":" + pg.getInt("port",5432) + "/" + pg.getString("database");
            return new Database(url,pg.getString("username"),pg.getString("password"),pg.getInt("maximum-pool-size",8));
        }
        throw new IllegalArgumentException("Tipo de banco não suportado: " + type);
    }

    @Override public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (service != null) service.close();
        if (database != null) database.close();
    }
}
