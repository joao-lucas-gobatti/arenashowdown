package com.arenashowdown.cassino;

import com.arenashowdown.cassino.build.LobbyPlan;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public final class LobbyDisplays {
    public static final String TAG = "cassino_lobby_managed";
    private final CassinoLobbyPlugin plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
    private TextDisplay ranking;

    public LobbyDisplays(CassinoLobbyPlugin plugin) { this.plugin = plugin; }

    public void spawn(World world) {
        // Load and retain the label chunks before removing persisted displays on restart.
        int[][] anchors={{0,48},{-51,0},{50,0},{0,-36},{-40,-24},{-40,-8},{-40,8},{-40,24},{40,-24},{40,-8},{40,8},{40,24}};
        for(int[] anchor:anchors) world.getChunkAt(anchor[0]>>4,anchor[1]>>4).addPluginChunkTicket(plugin);
        remove(world);
        int y = plugin.baseY();
        text(world, "§6§lARENA SHOWDOWN\n§fTHE GRAND DICE", 0.5, y + 20, 48.2, 0, 1.8f);
        text(world, "§6§lHALL DA FAMA", -50.2, y + 9, 0.5, -90, .8f);
        text(world, "§6§lVIP LOUNGE", 50.2, y + 9, 0.5, 90, .8f);
        text(world, "§f§lPALCO PRINCIPAL", 0.5, y + 20, -35.2, 0, 1.15f);
        for (LobbyPlan.Portal portal : LobbyPlan.PORTALS) {
            double x = portal.x(), z = portal.z();
            if (Math.abs(x) > 40) x += x < 0 ? 3.3 : -3.3;
            else z -= 3.3;
            text(world, "§6◆ §f§l" + plugin.portalTitle(portal.id()) + " §6◆", x + .5, y + 13, z + .5, portal.yaw(), .85f);
        }
        ranking = text(world, scoreboardText(List.of()), .5, y + 14, -35.1, 0, 1.05f);
        updateRanking();
    }

    public void remove(World world) {
        for (Entity entity : world.getEntities()) if (entity.getScoreboardTags().contains(TAG)) entity.remove();
    }

    public void updateRanking() {
        if (ranking == null || !ranking.isValid()) return;
        Object arenaApi = findArenaApi();
        if (arenaApi == null) { ranking.text(legacy.deserialize(scoreboardText(List.of()))); return; }
        try {
            Method method = arenaApi.getClass().getMethod("ranking", String.class, int.class);
            Object value = method.invoke(arenaApi, plugin.getConfig().getString("scoreboard.championship-id", "principal"), plugin.getConfig().getInt("scoreboard.max-entries", 8));
            if (value instanceof CompletableFuture<?> future) future.whenComplete((result, error) -> {
                if (!plugin.isEnabled()) return;
                List<String> lines = new ArrayList<>();
                if (error == null && result instanceof List<?> list) for (Object entry : list) lines.add(readEntry(entry));
                Bukkit.getScheduler().runTask(plugin, () -> { if (ranking != null && ranking.isValid()) ranking.text(legacy.deserialize(scoreboardText(lines))); });
            });
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("ArenaCore encontrado, mas o ranking não pôde ser consultado: " + e.getMessage());
        }
    }

    private Object findArenaApi() {
        var core = Bukkit.getPluginManager().getPlugin("ArenaCore");
        if (core == null || !core.isEnabled()) return null;
        Collection<RegisteredServiceProvider<?>> registrations = Bukkit.getServicesManager().getRegistrations(core);
        for (RegisteredServiceProvider<?> registration : registrations)
            if (registration.getService().getName().equals("com.arenashowdown.api.ArenaApi")) return registration.getProvider();
        return null;
    }

    private String readEntry(Object entry) {
        try {
            String name = String.valueOf(entry.getClass().getMethod("playerName").invoke(entry));
            Object points = entry.getClass().getMethod("points").invoke(entry);
            return name + " §6- §f" + points;
        } catch (ReflectiveOperationException e) { return String.valueOf(entry); }
    }

    private String scoreboardText(List<String> entries) {
        StringBuilder text = new StringBuilder("§6§l").append(plugin.getConfig().getString("scoreboard.title", "ARENA SHOWDOWN")).append("\n§8━━━━━━━━━━━━━━\n");
        List<String> lines = entries.isEmpty() ? plugin.getConfig().getStringList("scoreboard.fallback-lines") : entries;
        for (int i = 0; i < lines.size(); i++) text.append("§6").append(i + 1).append(". §f").append(lines.get(i)).append('\n');
        return text.append("§8━━━━━━━━━━━━━━").toString();
    }

    private TextDisplay text(World world, String value, double x, double y, double z, float yaw, float scale) {
        TextDisplay display = world.spawn(new Location(world, x, y, z, yaw, 0), TextDisplay.class, entity -> {
            entity.text(legacy.deserialize(value));
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setSeeThrough(false);
            entity.setShadowed(true);
            entity.setBackgroundColor(org.bukkit.Color.fromARGB(180, 8, 8, 8));
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setTransformation(new Transformation(new Vector3f(), entity.getTransformation().getLeftRotation(), new Vector3f(scale), entity.getTransformation().getRightRotation()));
            entity.addScoreboardTag(TAG);
            entity.setPersistent(true);
        });
        return display;
    }
}

