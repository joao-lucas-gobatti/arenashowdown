package com.arenashowdown.parkour;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.parkour.arena.ArenaStore;
import com.arenashowdown.parkour.command.ParkourCommand;
import com.arenashowdown.parkour.player.PersistentSnapshotStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArenaParkourPlugin extends JavaPlugin {
    private ArenaStore arenas;private PersistentSnapshotStore snapshots;private ParkourAdapter adapter;
    @Override public void onEnable(){saveDefaultConfig();RegisteredServiceProvider<OrchestrationApi>registration=getServer().getServicesManager().getRegistration(OrchestrationApi.class);if(registration==null){getLogger().severe("ArenaCore nao publicou OrchestrationApi");getServer().getPluginManager().disablePlugin(this);return;}try{arenas=new ArenaStore(getDataFolder().toPath().resolve("arenas"));arenas.loadAll();snapshots=new PersistentSnapshotStore(this,getDataFolder().toPath().resolve(getConfig().getString("snapshot-directory","snapshots")));adapter=new ParkourAdapter(this,registration.getProvider(),arenas,snapshots);registration.getProvider().registerMinigame(adapter);ParkourCommand command=new ParkourCommand(this,arenas,adapter);var pc=getCommand("parkour");if(pc==null)throw new IllegalStateException("Comando parkour ausente");pc.setExecutor(command);pc.setTabCompleter(command);getServer().getPluginManager().registerEvents(new ParkourListener(adapter),this);Bukkit.getOnlinePlayers().forEach(adapter::recover);getLogger().info("ArenaParkour habilitado; "+arenas.all().size()+" arena(s) carregada(s).");}catch(Exception e){getLogger().severe("Falha ao iniciar ArenaParkour: "+e.getMessage());getServer().getPluginManager().disablePlugin(this);}}
    @Override public void onDisable(){if(adapter!=null){getServer().getServicesManager().load(OrchestrationApi.class).unregisterMinigame("parkour");adapter.cancel("Plugin desabilitado");}if(snapshots!=null)snapshots.close();}
}
