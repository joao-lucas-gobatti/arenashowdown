package com.arenashowdown.parkour.palace;

import java.util.*;
import org.bukkit.Material;

/** Last-write-wins voxel plan, independent of a running Bukkit server. */
public final class PalaceBlocks {
    public record Pos(int x,int y,int z) {}
    public record Block(Pos pos,Material material,String data) {}
    private final Map<Pos,Block> cells=new HashMap<>();
    public void set(int x,int y,int z,Material m){Pos p=new Pos(x,y,z);cells.put(p,new Block(p,m,null));}
    public void state(int x,int y,int z,Material m,String properties){Pos p=new Pos(x,y,z);cells.put(p,new Block(p,m,"minecraft:"+m.name().toLowerCase(Locale.ROOT)+"["+properties+"]"));}
    public void box(int x,int y,int z,int X,int Y,int Z,Material m){for(int a=Math.min(x,X);a<=Math.max(x,X);a++)for(int b=Math.min(y,Y);b<=Math.max(y,Y);b++)for(int c=Math.min(z,Z);c<=Math.max(z,Z);c++)set(a,b,c,m);}
    public Material get(int x,int y,int z){Block b=cells.get(new Pos(x,y,z));return b==null?Material.AIR:b.material;}
    public List<Block> ordered(){var list=new ArrayList<>(cells.values());list.sort(Comparator.comparingInt((Block b)->b.pos.x>>4).thenComparingInt(b->b.pos.z>>4).thenComparingInt(b->b.pos.y));return list;}
}
