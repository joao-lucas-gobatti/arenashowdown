package com.arenashowdown.tntrun;

import com.arenashowdown.api.OrchestrationApi;
import com.arenashowdown.tntrun.arena.ArenaStore;
import com.arenashowdown.tntrun.persistence.*;
import com.arenashowdown.tntrun.configuration.PluginConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;

public final class ArenaTNTRunPlugin extends JavaPlugin {
    private TntRunAdapter adapter;
    private OrchestrationApi api;
    private boolean registered;
    @Override public void onEnable() {
        api=getServer().getServicesManager().load(OrchestrationApi.class);
        if (api==null) { getLogger().severe("ArenaCore did not publish OrchestrationApi"); getServer().getPluginManager().disablePlugin(this); return; }
        try {
            ArenaStore arenas=new ArenaStore(getDataFolder().toPath().resolve("arenas"));
            PluginConfiguration configuration=new PluginConfiguration(getDataFolder().toPath(),arenas); configuration.initialize();
            var recovery=getDataFolder().toPath().resolve("recovery");
            adapter=new TntRunAdapter(this,api,arenas,new BlockJournal(recovery),new PlayerSnapshots(recovery.resolve("players")),new ResultOutbox(recovery),configuration);
            TntRunCommand command=new TntRunCommand(this,arenas,adapter);
            var pc=Objects.requireNonNull(getCommand("tntrun")); pc.setExecutor(command); pc.setTabCompleter(command);
            getServer().getPluginManager().registerEvents(new TntRunListener(this,adapter),this);
            // A failed recovery keeps the plugin alive, protection active, and new rounds blocked.
            try { adapter.recoverPersistent(); } catch (Exception e) { getLogger().severe("Recovery retained; /tntrun recover: "+e); }
            api.registerMinigame(adapter); registered=true;
            getLogger().info("ArenaTNTRun registered as tntrun; arenas="+arenas.all().size());
        } catch (Exception e) { getLogger().severe("ArenaTNTRun startup failed: "+e); getServer().getPluginManager().disablePlugin(this); }
    }
    @Override public void onDisable() {
        if (registered) api.unregisterMinigame("tntrun");
        if (adapter!=null) adapter.shutdown();
    }
}
