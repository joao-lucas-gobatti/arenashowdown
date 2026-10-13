package com.arenashowdown.parkour.wonder;

import org.bukkit.Material;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

/** Authored architectural composition. Prototype contains the complete atrium and Easy chapter one. */
public final class WonderArchitecture {
    private final WonderBlocks blocks=new WonderBlocks();private final WonderComponents c=new WonderComponents(blocks);
    private WonderArchitecture(){}
    public static WonderBlocks buildPrototype(){WonderArchitecture a=new WonderArchitecture();a.atrium();a.easyPortal();a.confettiGarden();return a.blocks;}
    /** Complete static plan: atrium, four portals, twelve authored chapters and all gameplay landings. */
    public static WonderBlocks build(){
        WonderArchitecture a=new WonderArchitecture();a.atrium();a.easyPortal();a.completePortals();
        a.confettiGarden();a.giantToyRoom();a.rainbowBallroom();
        a.floatingCardKingdom();a.skyClockwork();a.celestialMirrorHall();
        a.brokenTheatre();a.mechanicalWonder();a.grandShowdown();
        a.fracturedReality();a.infinitePalace();a.crownOfDice();
        a.connectChapters();a.materializeAllRoutes();return a.blocks;
    }

    private void atrium(){
        c.wing(Wing.EASY);
        // Radial champagne/teal/coral rosette; deliberately not a checkerboard.
        for(int u=-38;u<=38;u++)for(int v=-38;v<=38;v++){
            double d=Math.hypot(u,v);if(d>38)continue;
            double angle=(Math.atan2(v,u)+Math.PI)/(Math.PI/8);int sector=(int)Math.floor(angle);
            Material floor=d<10?(sector%2==0?Material.CALCITE:Material.EXPOSED_COPPER)
                :d<24?(sector%3==0?Material.PRISMARINE_BRICKS:Material.SMOOTH_SANDSTONE)
                :(sector%2==0?Material.TUFF_BRICKS:Material.CALCITE);
            c.p(u,0,v,floor);if(d>36.5)c.box(u,-5,v,u,-1,v,Material.TUFF_BRICKS);
            // Layered faceted dome with stained-glass ribs.
            int roof=76-(int)Math.round(22*Math.pow(d/38.0,1.7));
            if(d>8)c.p(u,roof,v,(sector%4==0)?Material.CYAN_TERRACOTTA:Material.CALCITE);
            if(((int)Math.round(d))%9==0&&sector%2==0)c.p(u,roof-1,v,Material.EXPOSED_COPPER);
        }
        // Deep walls, fluted columns and asymmetric mezzanines.
        for(int angle=0;angle<360;angle+=15){double a=Math.toRadians(angle);int u=(int)Math.round(37*Math.cos(a)),v=(int)Math.round(37*Math.sin(a));
            for(int h=1;h<=52;h++)c.p(u,h,v,h%11==0?Material.EXPOSED_COPPER:Material.TUFF_BRICKS);
        }
        for(int angle=22;angle<360;angle+=45){double a=Math.toRadians(angle);int u=(int)Math.round(31*Math.cos(a)),v=(int)Math.round(31*Math.sin(a));
            c.box(u-2,0,v-2,u+2,3,v+2,Material.SMOOTH_SANDSTONE);c.box(u-1,4,v-1,u+1,34,v+1,Material.STRIPPED_WARPED_STEM);
            for(int h=7;h<34;h+=6)c.ring(u,h,v,2,Material.EXPOSED_COPPER);c.disc(u,35,v,3,Material.CALCITE);
        }
        for(int s:new int[]{-1,1}){
            c.ornamentalBridge(s*17,14,-26,s*29,-8,Material.DARK_PRISMARINE,Material.EXPOSED_COPPER);
            c.curvedStaircase(s*24,-2,1,10,26,Material.SMOOTH_SANDSTONE,Material.CUT_COPPER);
            c.tieredFountain(s*21,1,18,6,Material.EXPOSED_COPPER,Material.LIGHT_BLUE_STAINED_GLASS,Material.PRISMARINE);
            c.fantasyTree(s*25,1,-18,10,Material.STRIPPED_CHERRY_WOOD,Material.FLOWERING_AZALEA_LEAVES,Material.PINK_PETALS);
        }
        // Suspended gardens and four balcony silhouettes.
        for(int q=0;q<4;q++){int su=q<2?-1:1,sv=(q%2==0)?-1:1;c.disc(su*24,17,sv*23,7,Material.MOSS_BLOCK);c.ring(su*24,18,sv*23,7,Material.EXPOSED_COPPER);
            for(int h=19;h<27;h+=3)c.p(su*24,h,sv*23,Material.IRON_CHAIN);c.monumentalFlower(su*24,20,sv*23,4,q%2==0?Material.PINK_CONCRETE_POWDER:Material.LIGHT_BLUE_CONCRETE_POWDER,Material.YELLOW_TERRACOTTA);
        }
        c.orbitalDice(0,27,0,6);c.monumentalChandelier(0,72,0);
        // Coral/menta fountain below the floating die.
        c.tieredFountain(0,1,0,10,Material.CUT_COPPER,Material.LIGHT_BLUE_STAINED_GLASS,Material.PRISMARINE_BRICKS);
        for(int angle=0;angle<360;angle+=45){double a=Math.toRadians(angle);int u=(int)Math.round(13*Math.cos(a)),v=(int)Math.round(13*Math.sin(a));c.p(u,2,v,angle%90==0?Material.PINK_GLAZED_TERRACOTTA:Material.WAXED_OXIDIZED_COPPER);}
        atriumWindows();otherPortalSilhouettes();
    }
    private void atriumWindows(){
        // Four groups of deep, lit art-deco glazing between portals.
        for(Wing wing:Wing.values()){c.wing(wing);for(int side:new int[]{-1,1})for(int offset:new int[]{-14,14}){
            int baseV=29+Math.abs(offset)/7;c.box(side*18,7,baseV+offset/4,side*18,31,baseV+offset/4,Material.CALCITE);
            for(int h=9;h<=29;h++)for(int d=-3;d<=3;d++){boolean rim=Math.abs(d)==3||h==9||h==29;c.p(side*19,h,baseV+d,rim?Material.EXPOSED_COPPER:(h+d)%4==0?Material.PINK_STAINED_GLASS:Material.CYAN_STAINED_GLASS);c.p(side*20,h,baseV+d,Material.SEA_LANTERN);}
        }}
    }
    private void otherPortalSilhouettes(){
        c.wing(Wing.MEDIUM);c.artDecoArch(38,11,24,Material.QUARTZ_BRICKS,Material.LIGHT_BLUE_GLAZED_TERRACOTTA);
        for(int s:new int[]{-1,1})for(int h=4;h<22;h++)c.p(s*(8+h/6),h,38,Material.WHITE_TERRACOTTA);
        c.wing(Wing.HARD);c.artDecoArch(38,12,22,Material.DEEPSLATE_BRICKS,Material.CUT_COPPER);
        for(int r:new int[]{5,9})c.verticalRing(0,12,39,r,r==5?Material.COPPER_BLOCK:Material.DARK_OAK_WOOD);
        c.wing(Wing.IMPOSSIBLE);for(int r:new int[]{8,12,16}){c.verticalRing(0,15,38,r,r==12?Material.AMETHYST_BLOCK:Material.PURPUR_PILLAR);}
        for(int h=2;h<31;h+=4){c.p(-17+h/2,h,38,Material.SEA_LANTERN);c.p(17-h/2,h,38,Material.END_ROD);}
    }
    private void easyPortal(){
        c.wing(Wing.EASY);
        // Floral gate: rounded canopy, giant chips and ribbon stairs, unlike the other portals.
        c.artDecoArch(38,13,26,Material.SMOOTH_SANDSTONE,Material.WAXED_OXIDIZED_COPPER);
        for(int s:new int[]{-1,1}){
            c.monumentalFlower(s*13,8,39,4,s<0?Material.PINK_CONCRETE_POWDER:Material.LIGHT_BLUE_CONCRETE_POWDER,Material.YELLOW_GLAZED_TERRACOTTA);
            for(int h=4;h<=18;h+=4){c.disc(s*17,h,40,4,h%8==0?Material.PINK_TERRACOTTA:Material.WAXED_OXIDIZED_CUT_COPPER);c.ring(s*17,h,40,4,Material.CALCITE);}
        }
        c.lanternArc(0,18,40,13,Material.EXPOSED_COPPER,Material.VERDANT_FROGLIGHT);
        c.ornamentalBridge(-4,1,40,0,48,Material.SMOOTH_SANDSTONE,Material.WAXED_OXIDIZED_COPPER);
    }
    private void confettiGarden(){
        c.wing(Wing.EASY);
        // Three nested garden rooms, each with a different skyline and focal composition.
        for(int v=44;v<=96;v++)for(int u=-43;u<=43;u++){
            if(Math.abs(u)>40){c.p(u,0,v,Material.MOSSY_STONE_BRICKS);if(v%5==0)for(int h=1;h<=24+(v%11);h++)c.p(u,h,v,h%8==0?Material.WAXED_OXIDIZED_COPPER:Material.SMOOTH_SANDSTONE);}
            if(Math.abs(u)>=37&&v%9<6)for(int h=5;h<29;h++)c.p(u,h,v,(h+v)%7==0?Material.YELLOW_STAINED_GLASS:Material.LIGHT_BLUE_STAINED_GLASS);
            // Voxel greenhouse ribs; open between ribs to avoid flat walls.
            if(v%8==0){int roof=42-(int)Math.round(10*Math.pow(Math.abs(u)/43.0,1.5));c.p(u,roof,v,Material.WAXED_OXIDIZED_COPPER);if(Math.abs(u)%7<5)c.p(u,roof+1,v,Material.WHITE_STAINED_GLASS);}
        }
        for(int v=46;v<=94;v+=12){c.lanternArc(0,25,v,34,Material.WAXED_OXIDIZED_COPPER,Material.VERDANT_FROGLIGHT);}
        // Beat 1: chip orchard and bridge fountain.
        for(int s:new int[]{-1,1}){
            c.fantasyTree(s*28,1,55,9,Material.STRIPPED_CHERRY_WOOD,Material.AZALEA_LEAVES,Material.PINK_PETALS);
            c.monumentalFlower(s*34,7,67,5,s<0?Material.PINK_CONCRETE_POWDER:Material.LIGHT_BLUE_CONCRETE_POWDER,Material.YELLOW_TERRACOTTA);
            c.curvedStaircase(s*30,76,1,8,20,Material.CUT_SANDSTONE,Material.WAXED_OXIDIZED_COPPER);
        }
        c.tieredFountain(0,1,65,9,Material.WAXED_OXIDIZED_COPPER,Material.LIGHT_BLUE_STAINED_GLASS,Material.YELLOW_TERRACOTTA);
        c.ornamentalBridge(-35,3,73,-21,87,Material.CHERRY_PLANKS,Material.CUT_COPPER);
        c.ornamentalBridge(35,3,73,21,87,Material.BAMBOO_MOSAIC,Material.CUT_COPPER);
        // Beat 2/3: suspended chip constellation and original smiling clock-flower.
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9;int u=(int)Math.round(28*Math.cos(a)),v=82+(int)Math.round(10*Math.sin(a));int h=17+i%3*3;c.disc(u,h,v,4,i%3==0?Material.PINK_TERRACOTTA:i%3==1?Material.LIGHT_BLUE_TERRACOTTA:Material.YELLOW_TERRACOTTA);c.ring(u,h,v,4,Material.CALCITE);c.box(u,h+1,v,u,34,v,Material.IRON_CHAIN);}
        c.verticalRing(0,18,90,12,Material.EXPOSED_COPPER);c.verticalRing(0,18,89,9,Material.YELLOW_TERRACOTTA);
        for(int hour=0;hour<12;hour++){double a=Math.toRadians(hour*30);c.p((int)Math.round(10*Math.cos(a)),18+(int)Math.round(10*Math.sin(a)),88,Material.SEA_LANTERN);}
        c.p(-3,21,87,Material.PRISMARINE);c.p(3,21,87,Material.PRISMARINE);for(int u=-4;u<=4;u++)c.p(u,14+Math.abs(u)/3,87,Material.CALCITE);
        buildGardenRoute();
    }
    private void buildGardenRoute(){
        Material[] decks={Material.PINK_TERRACOTTA,Material.WAXED_OXIDIZED_CUT_COPPER,Material.YELLOW_TERRACOTTA,Material.LIGHT_BLUE_TERRACOTTA,Material.SMOOTH_SANDSTONE};
        for(Landing node:WonderRoute.prototypeGarden()){
            int h=node.y()-FLOOR,r=node.radius();
            // Reserve only the player's body volume; scenery remains tightly integrated around it.
            c.box(node.u()-r,h+1,node.v()-r,node.u()+r,h+3,node.v()+r,Material.AIR);
            Material deck=decks[node.transition()%decks.length];
            switch(node.obstacle()){
                case CHIP,START -> {c.disc(node.u(),h,node.v(),Math.max(2,r),deck);c.ring(node.u(),h,node.v(),Math.max(2,r),Material.CALCITE);}
                case ORNAMENTAL_BRIDGE -> {c.box(node.u()-r,h,node.v()-1,node.u()+r,h,node.v()+1,Material.CHERRY_PLANKS);for(int u=node.u()-r;u<=node.u()+r;u+=2)c.p(u,h-1,node.v(),Material.WAXED_OXIDIZED_COPPER);}
                case FLOWER_CROWN -> {c.disc(node.u(),h,node.v(),Math.max(2,r),Material.YELLOW_TERRACOTTA);for(int a=0;a<8;a++){double angle=a*Math.PI/4;c.p(node.u()+(int)Math.round((r+1)*Math.cos(angle)),h,node.v()+(int)Math.round((r+1)*Math.sin(angle)),deck);}}
                case FOUNTAIN_RIM -> {c.ring(node.u(),h,node.v(),Math.max(2,r+1),Material.CALCITE);c.disc(node.u(),h-1,node.v(),Math.max(1,r),Material.LIGHT_BLUE_STAINED_GLASS);c.disc(node.u(),h,node.v(),r,deck);}
                case CURVED_STEP -> {c.disc(node.u(),h,node.v(),Math.max(1,r),Material.BAMBOO_MOSAIC);c.ring(node.u(),h-1,node.v(),Math.max(2,r+1),Material.WAXED_OXIDIZED_COPPER);}
                case DICE_LEDGE -> {c.box(node.u()-r,h,node.v()-r,node.u()+r,h,node.v()+r,Material.CALCITE);for(int a=-r;a<=r;a+=Math.max(1,r*2))c.p(node.u()+a,h,node.v()-r-1,Material.PRISMARINE);}
                default -> c.disc(node.u(),h,node.v(),Math.max(1,r),deck);
            }
            // Supports belong to the garden objects instead of identical posts.
            if(node.transition()%4==0){for(int y=1;y<h;y++)c.p(node.u(),y,node.v(),Material.BAMBOO_BLOCK);}
            else if(node.transition()%4==1){for(int y=1;y<h;y++)c.p(node.u(),y,node.v(),Material.STRIPPED_CHERRY_WOOD);}
            else {c.ring(node.u(),Math.max(1,h-2),node.v(),Math.max(2,r+1),Material.FLOWERING_AZALEA_LEAVES);c.p(node.u(),h-1,node.v(),Material.SEA_LANTERN);}
            if(node.checkpoint()){c.ring(node.u(),h+1,node.v(),r+2,Material.VERDANT_FROGLIGHT);c.p(node.u(),h-1,node.v(),Material.GOLD_BLOCK);}
        }
    }
    private void completePortals(){
        c.wing(Wing.MEDIUM);c.cardCastle(0,3,41);c.cloudIsland(-17,6,43,4,Material.LIGHT_BLUE_TERRACOTTA,Material.WHITE_WOOL);
        c.wing(Wing.HARD);c.theatreCurtain(0,2,40,14,22,Material.RED_NETHER_BRICKS,Material.GOLD_BLOCK);c.gear(0,15,39,10,Material.CUT_COPPER,Material.DEEPSLATE_TILES);
        c.wing(Wing.IMPOSSIBLE);c.fracturedRings(0,16,39,16,Material.AMETHYST_BLOCK,Material.CYAN_CONCRETE);c.crystalCluster(-17,1,42,4,Material.PURPLE_STAINED_GLASS,Material.OBSIDIAN);
    }
    private void giantToyRoom(){
        c.wing(Wing.EASY);c.box(-43,-6,100,-39,0,154,Material.TERRACOTTA);c.box(39,-6,100,43,0,154,Material.SMOOTH_SANDSTONE);
        for(int v=102;v<=152;v+=8){c.lanternArc(0,27+(v%3),v,38,Material.YELLOW_TERRACOTTA,Material.PEARLESCENT_FROGLIGHT);for(int u:new int[]{-36,36})c.chamferedDice(u,5+(v%16),v,3,Material.PINK_TERRACOTTA,Material.CALCITE,Material.LIGHT_BLUE_CONCRETE);}
        c.toyDiceTower(0,7,127,Material.SMOOTH_SANDSTONE,Material.PINK_TERRACOTTA,Material.PRISMARINE);
        c.clockFace(-27,22,145,10,Material.YELLOW_TERRACOTTA,Material.CUT_COPPER,Material.LIGHT_BLUE_CONCRETE);
        for(int i=0;i<5;i++){c.gear(24-i*3,12+i*7,110+i*8,5+i%2,Material.WAXED_CUT_COPPER,Material.BLUE_TERRACOTTA);c.monumentalFlower(-31+i*3,5,108+i*10,3,Material.PINK_CONCRETE_POWDER,Material.YELLOW_GLAZED_TERRACOTTA);}
    }
    private void rainbowBallroom(){
        c.wing(Wing.EASY);c.box(-43,-4,156,-40,2,210,Material.PINK_TERRACOTTA);c.box(40,-4,156,43,2,210,Material.LIGHT_BLUE_TERRACOTTA);
        for(int v=158;v<=208;v+=10){c.artDecoArch(v,38,38+(v%20)/2,Material.CALCITE,v%20==8?Material.PINK_STAINED_GLASS:Material.LIGHT_BLUE_STAINED_GLASS);if(v%20==8)c.lanternArc(0,30,v,30,Material.WAXED_OXIDIZED_COPPER,Material.OCHRE_FROGLIGHT);}
        c.theatreCurtain(-27,5,178,9,27,Material.PINK_WOOL,Material.GOLD_BLOCK);c.theatreCurtain(27,5,190,9,27,Material.LIGHT_BLUE_WOOL,Material.CUT_COPPER);
        c.theatreBalcony(0,18,174,32,Material.BAMBOO_MOSAIC,Material.WAXED_OXIDIZED_COPPER);c.theatreBalcony(0,28,198,28,Material.CHERRY_PLANKS,Material.GOLD_BLOCK);
        c.monumentalChandelier(0,76,184);for(int s:new int[]{-1,1})c.curvedStaircase(s*29,202,2,9,28,Material.SMOOTH_SANDSTONE,Material.PINK_GLAZED_TERRACOTTA);
    }
    private void floatingCardKingdom(){
        c.wing(Wing.MEDIUM);c.box(-44,-5,46,-41,0,98,Material.LIGHT_BLUE_TERRACOTTA);c.box(41,-5,46,44,0,98,Material.WHITE_TERRACOTTA);
        for(int i=0;i<7;i++){int u=-33+i*11,v=50+(i%3)*17,h=7+(i%4)*7;c.cloudIsland(u,h,v,5,Material.MOSS_BLOCK,Material.WHITE_WOOL);c.monumentalCard(u,h+3,v-3,3,10,Material.QUARTZ_BLOCK,Material.GOLD_BLOCK,i%2==0?Material.CYAN_TERRACOTTA:Material.PURPLE_TERRACOTTA);}
        c.cardCastle(0,13,75);for(int v=48;v<=96;v+=12)c.lanternArc(0,31,v,38,Material.QUARTZ_BRICKS,Material.SEA_LANTERN);
        c.crystalCluster(-30,2,88,5,Material.LIGHT_BLUE_STAINED_GLASS,Material.PRISMARINE_BRICKS);c.crystalCluster(31,4,57,4,Material.PURPLE_STAINED_GLASS,Material.CALCITE);
    }
    private void skyClockwork(){
        c.wing(Wing.MEDIUM);for(int v=100;v<=154;v+=9){c.artDecoArch(v,40,42,Material.PRISMARINE_BRICKS,Material.YELLOW_STAINED_GLASS);for(int u:new int[]{-39,39})for(int h=2;h<34+(v%4);h++)if(h%5<3)c.p(u,h,v,h%10==0?Material.GOLD_BLOCK:Material.CYAN_TERRACOTTA);}
        c.clockFace(0,35,128,20,Material.LIGHT_BLUE_TERRACOTTA,Material.GOLD_BLOCK,Material.DARK_PRISMARINE);
        int[][] gears={{-27,19,111,8},{25,18,113,7},{-29,15,139,6},{27,29,144,9},{-18,48,148,5},{19,51,106,6},{0,61,151,8}};
        for(int[] g:gears)c.gear(g[0],g[1],g[2],g[3],Material.EXPOSED_COPPER,Material.PRISMARINE_BRICKS);
        c.pipeNetwork(-35,5,105,24,Material.WAXED_OXIDIZED_COPPER,Material.GOLD_BLOCK);c.cloudIsland(32,12,150,6,Material.CALCITE,Material.WHITE_WOOL);
    }
    private void celestialMirrorHall(){
        c.wing(Wing.MEDIUM);for(int v=158;v<=210;v+=7){int lean=(v/7)%2==0?4:-4;for(int h=1;h<=48;h++){c.p(-41+lean*h/48,h,v,h%8==0?Material.GOLD_BLOCK:Material.QUARTZ_PILLAR);c.p(41-lean*h/48,h,v,h%8==0?Material.AMETHYST_BLOCK:Material.LIGHT_BLUE_STAINED_GLASS);}if(v%14==4)c.fracturedRings(0,28,v,22,Material.QUARTZ_BLOCK,Material.PURPLE_STAINED_GLASS);}
        c.mirrorPrism(0,8,185,12,54,Material.AMETHYST_BLOCK,Material.TINTED_GLASS);
        for(int s:new int[]{-1,1}){c.crystalCluster(s*29,2,170,7,Material.LIGHT_BLUE_STAINED_GLASS,Material.CALCITE);c.crystalCluster(s*20,7,202,6,Material.PURPLE_STAINED_GLASS,Material.SMOOTH_QUARTZ);c.palaceArches(s*3,1,162,7,Material.QUARTZ_PILLAR,Material.GOLD_BLOCK);}
    }
    private void brokenTheatre(){
        c.wing(Wing.HARD);c.box(-44,-6,46,-40,1,98,Material.DEEPSLATE_BRICKS);c.box(40,-6,46,44,1,98,Material.RED_NETHER_BRICKS);
        c.theatreStage(0,2,69,20,Material.DARK_OAK_PLANKS,Material.CUT_COPPER);c.theatreCurtain(0,2,91,35,38,Material.RED_NETHER_BRICKS,Material.GOLD_BLOCK);
        c.theatreBalcony(-10,15,51,28,Material.POLISHED_DEEPSLATE,Material.CUT_COPPER);c.theatreBalcony(12,27,83,25,Material.DARK_OAK_PLANKS,Material.GOLD_BLOCK);
        for(int i=0;i<7;i++){int u=-32+i*10;c.ornamentalBridge(u,8+i%3*6,48+i*7,u+(i%2==0?13:-13),58+i*6,Material.DARK_OAK_WOOD,Material.IRON_CHAIN);c.p(u,35,50+i*7,Material.OCHRE_FROGLIGHT);}
        c.gear(-31,26,94,8,Material.CUT_COPPER,Material.DEEPSLATE_TILES);c.clockFace(29,24,95,9,Material.YELLOW_TERRACOTTA,Material.COPPER_BLOCK,Material.BLACKSTONE);
    }
    private void mechanicalWonder(){
        c.wing(Wing.HARD);for(int v=101;v<=154;v+=6){for(int h=1;h<50;h++)for(int u:new int[]{-42,42})if((h+v)%5!=0)c.p(u,h,v,h%9==0?Material.REDSTONE_LAMP:Material.OXIDIZED_COPPER);if(v%12==5)c.lanternArc(0,37,v,38,Material.COPPER_GRATE,Material.OCHRE_FROGLIGHT);}
        c.mechanicalWheel(0,35,128,20,Material.WAXED_OXIDIZED_COPPER,Material.RED_NETHER_BRICKS);
        c.pipeNetwork(-31,3,108,38,Material.CUT_COPPER,Material.GOLD_BLOCK);c.pipeNetwork(30,6,145,31,Material.OXIDIZED_COPPER,Material.REDSTONE_LAMP);
        for(int i=0;i<5;i++){c.gear(-26+i*13,13+i*8,111+i*7,5+i,Material.CUT_COPPER,Material.DEEPSLATE_TILES);c.chamferedDice(34-i*13,7+i*6,105+i*10,3,Material.CRIMSON_HYPHAE,Material.GOLD_BLOCK,Material.SEA_LANTERN);}
    }
    private void grandShowdown(){
        c.wing(Wing.HARD);for(int v=158;v<=212;v+=9){int half=42-(v-158)/9;for(int h=0;h<44;h++)for(int u:new int[]{-half,half})c.p(u,h,v,h%7==0?Material.GOLD_BLOCK:Material.RED_NETHER_BRICKS);c.artDecoArch(v,half,36+(v%5),Material.POLISHED_BLACKSTONE_BRICKS,Material.CUT_COPPER);}
        c.amphitheatre(0,2,184,37,Material.CRIMSON_PLANKS,Material.GOLD_BLOCK);c.theatreStage(0,3,180,16,Material.SMOOTH_SANDSTONE,Material.CUT_COPPER);
        for(int s:new int[]{-1,1}){c.chamferedDice(s*27,18,169,6,Material.POLISHED_BLACKSTONE,Material.GOLD_BLOCK,Material.REDSTONE_LAMP);c.theatreBalcony(s*10,30,203,24,Material.DARK_OAK_PLANKS,Material.GOLD_BLOCK);c.gear(s*31,47,198,9,Material.CUT_COPPER,Material.RED_NETHER_BRICKS);}
        c.raysAndSpiral(0,14,190,24,1,Material.IRON_CHAIN,Material.OCHRE_FROGLIGHT);
    }
    private void fracturedReality(){
        c.wing(Wing.IMPOSSIBLE);c.box(-44,-5,46,-41,0,98,Material.OBSIDIAN);c.box(41,-5,46,44,0,98,Material.PURPUR_BLOCK);
        c.fragmentedDice(0,31,73,13,Material.PURPLE_CONCRETE,Material.CYAN_CONCRETE,Material.SEA_LANTERN);c.fracturedRings(0,32,73,31,Material.AMETHYST_BLOCK,Material.CYAN_CONCRETE);
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9;int u=(int)Math.round(33*Math.cos(a)),v=72+(int)Math.round(22*Math.sin(a)),h=7+i*5;c.crystalCluster(u,h,v,4,Material.PURPLE_STAINED_GLASS,i%2==0?Material.OBSIDIAN:Material.BLUE_ICE);c.cloudIsland(-u,h/2,v+3,3,Material.CRYING_OBSIDIAN,Material.TINTED_GLASS);}
        c.raysAndSpiral(0,6,72,28,2,Material.END_ROD,Material.SEA_LANTERN);
    }
    private void infinitePalace(){
        c.wing(Wing.IMPOSSIBLE);for(int v=101;v<=157;v+=8){c.palaceArches((v%16==5?5:-5),1,v,1,Material.PURPUR_PILLAR,Material.MAGENTA_STAINED_GLASS);c.fracturedRings(0,33,v,35,Material.PURPUR_BLOCK,Material.CYAN_CONCRETE);}
        c.impossibleStaircase(0,8,128,27,88,Material.QUARTZ_BLOCK,Material.MAGENTA_CONCRETE);c.mirrorPrism(0,18,132,9,58,Material.CYAN_CONCRETE,Material.PURPLE_STAINED_GLASS);
        for(int s:new int[]{-1,1}){c.cardCastle(s*24,6,112);c.crystalCluster(s*31,5,148,7,Material.MAGENTA_STAINED_GLASS,Material.END_STONE_BRICKS);c.pipeNetwork(s*37,2,105,29,Material.PURPUR_PILLAR,Material.SEA_LANTERN);}
    }
    private void crownOfDice(){
        c.wing(Wing.IMPOSSIBLE);for(int v=158;v<=216;v+=7){int r=42-(v-158)/8;for(int a=0;a<360;a+=12){double angle=Math.toRadians(a);int u=(int)Math.round(r*Math.cos(angle));if(a%36!=0)c.p(u,8+(v-158)/2,v,Material.PURPLE_CONCRETE);}if(v%14==4)c.fracturedRings(0,52,v,Math.max(17,r),Material.CYAN_CONCRETE,Material.MAGENTA_CONCRETE);}
        c.crown(0,86,190,28,14,Material.PURPLE_CONCRETE,Material.SEA_LANTERN);c.fragmentedDice(0,103,190,7,Material.QUARTZ_BLOCK,Material.MAGENTA_CONCRETE,Material.SEA_LANTERN);
        c.raysAndSpiral(0,65,190,30,3,Material.CYAN_CONCRETE,Material.END_ROD);c.impossibleStaircase(0,55,188,31,72,Material.PURPUR_BLOCK,Material.MAGENTA_CONCRETE);
        for(int s:new int[]{-1,1}){c.crystalCluster(s*34,15,169,8,Material.CYAN_STAINED_GLASS,Material.OBSIDIAN);c.palaceArches(s*4,3,164,7,Material.PURPUR_PILLAR,Material.MAGENTA_STAINED_GLASS);}
        c.p(0,120,210,Material.END_ROD);
    }
    private void connectChapters(){
        for(Wing wing:Wing.values()){
            c.wing(wing);Material[] theme=switch(wing){
                case EASY -> new Material[]{Material.BAMBOO_MOSAIC,Material.WAXED_OXIDIZED_COPPER,Material.VERDANT_FROGLIGHT};
                case MEDIUM -> new Material[]{Material.QUARTZ_BRICKS,Material.GOLD_BLOCK,Material.SEA_LANTERN};
                case HARD -> new Material[]{Material.DARK_OAK_PLANKS,Material.CUT_COPPER,Material.OCHRE_FROGLIGHT};
                case IMPOSSIBLE -> new Material[]{Material.PURPUR_BLOCK,Material.CYAN_CONCRETE,Material.PEARLESCENT_FROGLIGHT};};
            Landing first=WonderRoute.stage(wing,1).landings().getFirst();
            c.chapterConnector(0,FLOOR,42,first.u(),first.y(),first.v(),theme[0],theme[1],theme[2]);
            for(int stage=1;stage<3;stage++){
                Landing from=WonderRoute.stage(wing,stage).landings().getLast(),to=WonderRoute.stage(wing,stage+1).landings().getFirst();
                c.chapterConnector(from.u(),from.y(),from.v(),to.u(),to.y(),to.v(),theme[0],theme[1],theme[2]);
            }
        }
    }
    private void materializeAllRoutes(){
        for(Wing wing:Wing.values()){c.wing(wing);for(Stage stage:WonderRoute.stages(wing))materializeRoute(stage);}
        for(Wing wing:Wing.values()){c.wing(wing);for(Landing node:WonderRoute.route(wing))clearHeadroom(node);}
    }
    private void clearHeadroom(Landing node){c.box(node.u()+node.minOffset(),node.y()-FLOOR+1,node.v()+node.minOffset(),node.u()+node.maxOffset(),node.y()-FLOOR+3,node.v()+node.maxOffset(),Material.AIR);}
    private void materializeRoute(Stage stage){
        Material[] palette=switch(stage.wing()){
            case EASY -> new Material[]{Material.PINK_TERRACOTTA,Material.YELLOW_TERRACOTTA,Material.LIGHT_BLUE_TERRACOTTA,Material.WAXED_OXIDIZED_CUT_COPPER,Material.SMOOTH_SANDSTONE};
            case MEDIUM -> new Material[]{Material.QUARTZ_BLOCK,Material.CYAN_TERRACOTTA,Material.PRISMARINE_BRICKS,Material.AMETHYST_BLOCK,Material.GOLD_BLOCK};
            case HARD -> new Material[]{Material.POLISHED_DEEPSLATE,Material.CUT_COPPER,Material.RED_NETHER_BRICKS,Material.DARK_OAK_PLANKS,Material.GILDED_BLACKSTONE};
            case IMPOSSIBLE -> new Material[]{Material.PURPUR_BLOCK,Material.CYAN_CONCRETE,Material.MAGENTA_CONCRETE,Material.OBSIDIAN,Material.QUARTZ_BLOCK};};
        for(Landing node:stage.landings()){
            int h=node.y()-FLOOR,r=node.radius();Material deck=palette[node.transition()%palette.length];
            switch(node.obstacle()){
                case CHIP -> {c.disc(node.u(),h,node.v(),Math.max(1,r),deck);c.ring(node.u(),h-1,node.v(),Math.max(2,r+1),Material.GOLD_BLOCK);}
                case ORNAMENTAL_BRIDGE -> {c.box(node.u()-Math.max(1,r),h,node.v(),node.u()+Math.max(1,r),h,node.v(),deck);c.p(node.u(),h-1,node.v(),Material.IRON_CHAIN);}
                case FLOWER_CROWN -> {c.disc(node.u(),h,node.v(),Math.max(1,r),deck);for(int a=0;a<8;a++){double angle=a*Math.PI/4;c.p(node.u()+(int)Math.round((r+1)*Math.cos(angle)),h-1,node.v()+(int)Math.round((r+1)*Math.sin(angle)),Material.FLOWERING_AZALEA_LEAVES);}}
                case FOUNTAIN_RIM -> {c.ring(node.u(),h,node.v(),Math.max(1,r),Material.CALCITE);c.p(node.u(),h,node.v(),deck);}
                case GEAR_TOOTH -> {c.p(node.u(),h,node.v(),deck);c.ring(node.u(),h-1,node.v(),Math.max(1,r+1),Material.CUT_COPPER);}
                case CLOCK_HAND -> {c.box(node.u()-r,h,node.v(),node.u()+r,h,node.v(),deck);c.p(node.u(),h-1,node.v(),Material.GOLD_BLOCK);}
                case CARD_EDGE,MIRROR_FRAME,THEATRE_BEAM,PIPE -> {c.box(node.u()-r,h,node.v(),node.u()+r,h,node.v(),deck);c.box(node.u()-r,h-2,node.v(),node.u()+r,h-1,node.v(),palette[(node.transition()+1)%palette.length]);}
                case CLOUD -> {c.disc(node.u(),h,node.v(),Math.max(1,r),Material.WHITE_WOOL);c.p(node.u(),h,node.v(),deck);}
                case CRYSTAL,FRAGMENT,CROWN_POINT -> {c.disc(node.u(),h,node.v(),r,deck);c.p(node.u(),h-1,node.v(),node.obstacle()==Obstacle.CROWN_POINT?Material.GOLD_BLOCK:Material.END_ROD);}
                case RING,ROULETTE_RIM -> {c.ring(node.u(),h,node.v(),Math.max(1,r),deck);c.p(node.u(),h,node.v(),palette[(node.transition()+2)%palette.length]);}
                case BALCONY,STATUE_LEDGE,TOY_BLOCK,DICE_LEDGE,PALACE_STEP,CURVED_STEP,SLAB,TRAPDOOR,FINISH,START -> {c.disc(node.u(),h,node.v(),r,deck);c.p(node.u(),h-1,node.v(),palette[(node.transition()+1)%palette.length]);}
                case LADDER,HONEY,SLIME -> throw new IllegalStateException("special movement obstacle requires authored validator: "+node.obstacle());
            }
            // Exact collision footprint used by BlockRegion, including even widths and all four rotations.
            c.box(node.u()+node.minOffset(),h,node.v()+node.minOffset(),node.u()+node.maxOffset(),h,node.v()+node.maxOffset(),deck);
            if(node.checkpoint()){c.ring(node.u(),h-1,node.v(),r+2,Material.SEA_LANTERN);c.p(node.u(),h-1,node.v(),Material.GOLD_BLOCK);}
            c.box(node.u()-r,h+1,node.v()-r,node.u()+r,h+3,node.v()+r,Material.AIR);
        }
    }
}
