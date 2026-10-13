package com.arenashowdown.parkour.arena;

import java.util.*;
public record ParkourArena(String name,String world,ArenaPoint start,List<ArenaPoint>spawns,List<BlockRegion>checkpoints,BlockRegion finish,ArenaPoint lobby,long timeLimitSeconds,double fallHeight,Map<String,Object>rules) {
    public ParkourArena { spawns=List.copyOf(spawns);checkpoints=List.copyOf(checkpoints);rules=Map.copyOf(rules); }
    public List<String> validate(){List<String>e=new ArrayList<>();if(name==null||name.isBlank())e.add("nome ausente");if(world==null||world.isBlank())e.add("mundo ausente");if(start==null)e.add("largada ausente");if(spawns.isEmpty())e.add("nenhum spawn");if(checkpoints.isEmpty())e.add("nenhum checkpoint");if(finish==null)e.add("chegada ausente");if(lobby==null)e.add("lobby ausente");if(timeLimitSeconds<=0)e.add("tempo limite invalido");return e;}
}
