package com.arenashowdown.parkour.wonder;

import java.util.*;
import java.util.stream.*;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

class WonderFullArchitectureTest {
    private static final WonderBlocks BLOCKS=WonderArchitecture.build();

    @Test void blueprintHasTwelveDistinctStagesAndExactGlobalIndexing(){
        List<Stage> stages=Arrays.stream(Wing.values()).flatMap(wing->WonderRoute.stages(wing).stream()).toList();
        List<Landing> all=stages.stream().flatMap(stage->stage.landings().stream()).toList();
        assertEquals(12,stages.size());assertEquals(516,all.size());assertEquals(504,stages.stream().mapToInt(Stage::transitions).sum());
        assertEquals(IntStream.range(0,516).boxed().toList(),all.stream().map(Landing::globalIndex).toList());
        assertEquals(36,all.stream().filter(node->node.transition()>0&&node.checkpoint()).count());
        for(Wing wing:Wing.values()){
            assertEquals(127,WonderRoute.route(wing).size(),wing+" route nodes");
            assertEquals(126,WonderRoute.arena(wing).checkpoints().size(),wing+" official transitions");
            assertEquals(9,WonderRoute.route(wing).stream().filter(Landing::checkpoint).count(),wing+" safe checkpoints");
        }
        for(Stage stage:stages){
            assertEquals(43,stage.landings().size(),stage.name());assertFalse(stage.name().isBlank());assertFalse(stage.layout().isBlank());assertFalse(stage.landmark().isBlank());
            assertEquals(Set.of(14,28,42),stage.landings().stream().filter(Landing::checkpoint).filter(n->n.transition()>0).map(Landing::transition).collect(Collectors.toSet()));
        }
        Set<String> signatures=stages.stream().map(WonderFullArchitectureTest::spatialSignature).collect(Collectors.toSet());
        assertEquals(12,signatures.size(),"every chapter needs its own spatial grammar");
    }

    @Test void obstacleVocabularyIsBroadAndSpecialMovementIsNotImplicit(){
        Set<Obstacle> total=EnumSet.noneOf(Obstacle.class);
        for(Wing wing:Wing.values()){
            Set<Obstacle> obstacles=WonderRoute.route(wing).stream().map(Landing::obstacle).collect(Collectors.toSet());
            assertTrue(obstacles.size()>=8,wing+" obstacle types="+obstacles);total.addAll(obstacles);
        }
        assertTrue(total.size()>=20,"total obstacle types="+total);
        assertFalse(total.contains(Obstacle.LADDER));assertFalse(total.contains(Obstacle.SLIME));assertFalse(total.contains(Obstacle.HONEY));
    }

    @Test void everyLandingIsSupportedClearAndInsideConservativePhysics(){
        Set<String> coordinates=new HashSet<>();int count=0;
        for(Wing wing:Wing.values())for(Stage stage:WonderRoute.stages(wing)){
            List<Landing> route=stage.landings();count+=route.size();
            for(int i=0;i<route.size();i++){
                Landing node=route.get(i);int x=wing.x(node.u(),node.v()),z=wing.z(node.u(),node.v());
                assertTrue(coordinates.add(wing+":"+node.globalIndex()+":"+x+":"+node.y()+":"+z));
                assertNotEquals(Material.AIR,BLOCKS.get(x,node.y(),z),stage.name()+" unsupported "+node.transition());
                for(int du=node.minOffset();du<=node.maxOffset();du++)for(int dv=node.minOffset();dv<=node.maxOffset();dv++){
                    int fx=wing.x(node.u()+du,node.v()+dv),fz=wing.z(node.u()+du,node.v()+dv);
                    assertNotEquals(Material.AIR,BLOCKS.get(fx,node.y(),fz),stage.name()+" footprint "+node.transition()+"/"+du+","+dv);
                    for(int dy=1;dy<=3;dy++)assertEquals(Material.AIR,BLOCKS.get(fx,node.y()+dy,fz),stage.name()+" footprint headroom "+node.transition()+"/"+du+","+dv+"/"+dy);
                }
                for(int dy=1;dy<=3;dy++)assertEquals(Material.AIR,BLOCKS.get(x,node.y()+dy,z),stage.name()+" headroom "+node.transition()+"/"+dy);
                if(i==0)continue;Landing previous=route.get(i-1);
                assertNotEquals(previous.u()+":"+previous.v()+":"+previous.y(),node.u()+":"+node.v()+":"+node.y(),stage.name()+" duplicate transition "+i);
                assertTrue(WonderRoute.edgeGap(previous,node)<=wing.maxEdgeGap+.001,stage.name()+" gap "+i);
                int rise=node.y()-previous.y();assertTrue(rise<=wing.maxRise&&rise>=-wing.maxDrop,stage.name()+" vertical delta "+i+"="+rise);
            }
        }
        assertEquals(516,count);
        Landing crown=WonderRoute.stage(Wing.IMPOSSIBLE,3).landings().get(42);assertTrue(crown.y()>=170&&crown.y()<=174,"crown finish="+crown.y());
    }

    @Test void completePlanFillsApprovedEnvelopeWithLayeredPalette(){
        WonderBlocks.Bounds bounds=BLOCKS.bounds();
        assertTrue(bounds.minX()>=-224&&bounds.maxX()<=224,bounds.toString());assertTrue(bounds.minZ()>=-224&&bounds.maxZ()<=224,bounds.toString());
        assertTrue(bounds.minY()>=58&&bounds.maxY()<=184,bounds.toString());
        assertTrue(bounds.width()>=400,"width="+bounds.width());assertTrue(bounds.depth()>=400,"depth="+bounds.depth());assertTrue(bounds.height()>=110,"height="+bounds.height());
        Map<Material,Long> materials=BLOCKS.materialCounts();assertTrue(materials.size()>=50,"materials="+materials.size());assertTrue(BLOCKS.size()>100_000,"cells="+BLOCKS.size());
        assertMaterial(materials,Material.GOLD_BLOCK,400);assertMaterial(materials,Material.CUT_COPPER,300);assertMaterial(materials,Material.AMETHYST_BLOCK,150);
        assertMaterial(materials,Material.RED_NETHER_BRICKS,500);assertMaterial(materials,Material.PURPUR_BLOCK,500);assertMaterial(materials,Material.CYAN_CONCRETE,300);
        assertMaterial(materials,Material.PINK_TERRACOTTA,300);assertMaterial(materials,Material.QUARTZ_BLOCK,300);assertMaterial(materials,Material.SEA_LANTERN,250);
        System.out.printf(Locale.ROOT,"WONDER_METRICS cells=%d materials=%d bounds=%dx%dx%d (%d,%d,%d)..(%d,%d,%d)%n",BLOCKS.size(),materials.size(),bounds.width(),bounds.height(),bounds.depth(),bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ());
    }

    @Test void representativeLandmarksAreVoxelCompositions(){
        assertSolid(Wing.EASY,0,1,65,"confetti fountain");assertSolid(Wing.EASY,-27,22,145,"toy clock");assertSolid(Wing.EASY,0,76,184,"ballroom chandelier");
        assertSolid(Wing.MEDIUM,-18,13,75,"card castle");assertSolid(Wing.MEDIUM,0,35,128,"sky clock");assertSolid(Wing.MEDIUM,0,18,185,"mirror prism");
        assertSolid(Wing.HARD,0,2,69,"broken stage");assertSolid(Wing.HARD,0,35,128,"roulette machine");assertSolid(Wing.HARD,0,2,184,"amphitheatre");
        assertSolid(Wing.IMPOSSIBLE,-9,33,71,"fractured die");assertSolid(Wing.IMPOSSIBLE,0,18,132,"infinite prism");assertSolid(Wing.IMPOSSIBLE,28,86,190,"crown");
    }

    @Test void reportsImportantCoordinates(){
        for(Wing wing:Wing.values())for(Stage stage:WonderRoute.stages(wing)){
            List<Landing> nodes=stage.landings();
            for(int transition:new int[]{0,14,28,42}){
                Landing node=nodes.get(transition);
                System.out.printf(Locale.ROOT,"POINT wing=%s stage=%d transition=%d type=%s x=%d y=%d z=%d%n",wing,stage.number(),transition,transition==0?"START":transition==42?"FINISH":"CHECKPOINT",wing.x(node.u(),node.v()),node.y()+1,wing.z(node.u(),node.v()));
            }
        }
    }

    private static void assertSolid(Wing wing,int u,int h,int v,String landmark){assertNotEquals(Material.AIR,BLOCKS.get(wing.x(u,v),FLOOR+h,wing.z(u,v)),landmark);}
    private static void assertMaterial(Map<Material,Long> materials,Material material,long minimum){assertTrue(materials.getOrDefault(material,0L)>=minimum,material+"="+materials.getOrDefault(material,0L));}
    private static String spatialSignature(Stage stage){
        List<Landing> nodes=stage.landings();StringBuilder result=new StringBuilder(stage.layout()).append('|');
        int minU=99,maxU=-99,minV=999,maxV=-999,minY=999,maxY=-999,turns=0,lastDu=9,lastDv=9;
        for(int i=0;i<nodes.size();i++){Landing n=nodes.get(i);minU=Math.min(minU,n.u());maxU=Math.max(maxU,n.u());minV=Math.min(minV,n.v());maxV=Math.max(maxV,n.v());minY=Math.min(minY,n.y());maxY=Math.max(maxY,n.y());if(i>0){Landing p=nodes.get(i-1);int du=Integer.compare(n.u(),p.u()),dv=Integer.compare(n.v(),p.v());if(i>1&&(du!=lastDu||dv!=lastDv))turns++;lastDu=du;lastDv=dv;}}
        return result.append(maxU-minU).append(':').append(maxV-minV).append(':').append(maxY-minY).append(':').append(turns).toString();
    }
}
