package com.arenashowdown.core.player;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

/** Main-thread-only snapshots for safe restoration after a match. */
public final class PlayerSessionManager {
    private final Map<UUID,Snapshot> snapshots=new HashMap<>();
    public void capture(Player player){ensureMain();snapshots.putIfAbsent(player.getUniqueId(),new Snapshot(player.getInventory().getContents().clone(),player.getInventory().getArmorContents().clone(),player.getLocation().clone(),player.getGameMode(),player.getHealth(),player.getFoodLevel()));}
    public void restore(Player player,boolean returnToLobby){ensureMain();Snapshot s=snapshots.remove(player.getUniqueId());if(s!=null){player.getInventory().setContents(s.contents());player.getInventory().setArmorContents(s.armor());player.setGameMode(s.gameMode());player.setHealth(Math.min(s.health(),player.getMaxHealth()));player.setFoodLevel(s.food());player.teleport(returnToLobby?player.getWorld().getSpawnLocation():s.location());}else if(returnToLobby)player.teleport(player.getWorld().getSpawnLocation());}
    public boolean hasSnapshot(UUID playerId){return snapshots.containsKey(playerId);}
    private void ensureMain(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("PlayerSessionManager deve ser usado na thread principal");}
    private record Snapshot(ItemStack[] contents,ItemStack[] armor,Location location,GameMode gameMode,double health,int food){}
}
