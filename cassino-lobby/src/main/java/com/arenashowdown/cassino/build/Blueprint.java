package com.arenashowdown.cassino.build;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

public final class Blueprint {
    private final List<BlockOperation> blocks = new ArrayList<>();

    public List<BlockOperation> blocks() { return blocks; }
    public void set(int x, int y, int z, Material material) { blocks.add(new BlockOperation(x, y, z, material)); }
    public void data(int x, int y, int z, Material material, String properties) {
        blocks.add(new BlockOperation(x,y,z,material,"minecraft:" + material.name().toLowerCase(java.util.Locale.ROOT) + "[" + properties + "]"));
    }

    public void box(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) set(x, y, z, material);
    }

    public void hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    if (x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2) set(x, y, z, material);
    }

    public void disc(int cx, int y, int cz, int radius, Material material) {
        int rr = radius * radius;
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++)
            if (x * x + z * z <= rr) set(cx + x, y, cz + z, material);
    }

    public void ring(int cx, int y, int cz, int radius, int thickness, Material material) {
        int outer = radius * radius, inner = (radius - thickness) * (radius - thickness);
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            int d = x * x + z * z;
            if (d <= outer && d >= inner) set(cx + x, y, cz + z, material);
        }
    }

    public void column(int x, int y1, int y2, int z, Material core, Material accent) {
        box(x - 1, y1, z - 1, x + 1, y2, z + 1, core);
        box(x - 2, y1, z - 2, x + 2, y1 + 1, z + 2, accent);
        box(x - 2, y2 - 1, z - 2, x + 2, y2, z + 2, accent);
    }
}

