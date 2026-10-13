package com.arenashowdown.cassino;

import com.arenashowdown.cassino.build.Blueprint;
import com.arenashowdown.cassino.build.BuildRunner;
import com.arenashowdown.cassino.build.LobbyPlan;
import java.nio.file.Path;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

public final class CassinoLobbyPlugin extends org.bukkit.plugin.java.JavaPlugin implements TabExecutor {
    private LobbyDisplays displays;
    private boolean generating;

    @Override public void onEnable() {
        saveDefaultConfig();
        // V2 always has its own world; retain all user portal/ranking settings.
        if (!getConfig().contains("world.v1-name")) {
            String previous = getConfig().getString("world.name", "cassino_lobby");
            getConfig().set("world.v1-name", previous);
            getConfig().set("world.name", previous + "_v2");
            saveConfig();
        }
        displays = new LobbyDisplays(this);
        var command = getCommand("cassino");
        if (command == null) throw new IllegalStateException("Comando cassino ausente do plugin.yml");
        command.setExecutor(this); command.setTabCompleter(this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        World world = markerExists() ? getLobbyWorld(true) : getLobbyWorld(false);
        if (world != null && markerExists()) {
            configureWorld(world);
            Bukkit.getScheduler().runTask(this, () -> displays.spawn(world));
        } else if (getConfig().getBoolean("generation.auto-generate", false)) {
            Bukkit.getScheduler().runTask(this, () -> generate(Bukkit.getConsoleSender(), false));
        }
        long interval = Math.max(20L, getConfig().getLong("scoreboard.refresh-seconds", 10) * 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> displays.updateRanking(), interval, interval);
        getLogger().info("CassinoLobby pronto. Use /cassino generate.");
    }

    public int baseY() { return getConfig().getInt("world.base-y", 64); }
    public Path markerFile() { return getDataFolder().toPath().resolve("generated-v2-" + getConfig().getString("world.name").replaceAll("[^a-zA-Z0-9_-]", "_") + ".marker"); }
    public boolean markerExists() { return java.nio.file.Files.exists(markerFile()); }
    public boolean isLobby(World world) { return world != null && world.getName().equals(getConfig().getString("world.name", "cassino_lobby")); }
    public String portalTitle(String id) { return getConfig().getString("portals." + id + ".title", id.toUpperCase()); }

    public World getLobbyWorld(boolean create) {
        String name = getConfig().getString("world.name", "cassino_lobby");
        World loaded = Bukkit.getWorld(name);
        if (loaded != null || !create) return loaded;
        WorldCreator creator = new WorldCreator(name).environment(World.Environment.NORMAL).generateStructures(false);
        creator.generator(new ChunkGenerator() {
            @Override public @NotNull ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull BiomeGrid biome) {
                return createChunkData(world);
            }
        });
        return creator.createWorld();
    }

    public void finishGeneration(World world) {
        generating = false;
        configureWorld(world);
        displays.spawn(world);
    }

    private void configureWorld(World world) {
        int y = baseY();
        world.setSpawnLocation(new Location(world, .5, y + 4, 22.5, 180, 0));
        world.setTime(18000); world.setStorm(false); world.setThundering(false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
    }

    private void generate(CommandSender sender, boolean force) {
        if (generating) { sender.sendMessage("§cJá existe uma geração em andamento."); return; }
        if (markerExists() && !force) { sender.sendMessage("§eO lobby já foi gerado. Use /cassino regenerate CONFIRMAR para reconstruir."); return; }
        World world = getLobbyWorld(true);
        if (world == null) { sender.sendMessage("§cNão foi possível criar o mundo."); return; }
        if (force) displays.remove(world);
        generating = true;
        int base = baseY();
        sender.sendMessage("§6Preparando arquitetura V2 fora da thread principal...");
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Blueprint blueprint = LobbyPlan.create(base);
                Bukkit.getScheduler().runTask(this, () -> new BuildRunner(this, world, blueprint, sender).runTaskTimer(this, 1L, 1L));
            } catch (Exception error) {
                Bukkit.getScheduler().runTask(this, () -> generationFailed(error));
            }
        });
    }

    public void generationFailed(Throwable error) {
        generating = false;
        getLogger().log(java.util.logging.Level.SEVERE, "Falha na geração V2; mundo V1 preservado", error);
    }

    public void teleportToSpawn(Player player) {
        if (generating || !markerExists()) { player.sendMessage("§eAguarde a conclusão da geração V2."); return; }
        World world = getLobbyWorld(true);
        if (world != null) player.teleportAsync(new Location(world, .5, baseY() + 4, 22.5, 180, 0));
    }

    public void activatePortal(Player player, String id) {
        String root = "portals." + id;
        String command = getConfig().getString(root + ".command", "").trim();
        if (!command.isEmpty()) {
            player.performCommand(command.replace("%player%", player.getName()).replaceFirst("^/", ""));
            return;
        }
        String worldName = getConfig().getString(root + ".destination.world", "");
        World destination = Bukkit.getWorld(worldName);
        if (destination == null && !worldName.isBlank()
            && new java.io.File(new java.io.File(Bukkit.getWorldContainer(),worldName),"level.dat").isFile()) {
            destination = new WorldCreator(worldName).createWorld();
        }
        if (destination != null) {
            player.teleportAsync(new Location(destination, getConfig().getDouble(root + ".destination.x"), getConfig().getDouble(root + ".destination.y"), getConfig().getDouble(root + ".destination.z"), (float)getConfig().getDouble(root + ".destination.yaw"), (float)getConfig().getDouble(root + ".destination.pitch")));
        } else player.sendMessage("§6" + portalTitle(id) + " §7ainda não foi configurado. Use §f/cassino setportal " + id + "§7.");
    }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (args.length == 0) { help(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "view" -> {
                if (!(sender instanceof Player player) || !markerExists()) { sender.sendMessage("Entre no jogo após a geração."); break; }
                String spot=args.length>1?args[1]:"fachada";
                double[] point=switch(spot) {
                    case "teto" -> new double[]{0,8,6,180,-65};
                    case "galeria" -> new double[]{38,17,16,90,0};
                    case "lounge" -> new double[]{68,4,4,180,0};
                    case "fama" -> new double[]{-68,4,4,180,0};
                    case "salao" -> new double[]{0,4,22,180,-10};
                    default -> new double[]{0,1,83,180,-15};
                };
                player.teleportAsync(new Location(getLobbyWorld(true),point[0]+.5,baseY()+point[1],point[2]+.5,(float)point[3],(float)point[4]));
            }
            case "generate" -> generate(sender, false);
            case "regenerate" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("CONFIRMAR")) sender.sendMessage("§cConfirme com /cassino regenerate CONFIRMAR. A região do lobby será sobrescrita.");
                else generate(sender, true);
            }
            case "tp" -> { if (sender instanceof Player player) teleportToSpawn(player); else sender.sendMessage("§cSomente jogadores."); }
            case "setportal" -> setPortal(sender, args);
            case "reload" -> { reloadConfig(); sender.sendMessage("§aConfiguração recarregada."); }
            case "status" -> sender.sendMessage("§6CassinoLobby: §fworld=" + (getLobbyWorld(false) != null) + ", gerado=" + markerExists() + ", gerando=" + generating);
            default -> help(sender);
        }
        return true;
    }

    private void setPortal(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cSomente jogadores."); return; }
        if (args.length < 2 || LobbyPlan.PORTALS.stream().noneMatch(p -> p.id().equalsIgnoreCase(args[1]))) { sender.sendMessage("§cUso: /cassino setportal <id>"); return; }
        String id = args[1].toLowerCase(); Location at = player.getLocation(); String root = "portals." + id + ".destination";
        getConfig().set(root + ".world", at.getWorld().getName()); getConfig().set(root + ".x", at.getX()); getConfig().set(root + ".y", at.getY()); getConfig().set(root + ".z", at.getZ()); getConfig().set(root + ".yaw", at.getYaw()); getConfig().set(root + ".pitch", at.getPitch());
        getConfig().set("portals." + id + ".command", ""); saveConfig(); sender.sendMessage("§aDestino de " + id + " salvo na sua posição.");
    }

    private void help(CommandSender sender) { sender.sendMessage("§6/cassino §f<generate|regenerate CONFIRMAR|tp|setportal <id>|reload|status>"); }

    @Override public java.util.List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String @NotNull [] args) {
        if (args.length == 1) return java.util.List.of("generate", "regenerate", "tp", "setportal", "reload", "status");
        if (args.length == 2 && args[0].equalsIgnoreCase("setportal")) return LobbyPlan.PORTALS.stream().map(LobbyPlan.Portal::id).toList();
        if (args.length == 2 && args[0].equalsIgnoreCase("regenerate")) return java.util.List.of("CONFIRMAR");
        return java.util.List.of();
    }
}

