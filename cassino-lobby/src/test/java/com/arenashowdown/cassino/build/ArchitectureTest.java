package com.arenashowdown.cassino.build;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTest {
    record Cell(int x,int y,int z) {}
    @Test void allEightArcadesAndBothWingsHaveWalkableRoutes() {
        var blocks=LobbyPlan.create(64).blocks();
        Map<Cell,Material> map=new HashMap<>();
        for(var op:blocks) map.put(new Cell(op.x(),op.y(),op.z()),op.material());
        Set<Cell> visited=new HashSet<>(); ArrayDeque<Cell> queue=new ArrayDeque<>();
        Cell start=new Cell(0,67,22);queue.add(start);visited.add(start);
        while(!queue.isEmpty()){
            Cell c=queue.remove();
            for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                Cell n=new Cell(c.x+d[0],67,c.z+d[1]);
                if(Math.abs(n.x)>90||Math.abs(n.z)>95||visited.contains(n))continue;
                Material ground=map.getOrDefault(new Cell(n.x,66,n.z),Material.AIR);
                Material feet=map.getOrDefault(n,Material.AIR);
                Material head=map.getOrDefault(new Cell(n.x,68,n.z),Material.AIR);
                if(ground!=Material.AIR&&feet==Material.AIR&&head==Material.AIR){visited.add(n);queue.add(n);}
            }
        }
        for(var p:LobbyPlan.PORTALS) assertTrue(visited.contains(new Cell(p.x(),67,p.z())),"Blocked portal: "+p.id());
        assertTrue(visited.contains(new Cell(-68,67,0)),"Hall of fame blocked");
        assertTrue(visited.contains(new Cell(68,67,0)),"Lounge blocked");
        long gold=map.values().stream().filter(m->m==Material.GOLD_BLOCK).count();
        assertTrue(gold<map.size()/100,"Gold must remain below 1% of blocks");
        assertTrue(blocks.stream().anyMatch(op->op.data()!=null),"Missing shaped blocks");
    }
}
