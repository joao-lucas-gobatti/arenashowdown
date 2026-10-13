package com.arenashowdown.parkour.palace;

import com.arenashowdown.parkour.arena.*;
import java.util.*;

/** Shared geometry for generation, training and championship checkpoint regions. */
public final class PalaceRoute {
    public static final String WORLD="grand_dice_palace";
    public static final int FLOOR=70;
    public enum Wing {
        EASY("THE GOLDEN HALL",0,-1,3), MEDIUM("THE FLOATING CARDS",1,0,2),
        HARD("THE CRIMSON THEATRE",-1,0,1), IMPOSSIBLE("THE SHADOW PALACE",0,1,1);
        public final String title; public final int dx,dz,width;
        Wing(String title,int dx,int dz,int width){this.title=title;this.dx=dx;this.dz=dz;this.width=width;}
        public int x(int u,int v){return -dz*u+dx*v;}
        public int z(int u,int v){return dx*u+dz*v;}
        public float yaw(){return dx==1?-90:dx==-1?90:dz==-1?180:0;}
        public String id(){return "palace-"+name().toLowerCase(Locale.ROOT);}
    }
    public record Landing(int index,int stage,int u,int v,int y,int width,boolean checkpoint) {
        public int lo(){return -(width/2);}
        public int hi(){return lo()+width-1;}
        public ArenaPoint point(Wing w){return new ArenaPoint(WORLD,w.x(u,v)+.5,y+1,w.z(u,v)+.5,w.yaw(),0);}
        public BlockRegion region(Wing w){
            return BlockRegion.between(WORLD,w.x(u+lo(),v+lo()),y+1,w.z(u+lo(),v+lo()),w.x(u+hi(),v+hi()),y+1,w.z(u+hi(),v+hi()));
        }
    }
    private PalaceRoute(){}
    public static List<Landing> landings(Wing wing){
        List<Landing> nodes=new ArrayList<>();
        nodes.add(new Landing(0,0,0,30,FLOOR,5,true));
        for(int stage=0;stage<3;stage++)for(int step=0;step<7;step++){
            int index=stage*7+step+1;
            int u=switch(wing){case EASY->0;case MEDIUM->step%3-1;case HARD->new int[]{0,1,0,-1,0,1,0}[step];case IMPOSSIBLE->step%2;};
            int y=FLOOR+stage*2+(step>=3?1:0)+(step==6?1:0);
            if(wing==Wing.IMPOSSIBLE)y=FLOOR+stage*2+(step==6?2:step%2);
            nodes.add(new Landing(index,stage+1,u,34+stage*28+step*4,y,wing.width,step==6));
        }
        return List.copyOf(nodes);
    }
    public static ParkourArena arena(Wing wing){
        List<Landing> nodes=landings(wing); ArenaPoint start=nodes.getFirst().point(wing);
        List<ArenaPoint> spawns=new ArrayList<>();
        for(int u=-1;u<=1;u++)for(int v=29;v<=31;v++)spawns.add(new ArenaPoint(WORLD,wing.x(u,v)+.5,71,wing.z(u,v)+.5,wing.yaw(),0));
        return new ParkourArena(wing.id(),WORLD,start,spawns,nodes.subList(1,nodes.size()).stream().map(n->n.region(wing)).toList(),nodes.getLast().region(wing),new ArenaPoint(WORLD,.5,71,.5,180,0),900,68,Map.of("palace",true,"difficulty",wing.name(),"stages",3));
    }
}
