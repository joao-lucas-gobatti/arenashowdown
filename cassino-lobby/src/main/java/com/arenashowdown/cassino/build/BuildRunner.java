package com.arenashowdown.cassino.build;

import com.arenashowdown.cassino.CassinoLobbyPlugin;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public final class BuildRunner extends BukkitRunnable {
    private final CassinoLobbyPlugin plugin;
    private final World world;
    private final List<BlockOperation> blocks;
    private final CommandSender sender;
    private final int perTick;
    private int cursor;
    private boolean loading;
    private org.bukkit.Chunk held;
    private final java.util.Map<String, org.bukkit.block.data.BlockData> data = new java.util.HashMap<>();
    private int ticks;

    public BuildRunner(CassinoLobbyPlugin plugin, World world, Blueprint blueprint, CommandSender sender) {
        this.plugin = plugin;
        this.world = world;
        this.blocks = blueprint.blocks();
        // Stable sort: writes within each chunk retain architectural layering order.
        this.blocks.sort(java.util.Comparator.comparingInt((BlockOperation op) -> op.x() >> 4).thenComparingInt(op -> op.z() >> 4));
        this.sender = sender;
        this.perTick = Math.max(500, plugin.getConfig().getInt("generation.blocks-per-tick", 6000));
    }

    @Override public void run() {
        if (loading) return;
        try {
        long deadline = System.nanoTime() + 4_000_000L;
        int end = Math.min(cursor + perTick, blocks.size());
        for (; cursor < end; cursor++) {
            BlockOperation op = blocks.get(cursor);
            int cx=op.x()>>4, cz=op.z()>>4;
            if (held == null || held.getX()!=cx || held.getZ()!=cz) {
                if (held != null) held.removePluginChunkTicket(plugin);
                held=null; loading=true;
                world.getChunkAtAsync(cx,cz,true).whenComplete((chunk,error) -> org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    loading=false;
                    if (error!=null) { cancel(); plugin.generationFailed(error); return; }
                    held=chunk; held.addPluginChunkTicket(plugin);
                }));
                return;
            }
            if (op.data()==null) world.getBlockAt(op.x(), op.y(), op.z()).setType(op.material(), false);
            else world.getBlockAt(op.x(),op.y(),op.z()).setBlockData(data.computeIfAbsent(op.data(), org.bukkit.Bukkit::createBlockData),false);
            if (System.nanoTime()>=deadline) { cursor++; break; }
        }
        if (cursor < blocks.size()) {
            if (++ticks % 100 == 0) sender.sendMessage("§6Lobby V2: §f" + (cursor * 100 / blocks.size()) + "% concluído.");
            return;
        }
        cancel();
        if (held!=null) held.removePluginChunkTicket(plugin);
        plugin.finishGeneration(world);
        world.save();
        try {
            Files.createDirectories(plugin.markerFile().getParent());
            Files.writeString(plugin.markerFile(), "version=2\nworld=" + world.getName() + "\n");
        } catch (IOException e) {
            plugin.getLogger().warning("Não foi possível salvar o marcador: " + e.getMessage());
        }
        sender.sendMessage("§aLobby cassino gerado com " + blocks.size() + " operações de bloco.");
        } catch (Exception error) {
            cancel(); if (held!=null) held.removePluginChunkTicket(plugin); plugin.generationFailed(error);
        }
    }
}

