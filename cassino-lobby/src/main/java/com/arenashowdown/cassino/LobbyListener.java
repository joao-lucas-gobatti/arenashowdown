package com.arenashowdown.cassino;

import com.arenashowdown.cassino.build.LobbyPlan;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class LobbyListener implements Listener {
    private final CassinoLobbyPlugin plugin;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    public LobbyListener(CassinoLobbyPlugin plugin) { this.plugin = plugin; }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("world.auto-teleport-first-join", true) || !plugin.markerExists()) return;
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) plugin.teleportToSpawn(player);
    }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock() || !plugin.isLobby(event.getPlayer().getWorld())) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (cooldown.getOrDefault(player.getUniqueId(), 0L) > now) return;
        int y = plugin.baseY();
        for (LobbyPlan.Portal portal : LobbyPlan.PORTALS) {
            Location center = new Location(player.getWorld(), portal.x() + .5, y + 7, portal.z() + .5);
            double dx = player.getX() - center.getX(), dz = player.getZ() - center.getZ();
            if (player.getY() >= y+3 && player.getY() <= y+5 && Math.abs(dx)<=1.5 && Math.abs(dz)<=3) {
                cooldown.put(player.getUniqueId(), now + 2500);
                plugin.activatePortal(player, portal.id());
                return;
            }
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent event) {
        if (protectedFrom(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler public void onPlace(BlockPlaceEvent event) {
        if (protectedFrom(event.getPlayer())) event.setCancelled(true);
    }
    @EventHandler public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && plugin.isLobby(player.getWorld())) event.setCancelled(true);
    }
    private boolean protectedFrom(Player player) {
        return plugin.isLobby(player.getWorld()) && plugin.getConfig().getBoolean("world.protect", true)
            && !(player.hasPermission("cassino.admin") && player.getGameMode() == GameMode.CREATIVE);
    }
}
