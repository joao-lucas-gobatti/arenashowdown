package com.arenashowdown.tntrun;

import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** The only floor mutation permitted is the adapter's delayed, physics-free removal. */
public final class TntRunListener implements Listener {
    private final JavaPlugin plugin;
    private final TntRunAdapter game;
    public TntRunListener(JavaPlugin plugin,TntRunAdapter game) { this.plugin=plugin; this.game=game; }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(PlayerMoveEvent e) {
        if (game.restoring() || game.internalMove()) return;
        if (game.frozen(e.getPlayer().getUniqueId()) && e.hasChangedPosition()) { e.setTo(e.getFrom()); return; }
        if (game.spectator(e.getPlayer().getUniqueId()) && !game.inside(e.getTo())) { e.setTo(e.getFrom()); return; }
        if (!game.active(e.getPlayer().getUniqueId()) && game.busy() && game.inside(e.getTo())) e.setTo(e.getFrom());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e) {
        if (!game.allowTeleport(e.getPlayer().getUniqueId(),e.getTo())) e.setCancelled(true);
    }
    @EventHandler public void quit(PlayerQuitEvent e) { game.quit(e.getPlayer()); }
    @EventHandler public void join(PlayerJoinEvent e) { plugin.getServer().getScheduler().runTask(plugin,() -> game.recoverJoin(e.getPlayer())); }
    @EventHandler public void world(WorldLoadEvent e) {
        if (game.status().equals("RECOVERY_PENDING")) plugin.getServer().getScheduler().runTask(plugin,() -> {
            try { game.recoverPersistent(); } catch (Exception failure) { plugin.getLogger().warning("Recovery retained: "+failure); }
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && game.protectedPlayer(p.getUniqueId())) e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent hit) {
            Entity attacker=hit.getDamager();
            if (attacker instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) attacker=shooter;
            if (attacker instanceof Player p && game.protectedPlayer(p.getUniqueId())) e.setCancelled(true);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void food(FoodLevelChangeEvent e) { if (e.getEntity() instanceof Player p && game.protectedPlayer(p.getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e) { if (game.protectedLocation(e.getBlock().getLocation()) || game.protectedPlayer(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e) { if (game.protectedLocation(e.getBlock().getLocation()) || game.protectedPlayer(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e) {
        if (game.protectedPlayer(e.getPlayer().getUniqueId()) || (e.getClickedBlock()!=null && game.protectedLocation(e.getClickedBlock().getLocation()))) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void interactEntity(PlayerInteractEntityEvent e) { if (game.protectedPlayer(e.getPlayer().getUniqueId()) || game.protectedLocation(e.getRightClicked().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void click(InventoryClickEvent e) { if (game.protectedPlayer(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drag(InventoryDragEvent e) { if (game.protectedPlayer(e.getWhoClicked().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drop(PlayerDropItemEvent e) { if (game.protectedPlayer(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && game.protectedPlayer(p.getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e) { if (game.protectedPlayer(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void flight(PlayerToggleFlightEvent e) { if (game.protectedPlayer(e.getPlayer().getUniqueId()) && !game.spectator(e.getPlayer().getUniqueId())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void commands(PlayerCommandPreprocessEvent e) {
        if (!game.protectedPlayer(e.getPlayer().getUniqueId())) return;
        String cmd=e.getMessage().trim().toLowerCase(java.util.Locale.ROOT);
        if (!cmd.matches("/(?:tntrun|arenatntrun:tntrun)\\s+leave\\s*") && !e.getPlayer().hasPermission("arenatntrun.admin")) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityBlock(EntityChangeBlockEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void physics(BlockPhysicsEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void flow(BlockFromToEvent e) { if (game.protectedLocation(e.getBlock().getLocation()) || game.protectedLocation(e.getToBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void ignite(BlockIgniteEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spread(BlockSpreadEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void grow(BlockGrowEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void form(BlockFormEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonExtendEvent e) {
        if (game.protectedLocation(e.getBlock().getLocation()) || e.getBlocks().stream().anyMatch(b -> game.protectedLocation(b.getLocation()) || game.protectedLocation(b.getRelative(e.getDirection()).getLocation()))) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e) {
        if (game.protectedLocation(e.getBlock().getLocation()) || e.getBlocks().stream().anyMatch(b -> game.protectedLocation(b.getLocation()) || game.protectedLocation(b.getRelative(e.getDirection()).getLocation()))) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(EntityExplodeEvent e) {
        if (game.protectedLocation(e.getLocation()) || e.blockList().stream().anyMatch(b -> game.protectedLocation(b.getLocation()))) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(BlockExplodeEvent e) {
        if (game.protectedLocation(e.getBlock().getLocation()) || e.blockList().stream().anyMatch(b -> game.protectedLocation(b.getLocation()))) e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void prime(ExplosionPrimeEvent e) { if (game.protectedLocation(e.getEntity().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void tnt(TNTPrimeEvent e) { if (game.protectedLocation(e.getBlock().getLocation())) e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void potion(EntityPotionEffectEvent e) { if (!game.restoring() && e.getEntity() instanceof Player p && game.protectedPlayer(p.getUniqueId()) && e.getCause()!=EntityPotionEffectEvent.Cause.PLUGIN) e.setCancelled(true); }
}
