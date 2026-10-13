package com.arenashowdown.parkour.palace;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import static org.junit.jupiter.api.Assertions.*;
import static com.arenashowdown.parkour.palace.PalaceRoute.*;

class PalaceGeometryTest {
    @Test void twelveStagesHaveSupportedLandingsAndClearHeadroom(){
        PalaceBlocks b=PalaceArchitecture.build();int checks=0;
        for(Wing w:Wing.values()){
            assertTrue(PalaceRoute.arena(w).validate().isEmpty());
            var route=landings(w);
            assertEquals(22,route.size());
            for(var n:route){
                if(n.stage()>0&&n.checkpoint())checks++;
                for(int du=n.lo();du<=n.hi();du++)for(int dv=n.lo();dv<=n.hi();dv++){
                    int x=w.x(n.u()+du,n.v()+dv),z=w.z(n.u()+du,n.v()+dv);
                    assertNotEquals(Material.AIR,b.get(x,n.y(),z),w+" unsupported "+n.index());
                    for(int h=1;h<=3;h++)assertEquals(Material.AIR,b.get(x,n.y()+h,z),w+" obstructed "+n.index()+" at "+x+","+(n.y()+h)+","+z);
                }
                assertTrue(n.region(w).contains(WORLD,n.point(w).x(),n.point(w).y(),n.point(w).z()));
            }
            for(int i=1;i<route.size();i++){
                var from=route.get(i-1);var to=route.get(i);
                assertTrue(to.y()-from.y()<=1,"upward jump too high");
                // Conservative geometric envelope; NOT a physical client playtest.
                double forward=to.v()-from.v()-(from.width()+to.width())/2.0;
                double sideways=Math.max(0,Math.abs(to.u()-from.u())-(from.width()+to.width())/2.0);
                assertTrue(Math.hypot(forward,sideways)<=3.05,"gap beyond intended sprint envelope");
                assertTrue(to.v()>from.v());
                for(int v=from.v()+from.hi()+1;v<to.v()+to.lo();v++)
                    for(int u=-3;u<=3;u++)for(int y=Math.max(from.y(),to.y())+1;y<=Math.max(from.y(),to.y())+3;y++)
                        assertEquals(Material.AIR,b.get(w.x(u,v),y,w.z(u,v)),"jump head collision");
            }
        }
        assertEquals(12,checks);
        assertTrue(Wing.EASY.z(0,50)<0);assertTrue(Wing.MEDIUM.x(0,50)>0);
        assertTrue(Wing.HARD.x(0,50)<0);assertTrue(Wing.IMPOSSIBLE.z(0,50)>0);
    }
}
