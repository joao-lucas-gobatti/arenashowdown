package com.arenashowdown.parkour.wonder;

import java.util.*;
import org.bukkit.Material;

/** Deterministic last-write-wins voxel canvas; safe to compose off the server thread. */
public final class WonderBlocks {
    public record Pos(int x,int y,int z) {}
    public record Block(Pos pos,Material material,String data) {}
    public record Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
        public int width(){return maxX-minX+1;}
        public int height(){return maxY-minY+1;}
        public int depth(){return maxZ-minZ+1;}
    }
    private final Map<Pos,Block> cells=new HashMap<>();
    public void set(int x,int y,int z,Material material){
        Pos pos=new Pos(x,y,z);cells.put(pos,new Block(pos,material,null));
    }
    public void state(int x,int y,int z,Material material,String properties){
        Pos pos=new Pos(x,y,z);
        String data="minecraft:"+material.name().toLowerCase(Locale.ROOT)+"["+properties+"]";
        cells.put(pos,new Block(pos,material,data));
    }
    public void box(int x,int y,int z,int maxX,int maxY,int maxZ,Material material){
        for(int bx=Math.min(x,maxX);bx<=Math.max(x,maxX);bx++)
            for(int by=Math.min(y,maxY);by<=Math.max(y,maxY);by++)
                for(int bz=Math.min(z,maxZ);bz<=Math.max(z,maxZ);bz++)set(bx,by,bz,material);
    }
    public Material get(int x,int y,int z){Block block=cells.get(new Pos(x,y,z));return block==null?Material.AIR:block.material();}
    public int size(){return cells.size();}
    public Bounds bounds(){
        if(cells.isEmpty())return new Bounds(0,0,0,0,0,0);
        int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(Pos p:cells.keySet()){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
        return new Bounds(minX,minY,minZ,maxX,maxY,maxZ);
    }
    public Map<Material,Long> materialCounts(){
        Map<Material,Long> result=new EnumMap<>(Material.class);
        for(Block block:cells.values())result.merge(block.material(),1L,Long::sum);
        return Collections.unmodifiableMap(result);
    }
    public List<Block> ordered(){
        List<Block> result=new ArrayList<>(cells.values());
        result.sort(Comparator.comparingInt((Block block)->block.pos().x()>>4)
            .thenComparingInt(block->block.pos().z()>>4).thenComparingInt(block->block.pos().y()));
        return result;
    }
}
