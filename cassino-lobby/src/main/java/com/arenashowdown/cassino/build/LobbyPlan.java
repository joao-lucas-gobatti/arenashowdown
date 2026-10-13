package com.arenashowdown.cassino.build;

import java.util.List;
import org.bukkit.Material;

public final class LobbyPlan {
    public record Portal(String id, String title, int x, int y, int z, float yaw, Material accent) {}
    public record Label(String text, double x, double y, double z, float yaw, float scale) {}

    public static final List<Portal> PORTALS = List.of(
        new Portal("corrida", "CORRIDA", -43, 67, -24, -90, Material.WHITE_CONCRETE),
        new Portal("skywars", "SKYWARS", -43, 67, -8, -90, Material.LIGHT_BLUE_CONCRETE),
        new Portal("battle_royale", "BATTLE ROYALE", -43, 67, 8, -90, Material.ORANGE_CONCRETE),
        new Portal("gamble", "GAMBLE", -43, 67, 24, -90, Material.LIME_CONCRETE),
        new Portal("parkour", "PARKOUR", 43, 67, 24, 90, Material.MAGENTA_CONCRETE),
        new Portal("roube_a_bandeira", "ROUBE A BANDEIRA", 43, 67, 8, 90, Material.BLUE_CONCRETE),
        new Portal("csgo", "CSGO", 43, 67, -8, 90, Material.GRAY_CONCRETE),
        new Portal("tnt_run", "TNT RUN", 43, 67, -24, 90, Material.RED_CONCRETE)
    );

    private LobbyPlan() {}

    public static Blueprint create(int baseY) {
        return GrandCasino.create(baseY);
    }

    private static Blueprint legacy(int baseY) {
        Blueprint b = new Blueprint();
        int f = baseY;
        // Ilha/base e praça externa (entrada ao sul).
        b.disc(0, f - 4, 0, 94, Material.POLISHED_BLACKSTONE);
        b.disc(0, f - 3, 0, 92, Material.DEEPSLATE_TILES);
        b.box(-12, f - 2, 42, 12, f - 2, 91, Material.SMOOTH_QUARTZ);
        for (int z = 45; z <= 91; z += 6) b.box(-11, f - 1, z, 11, f - 1, z + 2, Material.BLACK_CONCRETE);
        b.box(-36, f - 2, 48, -18, f - 2, 85, Material.RED_TERRACOTTA);
        b.box(18, f - 2, 48, 36, f - 2, 85, Material.RED_TERRACOTTA);
        // Escadaria monumental.
        for (int i = 0; i < 5; i++) b.box(-22 + i * 2, f - 1 + i, 37 + i * 2, 22 - i * 2, f - 1 + i, 42, Material.SMOOTH_QUARTZ);
        // Salão principal: piso art déco e paredes.
        b.box(-48, f, -40, 48, f, 38, Material.POLISHED_BLACKSTONE);
        b.box(-45, f + 1, -37, 45, f + 1, 35, Material.SMOOTH_QUARTZ);
        for (int r = 42; r >= 6; r -= 9) b.ring(0, f + 2, 0, r, 3, (r / 9) % 2 == 0 ? Material.RED_CONCRETE : Material.BLACK_CONCRETE);
        b.ring(0, f + 2, 0, 34, 2, Material.GOLD_BLOCK);
        b.ring(0, f + 2, 0, 17, 2, Material.GOLD_BLOCK);
        b.disc(0, f + 2, 0, 7, Material.RED_CONCRETE);
        // Paredes e teto escalonado.
        b.box(-48, f + 1, -40, -46, f + 24, 38, Material.POLISHED_DEEPSLATE);
        b.box(46, f + 1, -40, 48, f + 24, 38, Material.POLISHED_DEEPSLATE);
        b.box(-48, f + 1, -40, 48, f + 24, -38, Material.POLISHED_DEEPSLATE);
        b.box(-48, f + 1, 36, -18, f + 24, 38, Material.POLISHED_DEEPSLATE);
        b.box(18, f + 1, 36, 48, f + 24, 38, Material.POLISHED_DEEPSLATE);
        b.box(-49, f + 24, -41, 49, f + 26, 39, Material.BLACK_CONCRETE);
        b.box(-43, f + 27, -35, 43, f + 29, 33, Material.RED_CONCRETE);
        b.box(-36, f + 30, -28, 36, f + 31, 26, Material.POLISHED_BLACKSTONE);
        // Colunas simétricas e molduras douradas.
        for (int x : new int[]{-42, -28, -14, 14, 28, 42}) {
            b.column(x, f + 2, f + 23, -34, Material.SMOOTH_QUARTZ, Material.GOLD_BLOCK);
            b.column(x, f + 2, f + 23, 32, Material.SMOOTH_QUARTZ, Material.GOLD_BLOCK);
        }
        for (int z : new int[]{-22, -2, 18}) {
            b.column(-42, f + 2, f + 23, z, Material.SMOOTH_QUARTZ, Material.GOLD_BLOCK);
            b.column(42, f + 2, f + 23, z, Material.SMOOTH_QUARTZ, Material.GOLD_BLOCK);
        }
        // Portais: arcos de 11x13, interior escuro e acento específico.
        for (Portal p : PORTALS) portal(b, p, f);
        // Palco e placar ao norte.
        for (int i = 0; i < 4; i++) b.box(-18 + i, f + 2 + i, -35 + i, 18 - i, f + 2 + i, -29, Material.RED_NETHER_BRICKS);
        b.box(-17, f + 6, -37, 17, f + 19, -36, Material.BLACK_CONCRETE);
        b.box(-19, f + 5, -38, 19, f + 20, -38, Material.GOLD_BLOCK);
        b.box(-16, f + 7, -39, 16, f + 18, -39, Material.RED_CONCRETE);
        // Cortinas laterais.
        b.box(-22, f + 5, -36, -19, f + 21, -33, Material.RED_WOOL);
        b.box(19, f + 5, -36, 22, f + 21, -33, Material.RED_WOOL);
        // Lustre central em camadas.
        b.box(-1, f + 22, -1, 1, f + 30, 1, Material.GOLD_BLOCK);
        b.ring(0, f + 21, 0, 9, 2, Material.GOLD_BLOCK);
        b.ring(0, f + 19, 0, 6, 2, Material.SEA_LANTERN);
        b.disc(0, f + 17, 0, 3, Material.GLOWSTONE);
        // Mezaninos.
        b.box(-44, f + 14, -30, -36, f + 15, 27, Material.DARK_OAK_PLANKS);
        b.box(36, f + 14, -30, 44, f + 15, 27, Material.DARK_OAK_PLANKS);
        for (int z = -30; z <= 27; z += 3) {
            b.set(-35, f + 16, z, Material.GOLD_BLOCK); b.set(35, f + 16, z, Material.GOLD_BLOCK);
        }
        // Hall da fama, lado oeste externo.
        room(b, -84, f, -30, -56, 30, Material.GOLD_BLOCK);
        b.box(-78, f + 2, -14, -62, f + 3, 14, Material.SMOOTH_QUARTZ);
        b.box(-74, f + 4, -7, -66, f + 6, 7, Material.GOLD_BLOCK);
        // Lounge VIP, lado leste externo.
        room(b, 56, f, -30, 84, 30, Material.RED_NETHER_BRICKS);
        for (int z = -18; z <= 18; z += 12) {
            b.box(63, f + 2, z - 2, 68, f + 3, z + 2, Material.RED_WOOL);
            b.box(72, f + 2, z - 2, 77, f + 3, z + 2, Material.RED_WOOL);
            b.box(69, f + 2, z - 1, 71, f + 3, z + 1, Material.DARK_OAK_PLANKS);
        }
        // Fachada art déco e marquee.
        b.box(-28, f + 5, 37, -19, f + 28, 40, Material.SMOOTH_QUARTZ);
        b.box(19, f + 5, 37, 28, f + 28, 40, Material.SMOOTH_QUARTZ);
        b.box(-30, f + 24, 37, 30, f + 28, 40, Material.GOLD_BLOCK);
        b.box(-24, f + 29, 38, 24, f + 34, 40, Material.RED_CONCRETE);
        b.box(-18, f + 35, 38, 18, f + 37, 40, Material.GOLD_BLOCK);
        // Dados gigantes da praça.
        die(b, -29, f, 56); die(b, 29, f, 56);
        // Fichas/roletas planas nas laterais.
        b.disc(-52, f, 61, 9, Material.RED_CONCRETE); b.ring(-52, f + 1, 61, 9, 2, Material.GOLD_BLOCK);
        b.disc(52, f, 61, 9, Material.BLACK_CONCRETE); b.ring(52, f + 1, 61, 9, 2, Material.GOLD_BLOCK);
        // Iluminação perimetral.
        for (int a = 0; a < 360; a += 10) {
            double rad = Math.toRadians(a); int x = (int)Math.round(Math.cos(rad) * 88), z = (int)Math.round(Math.sin(rad) * 88);
            b.set(x, f - 1, z, Material.SEA_LANTERN);
        }
        return b;
    }

    private static void portal(Blueprint b, Portal p, int f) {
        boolean side = Math.abs(p.x()) > 40;
        if (side) {
            b.box(p.x() - 1, f + 2, p.z() - 6, p.x() + 1, f + 14, p.z() + 6, Material.GOLD_BLOCK);
            b.box(p.x() - 2, f + 4, p.z() - 4, p.x() + 2, f + 12, p.z() + 4, p.accent());
            b.box(p.x() - 3, f + 4, p.z() - 3, p.x() + 3, f + 11, p.z() + 3, Material.BLACK_STAINED_GLASS);
        } else {
            b.box(p.x() - 6, f + 2, p.z() - 1, p.x() + 6, f + 14, p.z() + 1, Material.GOLD_BLOCK);
            b.box(p.x() - 4, f + 4, p.z() - 2, p.x() + 4, f + 12, p.z() + 2, p.accent());
            b.box(p.x() - 3, f + 4, p.z() - 3, p.x() + 3, f + 11, p.z() + 3, Material.BLACK_STAINED_GLASS);
        }
    }

    private static void room(Blueprint b, int x1, int f, int z1, int x2, int z2, Material accent) {
        b.box(x1, f, z1, x2, f, z2, Material.POLISHED_BLACKSTONE);
        b.hollowBox(x1, f + 1, z1, x2, f + 17, z2, Material.POLISHED_DEEPSLATE);
        b.box(x1 + 2, f + 16, z1 + 2, x2 - 2, f + 17, z2 - 2, accent);
    }

    private static void die(Blueprint b, int cx, int f, int cz) {
        b.box(cx - 5, f, cz - 5, cx + 5, f + 10, cz + 5, Material.WHITE_CONCRETE);
        for (int[] dot : new int[][]{{-3,-3},{3,-3},{0,0},{-3,3},{3,3}})
            b.box(cx + dot[0] - 1, f + 10, cz + dot[1] - 1, cx + dot[0] + 1, f + 11, cz + dot[1] + 1, Material.BLACK_CONCRETE);
    }
}
