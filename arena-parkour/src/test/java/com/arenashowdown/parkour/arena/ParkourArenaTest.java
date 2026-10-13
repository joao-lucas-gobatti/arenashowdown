package com.arenashowdown.parkour.arena;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ParkourArenaTest {
    private final ArenaPoint point=new ArenaPoint("parkour_test",0.5,70,0.5,0,0);
    @Test void reportsIncompleteArena(){ParkourArena arena=new ParkourArena("test","parkour_test",null,List.of(),List.of(),null,null,0,50,Map.of());assertTrue(arena.validate().size()>=5);}
    @Test void acceptsCompleteArenaAndThreeDimensionalRegions(){BlockRegion region=BlockRegion.between("parkour_test",3,72,3,1,70,1);ParkourArena arena=new ParkourArena("test","parkour_test",point,List.of(point),List.of(region),region,point,300,50,Map.of());assertTrue(arena.validate().isEmpty());assertTrue(region.contains("parkour_test",2,71,2));assertFalse(region.contains("parkour_test",2,75,2));assertFalse(region.contains("world",2,71,2));}
}
