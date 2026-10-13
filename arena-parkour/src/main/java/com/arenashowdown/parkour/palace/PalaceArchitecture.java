package com.arenashowdown.parkour.palace;

import org.bukkit.Material;
import static org.bukkit.Material.*;
import static com.arenashowdown.parkour.palace.PalaceRoute.*;

/** Architectural composition in local wing coordinates: u lateral, v outward, h above floor. */
public final class PalaceArchitecture {
    private final PalaceBlocks b=new PalaceBlocks();
    private Wing w=Wing.EASY;
    public static PalaceBlocks build(){var a=new PalaceArchitecture();a.atrium();for(Wing w:Wing.values()){a.w=w;a.wing();}return a.b;}
    private void p(int u,int h,int v,Material m){b.set(w.x(u,v),FLOOR+h,w.z(u,v),m);}
    private void box(int u,int h,int v,int U,int H,int V,Material m){for(int x=Math.min(u,U);x<=Math.max(u,U);x++)for(int y=Math.min(h,H);y<=Math.max(h,H);y++)for(int z=Math.min(v,V);z<=Math.max(v,V);z++)p(x,y,z,m);}
    private void slab(int u,int h,int v,Material m){b.state(w.x(u,v),FLOOR+h,w.z(u,v),m,"type=bottom");}
    private void disc(int u,int h,int v,int r,Material m){for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r)p(u+x,h,v+z,m);}
    private void ring(int u,int h,int v,int r,Material m){for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(x*x+z*z<=r*r&&x*x+z*z>=(r-1)*(r-1))p(u+x,h,v+z,m);}
    private Material accent(){return switch(w){case EASY->YELLOW_TERRACOTTA;case MEDIUM->CYAN_TERRACOTTA;case HARD->RED_TERRACOTTA;case IMPOSSIBLE->PURPLE_TERRACOTTA;};}
    private void column(int u,int v,int h,int height){
        box(u-2,h,v-2,u+2,h+1,v+2,POLISHED_BLACKSTONE_BRICKS);
        box(u-1,h+2,v-1,u+1,h+height-3,v+1,QUARTZ_PILLAR);
        for(int dy=3;dy<height-3;dy++){p(u-2,h+dy,v,CALCITE);p(u+2,h+dy,v,CALCITE);}
        for(int d:new int[]{2,height-2})for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)slab(u+x,h+d,v+z,SMOOTH_QUARTZ_SLAB);
        box(u-2,h+height-1,v-2,u+2,h+height,v+2,WAXED_CUT_COPPER);
        p(u,h+height+1,v,GOLD_BLOCK);
    }
    private void chandelier(int u,int v,int top,int radius){
        for(int h=top-4;h<=top;h++)p(u,h,v,IRON_CHAIN);
        for(int tier=0;tier<3;tier++){
            int r=Math.max(1,radius-tier*2),h=top-5-tier*2;
            ring(u,h,v,r,WAXED_CUT_COPPER);
            for(int angle=0;angle<360;angle+=45){int x=(int)Math.round(r*Math.cos(Math.toRadians(angle))),z=(int)Math.round(r*Math.sin(Math.toRadians(angle)));
                p(u+x,h+1,v+z,END_ROD);p(u+x,h-1,v+z,OCHRE_FROGLIGHT);p(u+x,h-2,v+z,YELLOW_STAINED_GLASS);
            }
        }
    }
    private void arch(int v,int half,int spring,int rise){
        for(int u=-half-2;u<=half+2;u++){
            int top=spring+(int)Math.round(rise*Math.sqrt(Math.max(0,1-u*u/(double)(half*half))));
            if(Math.abs(u)>=half)box(u,1,v-1,u,spring,v+1,QUARTZ_PILLAR);
            for(int h=top;h<=top+2;h++){p(u,h,v,SMOOTH_QUARTZ);p(u,h,v+1,WAXED_CUT_COPPER);}
            if(u%3==0)p(u,top-1,v,OCHRE_FROGLIGHT);
        }
    }
    private void window(int side,int v,int height){
        for(int z=-4;z<=4;z++)for(int h=5;h<=height;h++){
            boolean rim=Math.abs(z)==4||h==5||h==height;
            p(side*21,h,v+z,rim?SMOOTH_QUARTZ:((z+h)%5==0?accent():BLACK_STAINED_GLASS));
            if(!rim)p(side*22,h,v+z,OCHRE_FROGLIGHT);
            p(side*23,h,v+z,rim?SMOOTH_QUARTZ:((z+h)%5==0?accent():BLACK_STAINED_GLASS));
        }
        for(int z=-5;z<=5;z++){slab(side*20,4,v+z,SMOOTH_QUARTZ_SLAB);p(side*23,height+1,v+z,WAXED_CUT_COPPER);}
    }
    private void dice(int u,int h,int v,int r,boolean crown){
        for(int x=-r;x<=r;x++)for(int y=-r;y<=r;y++)for(int z=-r;z<=r;z++){
            int sum=Math.abs(x)+Math.abs(y)+Math.abs(z);if(sum>3*r-2)continue;
            if(Math.abs(x)==r||Math.abs(y)==r||Math.abs(z)==r||sum>=3*r-3)p(u+x,h+y,v+z,sum>=3*r-3?WAXED_CUT_COPPER:CALCITE);
        }
        for(int a:new int[]{-1,1})for(int c:new int[]{-1,1}){
            p(u+a*(r/2),h+c*(r/2),v+r,POLISHED_BLACKSTONE);p(u+r,h+a*(r/2),v+c*(r/2),POLISHED_BLACKSTONE);
        }
        p(u,h,v+r,BLACK_STAINED_GLASS);p(u,h,v+r-1,OCHRE_FROGLIGHT);
        if(crown){ring(u,h+r+1,v,r+1,WAXED_CUT_COPPER);for(int a=0;a<360;a+=45){int x=(int)Math.round((r+1)*Math.cos(Math.toRadians(a))),z=(int)Math.round((r+1)*Math.sin(Math.toRadians(a)));p(u+x,h+r+2,v+z,GOLD_BLOCK);p(u+x,h+r+3,v+z,END_ROD);}}
    }
    private void card(int u,int v,int base,int half,Material suit){
        for(int x=-half;x<=half;x++)for(int h=0;h<=half*3;h++){
            p(u+x,base+h,v,Math.abs(x)==half||h==0||h==half*3?WAXED_CUT_COPPER:SMOOTH_QUARTZ);
            if(Math.abs(x)+Math.abs(h-half*3/2)<=half-1)p(u+x,base+h,v-1,suit);
        }
        for(int x:new int[]{-half,half})p(u+x,base-1,v,END_ROD);
    }
    private void curtain(int u,int v,int base,int height,int width){
        for(int x=0;x<width;x++)box(u+x,base+(x==width/2?2:0),v+x%2,u+x,base+height,v+x%2,x%3==0?RED_TERRACOTTA:RED_WOOL);
        box(u,base+height+1,v,u+width-1,base+height+1,v+1,WAXED_CUT_COPPER);
    }
    private void bench(int u,int v,int h){box(u-2,h,v,u+2,h,v,RED_WOOL);box(u-2,h,v+1,u+2,h+1,v+1,DARK_OAK_PLANKS);for(int x:new int[]{-3,3})p(u+x,h,v,DARK_OAK_TRAPDOOR);}
    private void tree(int u,int v){box(u,-5,v,u,3,v,DARK_OAK_LOG);for(int h=1;h<7;h++)disc(u,h,v,Math.max(1,5-Math.abs(h-3)),h%2==0?FLOWERING_AZALEA_LEAVES:AZALEA_LEAVES);}
    private void fountain(int u,int v){disc(u,-5,v,6,SMOOTH_QUARTZ);ring(u,-4,v,6,WAXED_CUT_COPPER);disc(u,-4,v,5,BLUE_STAINED_GLASS);disc(u,-3,v,2,SMOOTH_QUARTZ);box(u,-2,v,u,2,v,SEA_LANTERN);disc(u,3,v,2,SMOOTH_QUARTZ);p(u,4,v,END_ROD);}
    private void atrium(){
        w=Wing.EASY;
        for(int u=-33;u<=33;u++)for(int v=-33;v<=33;v++){
            if(Math.abs(u)+Math.abs(v)>59)continue;
            box(u,-4,v,u,-1,v,POLISHED_DEEPSLATE);
            int d=Math.abs(u)+Math.abs(v);
            p(u,0,v,d<20?(d%6==0?WAXED_CUT_COPPER:CALCITE):((u/4+v/4)%2==0?POLISHED_BLACKSTONE:SMOOTH_QUARTZ));
            if(d==59)box(u,1,v,u,30,v,POLISHED_DEEPSLATE);
            double ell=(u*u+v*v)/1024.0;int roof=ell<.4?42:ell<.7?39:ell<1?35:31;
            p(u,roof,v,POLISHED_BLACKSTONE_BRICKS);p(u,roof+1,v,DEEPSLATE_TILES);
            if(ell>.38&&ell<.43||ell>.68&&ell<.73||ell>.98&&ell<1.03)p(u,roof-1,v,WAXED_CUT_COPPER);
            if(ell>.73&&ell<.77)p(u,36,v,OCHRE_FROGLIGHT);
        }
        for(Wing wing:Wing.values()){
            w=wing;
            for(int s:new int[]{-1,1}){
                box(s*12,1,32,s*27,30,33,POLISHED_DEEPSLATE);
                column(s*13,29,1,29);column(s*26,26,1,29);
                box(s*17,7,31,s*22,22,31,RED_TERRACOTTA);
                card(s*20,30,10,3,s<0?RED_CONCRETE:BLACK_CONCRETE);
                curtain(s*24,28,2,24,3);
            }
            arch(29,10,11,9);
            switch(wing){
                case EASY -> {for(int h=24;h<=27;h++)disc(0,h,28,4,h%2==0?CALCITE:YELLOW_TERRACOTTA);}
                case MEDIUM -> {card(-4,28,24,2,CYAN_CONCRETE);card(3,28,25,2,PURPLE_CONCRETE);}
                case HARD -> {curtain(-8,29,22,7,5);curtain(4,29,22,7,5);}
                case IMPOSSIBLE -> dice(0,28,28,3,true);
            }
            // Faceted upper balcony wrapping each atrium corner.
            box(17,14,20,28,14,26,DARK_OAK_PLANKS);
            for(int u=17;u<=28;u++)p(u,15,20,POLISHED_BLACKSTONE_BRICK_WALL);
            for(int i=0;i<14;i++)for(int u=23;u<=26;u++){
                int v=3+i;
                String facing=wing==Wing.EASY?"north":wing==Wing.MEDIUM?"east":wing==Wing.HARD?"west":"south";
                b.state(wing.x(u,v),71+i,wing.z(u,v),SMOOTH_QUARTZ_STAIRS,"facing="+facing);
            }
            box(23,14,17,26,14,20,DARK_OAK_PLANKS);box(23,15,20,26,17,20,AIR);
            bench(18,23,15);
            // Warm alcove fountains and a cheerful original usher sculpture.
            disc(-20,0,18,4,SMOOTH_QUARTZ);disc(-20,1,18,3,BLUE_STAINED_GLASS);p(-20,2,18,SEA_LANTERN);
        }
        w=Wing.EASY;
        chandelier(0,0,40,11);dice(0,22,0,5,false);
        for(int x:new int[]{-3,3})box(x,1,-20,x,3,-20,POLISHED_BLACKSTONE);
        box(-3,4,-20,3,8,-20,RED_TERRACOTTA);dice(0,11,-20,3,false);
        box(-4,15,-22,4,15,-18,POLISHED_BLACKSTONE);box(-2,16,-21,2,18,-19,POLISHED_BLACKSTONE);
        // Eyes and a white cartoon grin on the face looking toward spawn.
        p(-1,12,-17,BLACK_CONCRETE);p(1,12,-17,BLACK_CONCRETE);box(-1,10,-17,1,10,-17,WHITE_CONCRETE);
    }
    private void wing(){
        for(int u=-23;u<=23;u++)for(int v=32;v<=125;v++){
            p(u,-7,v,POLISHED_DEEPSLATE);
            p(u,-6,v,(Math.floorDiv(u,3)+Math.floorDiv(v,3))%2==0?POLISHED_BLACKSTONE:accent());
            int roof=32+Math.min(2,(v-32)/28)*2+(int)Math.round(7*Math.sqrt(Math.max(0,1-u*u/529.0)));
            p(u,roof,v,DEEPSLATE_TILES);
            if(v%7==0)p(u,roof-1,v,WAXED_CUT_COPPER);
            if(v%14==0)p(u,roof+1,v,WAXED_CUT_COPPER);
            if(Math.abs(u)==19)p(u,roof-2,v,OCHRE_FROGLIGHT);
        }
        for(int s:new int[]{-1,1}){
            box(s*23,-5,32,s*23,33,125,POLISHED_DEEPSLATE);
            // Raised visitor gallery is separated from timed course by solid glass.
            box(s*18,8,33,s*21,8,123,DARK_OAK_PLANKS);
            box(s*17,9,33,s*17,11,123,BLACK_STAINED_GLASS);
            for(int v=36;v<=120;v+=14){
                column(s*19,v,-5,36);window(s,v+6,26);
                box(s*24,-5,v,s*25,34,v,QUARTZ_PILLAR);
                p(s*20,10,v+5,LANTERN);
            }
        }
        box(-23,-5,125,23,34,126,POLISHED_DEEPSLATE);
        // End pavilions give each face a stepped silhouette and a projecting frame.
        for(int s:new int[]{-1,1}){
            column(s*19,127,-5,42);
            for(int tier=0;tier<4;tier++){
                int radius=5-tier;
                for(int u=-radius;u<=radius;u++)for(int v=-radius;v<=radius;v++)if(Math.abs(u)+Math.abs(v)<=radius+2)
                    p(s*19+u,36+tier*2,123+v,tier%2==0?WAXED_CUT_COPPER:POLISHED_BLACKSTONE_BRICKS);
            }
            p(s*19,44,123,END_ROD);
        }
        for(int h=4;h<=30;h++)for(int u=-15;u<=15;u++){
            if(Math.abs(u)+Math.abs(h-17)<20)p(u,h,124,(u+h)%5==0?accent():PURPLE_STAINED_GLASS);
            p(u,h,125,OCHRE_FROGLIGHT);
            if(Math.abs(u)+Math.abs(h-17)<20)p(u,h,126,(u+h)%5==0?accent():PURPLE_STAINED_GLASS);
            if(Math.abs(u)+Math.abs(h-17)==20)p(u,h,127,SMOOTH_QUARTZ);
        }
        for(int u=-16;u<=16;u++)p(u,32+(16-Math.abs(u))/3,127,WAXED_CUT_COPPER);
        for(int stage=0;stage<3;stage++){
            int c=46+stage*28;
            arch(c-13,15,16,10);chandelier(0,c,35,5);
            for(int s:new int[]{-1,1}){
                box(s*10,-5,c-11,s*15,-5,c+11,stage==1?DARK_OAK_PLANKS:SMOOTH_QUARTZ);
                bench(s*13,c+9,-4);
            }
            scene(stage,c);
        }
        // Carve the route AFTER scenery: no invisible head collisions or obstructed jumps.
        box(-4,0,32,4,13,116,AIR);
        for(var node:landings(w)){
            int h=node.y()-FLOOR;
            Material deck=node.checkpoint()?WAXED_CUT_COPPER:SMOOTH_QUARTZ;
            box(node.u()+node.lo(),h,node.v()+node.lo(),node.u()+node.hi(),h,node.v()+node.hi(),deck);
            p(node.u(),h-1,node.v(),OCHRE_FROGLIGHT);
            // Supports connect landings to the set: chip pedestals, books, theatrical rigging or obsidian spires.
            Material support=switch(w){case EASY->RED_TERRACOTTA;case MEDIUM->BOOKSHELF;case HARD->DARK_OAK_FENCE;case IMPOSSIBLE->POLISHED_BLACKSTONE_WALL;};
            box(node.u(),-5,node.v(),node.u(),h-2,node.v(),support);
            if(w==Wing.EASY){ring(node.u(),h-2,node.v(),1,RED_CONCRETE);}
        }
        // Finish balcony, beyond the last node, reached after completion.
        box(-7,6,119,7,6,123,SMOOTH_QUARTZ);
        for(int u=-7;u<=7;u++)p(u,7,123,POLISHED_BLACKSTONE_BRICK_WALL);
        box(-2,7,122,2,8,122,WAXED_CUT_COPPER);p(0,9,122,GOLD_BLOCK);p(0,10,122,LIGHTNING_ROD);
    }
    private void scene(int stage,int c){
        switch(w){
            case EASY -> golden(stage,c);
            case MEDIUM -> cards(stage,c);
            case HARD -> theatre(stage,c);
            case IMPOSSIBLE -> shadows(stage,c);
        }
    }
    private void golden(int stage,int c){
        if(stage==0){
            for(int s:new int[]{-1,1})for(int z:new int[]{c-6,c+6}){
                for(int h=-4;h<=2;h++){disc(s*11,h,z,4,h%2==0?CALCITE:RED_TERRACOTTA);ring(s*11,h,z,4,WAXED_CUT_COPPER);}
                box(s*6,-3,z,s*7,-1,z,DARK_OAK_FENCE);disc(s*7,0,z,2,GREEN_TERRACOTTA);
            }
        }else if(stage==1){
            dice(-11,5,c-3,5,false);dice(11,9,c+4,5,false);
            // Curving stair sculpture around the large die, outside the validated lane.
            for(int i=0;i<24;i++){double a=i*Math.PI/12;int u=-11+(int)Math.round(7*Math.cos(a)),v=c-3+(int)Math.round(7*Math.sin(a));slab(u,-3+i/2,v,SMOOTH_QUARTZ_SLAB);}
        }else{
            fountain(-11,c);tree(11,c-6);tree(11,c+7);
            for(int z=c-10;z<c+11;z++){p(-9,-4,z,MOSS_BLOCK);if(z%3==0)p(-9,-3,z,RED_TULIP);}
            card(-12,c+8,6,4,LIME_TERRACOTTA);
        }
    }
    private void cards(int stage,int c){
        if(stage==0){
            for(int s:new int[]{-1,1})for(int z=c-10;z<=c+10;z++){
                box(s*12,-4,z,s*15,13,z,z%7==0?DARK_OAK_LOG:BOOKSHELF);
                for(int h:new int[]{0,5,10,14})p(s*11,h,z,WAXED_CUT_COPPER);
            }
            card(-8,c,3,4,RED_CONCRETE);card(8,c+6,7,4,CYAN_CONCRETE);
        }else if(stage==1){
            for(int s:new int[]{-1,1})for(int i=0;i<3;i++){
                card(s*(8+i),c-8+i*7,3+i*3,3,i%2==0?PURPLE_CONCRETE:RED_CONCRETE);
                box(s*9,19,c-8+i*7,s*9,28,c-8+i*7,IRON_CHAIN);
            }
            for(int z=c-11;z<=c+11;z++)slab(-12,10,z,DARK_OAK_SLAB);
        }else{
            for(int s:new int[]{-1,1})for(int i=-1;i<=1;i++){
                int v=c+i*7;box(s*13,0,v-2,s*13,16,v+2,BLUE_ICE);
                box(s*12,0,v-3,s*12,17,v-3,WAXED_CUT_COPPER);box(s*12,0,v+3,s*12,17,v+3,WAXED_CUT_COPPER);
                for(int d=-4;d<=4;d++)p(s*11,10+Math.abs(d),v+d,SMOOTH_QUARTZ);
            }
            dice(-9,20,c,4,false);
        }
    }
    private void theatre(int stage,int c){
        if(stage==0){
            curtain(-15,c-6,-4,28,6);curtain(9,c+6,-4,28,6);
            for(int h:new int[]{8,16,24}){
                box(-16,h,c-10,-8,h,c+10,DARK_OAK_PLANKS);
                for(int z=c-9;z<=c+9;z+=6){p(-7,h+1,z,OCHRE_FROGLIGHT);box(10,h,z,15,h,z,DARK_OAK_LOG);}
            }
        }else if(stage==1){
            // Roulette wheel in a vertical frame; the course crosses its central opening.
            for(int u=-15;u<=15;u++)for(int h=-2;h<=28;h++){
                double r=Math.hypot(u,h-13);
                if(r>11&&r<15)p(u,h,c,((int)(Math.atan2(h-13,u)*8)&1)==0?RED_TERRACOTTA:POLISHED_BLACKSTONE);
                if(r>14&&r<15)p(u,h,c-1,WAXED_CUT_COPPER);
            }
            for(int s:new int[]{-1,1})gear(s*11,9,c+8,5);
        }else{
            curtain(-16,c+8,-3,28,7);curtain(10,c+8,-3,28,7);
            for(int i=0;i<4;i++)for(int s:new int[]{-1,1})box(s*(9+i*2),i-4,c-9,s*(10+i*2),i-4,c+7,DARK_OAK_PLANKS);
            for(int s:new int[]{-1,1}){box(s*11,14,c-8,s*11,22,c-8,IRON_CHAIN);p(s*11,13,c-8,SEA_LANTERN);}
            box(-8,16,c+10,8,17,c+10,RED_TERRACOTTA);
        }
    }
    private void gear(int u,int h,int v,int r){
        for(int x=-r-1;x<=r+1;x++)for(int y=-r-1;y<=r+1;y++){
            double d=Math.hypot(x,y);if(d>r-2&&d<=r||((Math.abs(x)%3==0||Math.abs(y)%3==0)&&d>r&&d<r+1.2))p(u+x,h+y,v,WAXED_CUT_COPPER);
            if((x==0||y==0)&&d<r)p(u+x,h+y,v,POLISHED_BLACKSTONE);
        }
        p(u,h,v,GOLD_BLOCK);
    }
    private void shadows(int stage,int c){
        if(stage==0){
            dice(-12,7,c-3,6,false);dice(12,14,c+3,6,false);
            for(int s:new int[]{-1,1})for(int i=0;i<6;i++){
                box(s*(7+i),1,c+8,s*(7+i),18-i,c+8,POLISHED_BLACKSTONE_BRICKS);p(s*(7+i),19-i,c+8,PEARLESCENT_FROGLIGHT);
            }
        }else if(stage==1){
            for(int s:new int[]{-1,1})for(int i=0;i<3;i++){
                card(s*11,c-8+i*8,-2+i*4,4,PURPLE_CONCRETE);
                box(s*7,10+i*3,c-8+i*8,s*15,10+i*3,c-8+i*8,POLISHED_BLACKSTONE_BRICKS);
                p(s*16,11+i*3,c-8+i*8,PEARLESCENT_FROGLIGHT);
            }
        }else{
            // Crowned throne overlooking the final landing, with a deep stepped plinth.
            for(int i=0;i<5;i++)box(-11+i,7+i,c+8+i,11-i,7+i,c+13,POLISHED_BLACKSTONE_BRICKS);
            box(-7,12,c+13,7,27,c+14,PURPLE_TERRACOTTA);
            for(int s:new int[]{-1,1}){column(s*10,c+10,7,24);box(s*7,12,c+7,s*8,15,c+13,WAXED_CUT_COPPER);}
            dice(0,23,c+9,6,true);
            for(int x=-6;x<=6;x+=3)p(x,12,c+7,PEARLESCENT_FROGLIGHT);
        }
    }
}
