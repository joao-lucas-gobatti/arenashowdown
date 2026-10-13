package com.arenashowdown.core.player;

import com.arenashowdown.core.service.MatchManager;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;

public final class ParticipantListener implements Listener {
    private final Plugin plugin; private final MatchManager matches;
    public ParticipantListener(Plugin plugin,MatchManager matches){this.plugin=plugin;this.matches=matches;}
    @EventHandler public void onQuit(PlayerQuitEvent event){matches.activeMatch(event.getPlayer().getUniqueId()).thenAccept(match->match.ifPresent(m->matches.disconnected(m.id(),event.getPlayer().getUniqueId())));}
    @EventHandler public void onJoin(PlayerJoinEvent event){matches.activeMatch(event.getPlayer().getUniqueId()).thenAccept(match->match.ifPresent(m->matches.reconnected(m.id(),event.getPlayer().getUniqueId()).thenAccept(accepted->{if(!accepted)plugin.getLogger().info(event.getPlayer().getName()+" retornou, mas a partida não aceita reconexão.");})));}
}
