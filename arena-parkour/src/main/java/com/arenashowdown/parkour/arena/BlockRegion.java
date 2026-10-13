package com.arenashowdown.parkour.arena;

public record BlockRegion(String world,int minX,int minY,int minZ,int maxX,int maxY,int maxZ) {
    public BlockRegion { if(minX>maxX||minY>maxY||minZ>maxZ)throw new IllegalArgumentException("Regiao invertida"); }
    public static BlockRegion between(String world,int x1,int y1,int z1,int x2,int y2,int z2){return new BlockRegion(world,Math.min(x1,x2),Math.min(y1,y2),Math.min(z1,z2),Math.max(x1,x2),Math.max(y1,y2),Math.max(z1,z2));}
    public boolean contains(String candidate,double x,double y,double z){return world.equals(candidate)&&x>=minX&&x<maxX+1&&y>=minY&&y<maxY+1&&z>=minZ&&z<maxZ+1;}
}
