package com.arenashowdown.parkour;

import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;

public final class ParkourListener implements Listener {
    private final ParkourAdapter adapter;public ParkourListener(ParkourAdapter adapter){this.adapter=adapter;}
    @EventHandler(ignoreCancelled=true)public void move(PlayerMoveEvent e){if(adapter.frozen(e.getPlayer().getUniqueId())&&e.hasChangedPosition()){e.setTo(e.getFrom());}}
    @EventHandler(ignoreCancelled=true)public void damage(EntityDamageEvent e){if(e.getEntity() instanceof org.bukkit.entity.Player p&&adapter.active(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true)public void breakBlock(BlockBreakEvent e){if(adapter.active(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true)public void placeBlock(BlockPlaceEvent e){if(adapter.active(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void death(PlayerDeathEvent e){if(adapter.active(e.getPlayer().getUniqueId())){e.setCancelled(true);}}
    @EventHandler public void quit(PlayerQuitEvent e){adapter.playerQuit(e.getPlayer());}
    @EventHandler public void join(PlayerJoinEvent e){adapter.recover(e.getPlayer());}
}
