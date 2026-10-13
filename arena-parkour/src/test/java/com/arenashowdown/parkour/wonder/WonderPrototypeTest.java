package com.arenashowdown.parkour.wonder;

import java.util.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

class WonderPrototypeTest {
    @Test void confettiGardenIsLongVariedAndPhysicallyConservative(){
        List<Landing> route=WonderRoute.prototypeGarden();
        assertEquals(TRANSITIONS_PER_STAGE+1,route.size());
        assertEquals(3,route.stream().skip(1).filter(Landing::checkpoint).count());
        assertEquals(3,route.stream().skip(1).map(Landing::beat).distinct().count());
        assertTrue(route.stream().map(Landing::obstacle).distinct().count()>=7);
        int turns=0;int lastDu=0,lastDv=0;
        for(int i=1;i<route.size();i++){
            Landing from=route.get(i-1),to=route.get(i);
            assertTrue(WonderRoute.edgeGap(from,to)<=2.55,"gap at transition "+i);
            assertTrue(to.y()-from.y()<=1,"rise at transition "+i);
            assertTrue(to.y()-from.y()>=-2,"drop at transition "+i);
            int du=Integer.compare(to.u(),from.u()),dv=Integer.compare(to.v(),from.v());
            if(i>1&&(du!=lastDu||dv!=lastDv))turns++;
            lastDu=du;lastDv=dv;
        }
        assertTrue(turns>=8,"route needs frequent direction changes");
    }

    @Test void prototypeHasSupportedClearLandingsAndArchitecturalDensity(){
        WonderBlocks blocks=WonderArchitecture.buildPrototype();
        for(Landing node:WonderRoute.prototypeGarden()){
            int x=Wing.EASY.x(node.u(),node.v()),z=Wing.EASY.z(node.u(),node.v());
            assertNotEquals(Material.AIR,blocks.get(x,node.y(),z),"unsupported landing "+node.transition());
            for(int dy=1;dy<=3;dy++)assertEquals(Material.AIR,blocks.get(x,node.y()+dy,z),"headroom "+node.transition()+"/"+dy);
        }
        WonderBlocks.Bounds bounds=blocks.bounds();
        assertTrue(bounds.width()>=77,"atrium width");
        assertTrue(bounds.depth()>=130,"atrium + chapter depth");
        assertTrue(bounds.height()>=75,"monumental vertical scale");
        assertTrue(blocks.size()>30_000,"prototype voxel density: "+blocks.size());
        Map<Material,Long> palette=blocks.materialCounts();
        assertTrue(palette.size()>=30,"material layering");
        assertTrue(palette.containsKey(Material.CALCITE));
        assertTrue(palette.containsKey(Material.PRISMARINE_BRICKS));
        assertTrue(palette.containsKey(Material.PINK_TERRACOTTA));
        assertTrue(palette.containsKey(Material.LIGHT_BLUE_TERRACOTTA));
        assertTrue(palette.containsKey(Material.WAXED_OXIDIZED_COPPER));
    }
}
