package com.arenashowdown.cassino.build;

import org.bukkit.Material;

public record BlockOperation(int x, int y, int z, Material material, String data) {
    public BlockOperation(int x, int y, int z, Material material) { this(x,y,z,material,null); }
}

