package com.arenashowdown.cassino.build;

import org.bukkit.Material;
import static org.bukkit.Material.*;

/** Original V2 architecture. Coordinates use the lobby floor as y=0. */
public final class GrandCasino {
    private final Blueprint b = new Blueprint();
    private final int f;
    private GrandCasino(int base) { f=base+2; }
    public static Blueprint create(int base) {
        GrandCasino g=new GrandCasino(base);
        g.terrain(); g.hall(); g.ceiling(); g.facade(); g.galleries(); g.stage();
        g.wings(); g.portals(); g.plaza(); g.exteriorFinish();
        return g.b;
    }
    private void box(int x,int y,int z,int X,int Y,int Z,Material m){b.box(x,f+y,z,X,f+Y,Z,m);}
    private void p(int x,int y,int z,Material m){b.set(x,f+y,z,m);}
    private void state(int x,int y,int z,Material m,String s){b.data(x,f+y,z,m,s);}
    private void ring(int x,int y,int z,int r,Material m){b.ring(x,f+y,z,r,1,m);}
    private void terrain(){
        for(int y=-7;y<=-3;y++) b.disc(0,f+y,0,98+(y+3)/2,POLISHED_DEEPSLATE);
        b.disc(0,f-2,0,98,SMOOTH_STONE);
        ring(0,-1,0,97,POLISHED_BLACKSTONE_BRICKS);
        box(-49,-1,-42,49,0,42,POLISHED_BLACKSTONE);
        // Single elevation: the decorative pattern never makes bumps in walking routes.
        for(int x=-47;x<=47;x++) for(int z=-40;z<=40;z++){
            Material m=((Math.floorDiv(x,4)+Math.floorDiv(z,4))&1)==0?SMOOTH_QUARTZ:POLISHED_BLACKSTONE;
            if(Math.abs(x)<9) m=Math.abs(x)==8?YELLOW_TERRACOTTA:RED_TERRACOTTA;
            if(Math.abs(x)+Math.abs(z)<19) m=(Math.abs(x)+Math.abs(z))%6==0?YELLOW_TERRACOTTA:CALCITE;
            if(Math.abs(x)==30||Math.abs(z)==39)m=WAXED_CUT_COPPER;
            p(x,0,z,m);
        }
    }
    private void column(int x,int z,int h){
        box(x-1,1,z-1,x+1,2,z+1,POLISHED_BLACKSTONE_BRICKS);
        box(x,3,z,x,h-2,z,QUARTZ_PILLAR);
        for(int y=3;y<h-2;y++){p(x-1,y,z,CALCITE);p(x+1,y,z,CALCITE);}
        box(x-1,h-2,z-1,x+1,h-1,z+1,WAXED_CUT_COPPER);
        p(x,h,z,GOLD_BLOCK);
        for(int dx:new int[]{-1,1})state(x+dx,2,z,SMOOTH_QUARTZ_STAIRS,"facing="+(dx<0?"east":"west")+",half=top");
    }
    private void hall(){
        box(-49,1,-42,49,29,-41,POLISHED_DEEPSLATE);
        box(-49,1,41,49,29,42,POLISHED_DEEPSLATE);
        for(int side:new int[]{-1,1}){
            box(side*48,1,-40,side*49,29,40,POLISHED_DEEPSLATE);
            for(int z=-32;z<=32;z+=16){
                column(side*34,z,16);
                // Recessed panels, projecting pilasters, luminous coves, and alternating diamonds.
                box(side*47,4,z-3,side*47,23,z+3,RED_TERRACOTTA);
                for(int dz:new int[]{-4,4})box(side*46,2,z+dz,side*46,27,z+dz,QUARTZ_PILLAR);
                box(side*46,24,z-3,side*46,24,z+3,WAXED_CUT_COPPER);
                for(int dy=-3;dy<=3;dy++)for(int dz=-3;dz<=3;dz++)
                    if(Math.abs(dy)+Math.abs(dz)==3)p(side*45,19+dy,z+dz,(z%32==0)?CALCITE:YELLOW_TERRACOTTA);
                p(side*45,19,z,OCHRE_FROGLIGHT);
                lamp(side*32,5,z);
            }
        }
        for(int x=-40;x<=40;x+=10){
            box(x-3,6,-40,x+3,24,-40,RED_TERRACOTTA);
            box(x,3,-39,x,27,-39,QUARTZ_PILLAR);
        }
        // Continuous cornices and embedded wall light.
        for(int side:new int[]{-1,1})for(int z=-40;z<=40;z++){
            state(side*46,28,z,SMOOTH_QUARTZ_SLAB,"type=top");
            if(z%4==0)p(side*47,27,z,OCHRE_FROGLIGHT);
        }
    }
    private void ceiling(){
        // Elliptical stepped coffers, with different heights rather than a solid roof mass.
        for(int x=-49;x<=49;x++)for(int z=-42;z<=42;z++){
            double d=x*x/1444.0+z*z/900.0;
            int y=d<.38?38:d<.7?36:d<1?33:30;
            Material m=d<.38?RED_TERRACOTTA:POLISHED_BLACKSTONE_BRICKS;
            p(x,y,z,m);p(x,y+1,z,DEEPSLATE_TILES);
            if((d>.35&&d<.40)||(d>.67&&d<.72)||(d>.97&&d<1.03))p(x,y-1,z,WAXED_CUT_COPPER);
            if((Math.abs(x)%12==0||Math.abs(z)%12==0)&&d>1)p(x,y-1,z,SMOOTH_QUARTZ);
            if(d>.71&&d<.75)p(x,34,z,OCHRE_FROGLIGHT);
        }
        chandelier(0,0,36,9);
        for(int x:new int[]{-23,23})for(int z:new int[]{-23,23})chandelier(x,z,28,3);
    }
    private void chandelier(int x,int z,int top,int r){
        for(int y=top-4;y<=top;y++)p(x,y,z,IRON_CHAIN);
        for(int tier=0;tier<3;tier++){
            int radius=Math.max(1,r-tier*3), y=top-5-tier*3;
            ring(x,y,z,radius,WAXED_CUT_COPPER);
            for(int a=0;a<360;a+=45){
                int dx=(int)Math.round(Math.cos(Math.toRadians(a))*radius),dz=(int)Math.round(Math.sin(Math.toRadians(a))*radius);
                p(x+dx,y+1,z+dz,END_ROD);p(x+dx,y-1,z+dz,IRON_CHAIN);
                p(x+dx,y-2,z+dz,OCHRE_FROGLIGHT);
                p(x+dx,y-3,z+dz,YELLOW_STAINED_GLASS);
            }
        }
    }
    private void arch(int cx,int z,int half,int spring,int rise,int depth){
        for(int x=-half-2;x<=half+2;x++)for(int y=1;y<=spring+rise+2;y++){
            double curve=spring+rise*Math.sqrt(Math.max(0,1-x*x/(double)(half*half)));
            boolean opening=Math.abs(x)<half&&y<curve;
            for(int dz=0;dz<depth;dz++)p(cx+x,y,z+dz,opening?AIR:(y<curve+2?SMOOTH_QUARTZ:POLISHED_BLACKSTONE_BRICKS));
        }
    }
    private void facade(){
        // Raised central entrance and deep canopy.
        box(-18,0,41,18,0,53,SMOOTH_QUARTZ);
        arch(0,41,13,13,9,5);
        for(int side:new int[]{-1,1}){
            column(side*16,47,25);
            for(int y=0;y<=34;y++){
                int r=y<24?9:y<28?8:y<31?6:4;
                for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++)if(Math.abs(x)+Math.abs(z)<=r+5){
                    boolean edge=Math.abs(x)==r||Math.abs(z)==r||Math.abs(x)+Math.abs(z)==r+5;
                    if(edge||y==0||y==24||y==28||y==31||y==34)
                        p(side*34+x,y,43+z,y%7==0?WAXED_CUT_COPPER:POLISHED_DEEPSLATE);
                }
            }
            for(int dx:new int[]{-5,0,5}){
                box(side*34+dx,6,52,side*34+dx,21,52,YELLOW_STAINED_GLASS);
                box(side*34+dx,6,51,side*34+dx,21,51,OCHRE_FROGLIGHT);
                box(side*34+dx-1,4,53,side*34+dx-1,23,53,QUARTZ_PILLAR);
            }
            p(side*34,35,43,END_ROD);
        }
        for(int z=45;z<=58;z++){
            int half=23-(z-45)/3;
            box(-half,13,z,half,14,z,POLISHED_BLACKSTONE_BRICKS);
            p(-half,14,z,WAXED_CUT_COPPER);p(half,14,z,WAXED_CUT_COPPER);
            if(z%3==0)for(int x=-half+2;x<half;x+=4)p(x,12,z,OCHRE_FROGLIGHT);
        }
        box(-21,16,46,21,22,47,RED_TERRACOTTA);
        for(int x=-22;x<=22;x++){p(x,23,48,WAXED_CUT_COPPER);if(x%3==0)p(x,15,48,OCHRE_FROGLIGHT);}
        for(int i=0;i<5;i++)box(-18+i*3,25+i*2,43,18-i*3,26+i*2,46,POLISHED_BLACKSTONE_BRICKS);
        dice(0,39,44,7);
        // Three actual stair treads connect plaza (-2) to interior floor (0).
        for(int i=0;i<3;i++)for(int x=-18;x<=18;x++)state(x,-2+i,56-i,SMOOTH_QUARTZ_STAIRS,"facing=north");
        box(-18,0,53,18,0,53,SMOOTH_QUARTZ);
    }
    private void dice(int cx,int cy,int cz,int r){
        for(int x=-r;x<=r;x++)for(int y=-r;y<=r;y++)for(int z=-r;z<=r;z++){
            int sum=Math.abs(x)+Math.abs(y)+Math.abs(z);
            if(sum>3*r-4)continue;
            int faces=(Math.abs(x)==r?1:0)+(Math.abs(y)==r?1:0)+(Math.abs(z)==r?1:0);
            if(faces>0||sum>=3*r-5)p(cx+x,cy+y,cz+z,faces>1?WAXED_CUT_COPPER:CALCITE);
        }
        for(int[] dot:new int[][]{{-3,-3},{3,-3},{0,0},{-3,3},{3,3}}){
            box(cx+dot[0]-1,cy+dot[1]-1,cz+r,cx+dot[0]+1,cy+dot[1]+1,cz+r,POLISHED_BLACKSTONE);
            p(cx+dot[0],cy+dot[1],cz+r-1,OCHRE_FROGLIGHT);
            p(cx+dot[0],cy+dot[1],cz+r,BLACK_STAINED_GLASS);
        }
        for(int s:new int[]{-1,1})for(int y:new int[]{-3,3})for(int z:new int[]{-3,3})
            box(cx+s*r,cy+y-1,cz+z-1,cx+s*r,cy+y+1,cz+z+1,POLISHED_BLACKSTONE);
        box(cx-4,cy-r-2,cz-4,cx+4,cy-r-1,cz+4,WAXED_CUT_COPPER);
    }
    private void galleries(){
        for(int side:new int[]{-1,1}){
            box(side*35,14,-35,side*46,14,35,DARK_OAK_PLANKS);
            for(int z=-35;z<=35;z++){
                int edge=34-(Math.abs(z)<9?2:0);
                box(side*edge,14,z,side*35,14,z,DARK_OAK_PLANKS);
                state(side*edge,15,z,POLISHED_BLACKSTONE_BRICK_WALL,"north=low,south=low,up=true");
                if(z%8==0)lamp(side*edge,16,z);
            }
            // Wide stairs inside the hall; bottom south, top north.
            for(int i=0;i<14;i++){
                box(side*25,1,32-i,side*29,i,32-i,POLISHED_BLACKSTONE_BRICKS);
                for(int x=25;x<=29;x++)state(side*x,1+i,32-i,SMOOTH_QUARTZ_STAIRS,"facing=north");
            }
            box(side*25,14,15,side*35,14,18,DARK_OAK_PLANKS);
            box(side*34,15,15,side*34,17,18,AIR);
            for(int z:new int[]{-25,-5})sofa(side*40,15,z);
        }
    }
    private void stage(){
        box(-20,1,-39,20,3,-29,DARK_OAK_PLANKS);
        for(int i=0;i<3;i++)for(int x=-10;x<=10;x++)state(x,i+1,-26-i,DARK_OAK_STAIRS,"facing=north");
        box(-16,5,-39,16,22,-39,POLISHED_BLACKSTONE);
        for(int x:new int[]{-18,18})column(x,-35,25);
        for(int x=-23;x<=23;x++){
            int low=22-(int)(3*Math.sin(Math.PI*(x+23)/46));
            box(x,low,-35,x,26,-34, x%3==0?RED_TERRACOTTA:RED_WOOL);
            if(Math.abs(x)>18)box(x,4,-35,x,23,-33+(Math.abs(x)%2),x%2==0?RED_WOOL:RED_TERRACOTTA);
        }
        for(int x=-15;x<=15;x+=5)p(x,4,-29,OCHRE_FROGLIGHT);
        // Upright piano and microphone, away from ranking sightline.
        box(-15,4,-33,-10,6,-31,POLISHED_BLACKSTONE);
        for(int x=-15;x<=-10;x++)state(x,5,-30,SMOOTH_QUARTZ_SLAB,"type=bottom");
        p(12,4,-31,POLISHED_BLACKSTONE_WALL);p(12,5,-31,END_ROD);
    }
    private void wings(){
        for(int side:new int[]{-1,1}){
            int cx=side*68;
            for(int x=-17;x<=17;x++)for(int z=-24;z<=24;z++){
                if(Math.abs(x)+Math.abs(z)>34)continue;
                p(cx+x,0,z,(x+z)%4==0?SMOOTH_QUARTZ:DARK_OAK_PLANKS);
                boolean edge=Math.abs(x)==17||Math.abs(z)==24||Math.abs(x)+Math.abs(z)==34;
                if(edge)box(cx+x,1,z,cx+x,17,z,POLISHED_DEEPSLATE);
                p(cx+x,18,z,DEEPSLATE_TILES);
            }
            // Cross corridor joins both wings through the main hall wall.
            box(side*46,0,-3,side*57,0,3,SMOOTH_QUARTZ);
            box(side*46,1,-3,side*57,7,3,AIR);
            chandelier(cx,0,17,3);
            for(int z:new int[]{-16,16}){column(cx-11,z,16);column(cx+11,z,16);}
            if(side==1){
                for(int z:new int[]{-14,12}){sofa(cx-7,1,z);sofa(cx+6,1,z);table(cx,1,z);}
                box(cx-10,1,-21,cx+9,3,-19,DARK_OAK_PLANKS);
                for(int x=-9;x<=9;x+=3)p(cx+x,4,-20,FLOWER_POT);
            }else{
                for(int i=-1;i<=1;i++){
                    int x=cx+i*8,h=i==0?4:2;
                    box(x-2,1,-16,x+2,h,-12,SMOOTH_QUARTZ);
                    p(x,h+1,-14,GOLD_BLOCK);p(x,h+2,-14,LIGHTNING_ROD);
                    p(x-1,h+2,-14,WAXED_CUT_COPPER);p(x+1,h+2,-14,WAXED_CUT_COPPER);
                }
                for(int x:new int[]{cx-9,cx+8})sofa(x,1,14);
            }
        }
    }
    private void portals(){
        int index=0;
        for(LobbyPlan.Portal portal:LobbyPlan.PORTALS){
            int x=portal.x(),z=portal.z(),side=Integer.signum(x);
            for(int dz=-6;dz<=6;dz++)for(int y=1;y<=12;y++){
                int top=8+(int)Math.round(3*Math.sqrt(Math.max(0,1-dz*dz/36.0)));
                if(Math.abs(dz)>=5||y>=top){
                    p(x,y,z+dz,SMOOTH_QUARTZ);
                    if(y==top)p(x-side,y,z+dz,WAXED_CUT_COPPER);
                }
            }
            box(x,1,z-4,x,8,z+4,AIR);
            box(x-side*4,0,z-4,x+side*3,0,z+4,portal.accent());
            for(int dz:new int[]{-5,5})lamp(x-side,4,z+dz);
            // Individual dioramas sit above the arcade rather than blocking its passage.
            switch(index++){
                case 0 -> {for(int dz=-3;dz<=3;dz++)for(int y=12;y<=14;y++)p(x,y,z+dz,(dz+y)%2==0?WHITE_CONCRETE:BLACK_CONCRETE);}
                case 1 -> {box(x-2,13,z-3,x+2,13,z+3,WHITE_WOOL);p(x,14,z,GRASS_BLOCK);p(x,15,z,OAK_SAPLING);}
                case 2 -> {p(x,13,z,IRON_BLOCK);p(x,14,z,ANVIL);p(x,15,z,LIGHTNING_ROD);}
                case 3 -> {for(int y=12;y<=15;y++)b.disc(x,f+y,z,2,y%2==0?RED_CONCRETE:CALCITE);}
                case 4 -> {for(int i=-2;i<=2;i++)p(x,13+Math.abs(i),z+i*2,MAGENTA_CONCRETE);}
                case 5 -> {for(int s:new int[]{-1,1}){box(x,12,z+s*3,x,15,z+s*3,DARK_OAK_FENCE);box(x,15,z+s*3,x,16,z+s*3+s,s<0?RED_WOOL:BLUE_WOOL);}}
                case 6 -> {box(x-1,12,z-2,x+1,13,z+2,IRON_BARS);p(x,14,z,LODESTONE);}
                case 7 -> {for(int dz:new int[]{-2,0,2})p(x,13,z+dz,TNT);p(x,14,z,RED_STAINED_GLASS);}
            }
        }
    }
    private void sofa(int x,int y,int z){
        box(x-2,y,z,x+2,y,z,RED_WOOL);box(x-2,y,z+1,x+2,y+1,z+1,RED_WOOL);
        for(int dx:new int[]{-3,3})state(x+dx,y,z,DARK_OAK_STAIRS,"facing="+(dx<0?"east":"west"));
    }
    private void table(int x,int y,int z){p(x,y,z,DARK_OAK_FENCE);state(x,y+1,z,DARK_OAK_TRAPDOOR,"half=top");p(x,y+2,z,FLOWER_POT);}
    private void lamp(int x,int y,int z){p(x,y,z,POLISHED_BLACKSTONE_WALL);p(x,y+1,z,LANTERN);}
    private void plaza(){
        box(-15,-2,57,15,-2,92,POLISHED_BLACKSTONE_BRICKS);
        for(int z=58;z<=92;z++)for(int x=-12;x<=12;x++)p(x,-2,z,Math.abs(x)==10?YELLOW_TERRACOTTA:(z/3+x/3)%2==0?SMOOTH_QUARTZ:POLISHED_BLACKSTONE);
        for(int side:new int[]{-1,1}){
            for(int z:new int[]{63,77,88}){lamp(side*17,-1,z);}
            for(int z:new int[]{65,80}){
                b.disc(side*36,f-1,z,8,POLISHED_BLACKSTONE_BRICKS);
                b.disc(side*36,f,z,7,MOSS_BLOCK);
                ring(side*36,0,z,8,SMOOTH_QUARTZ);
                for(int a=0;a<360;a+=45){int x=side*36+(int)(Math.cos(Math.toRadians(a))*5),zz=z+(int)(Math.sin(Math.toRadians(a))*5);p(x,1,zz,AZALEA_LEAVES);p(x,2,zz,RED_TULIP);}
                sofa(side*25,-1,z);
            }
            // Elevated playing-card sculpture, diamond motif.
            int cx=side*58;
            box(cx-5,-1,59,cx+5,0,65,POLISHED_BLACKSTONE_BRICKS);
            box(cx-4,1,62,cx+4,13,63,SMOOTH_QUARTZ);
            for(int y=-4;y<=4;y++)for(int x=-3;x<=3;x++)if(Math.abs(x)+Math.abs(y)<=4)p(cx+x,7+y,64,RED_CONCRETE);
            for(int x=-4;x<=4;x++){p(cx+x,14,63,WAXED_CUT_COPPER);p(cx+x,1,64,WAXED_CUT_COPPER);}
        }
    }

    private void exteriorFinish(){
        // Exterior buttresses frame recessed windows above the wing roofs.
        for(int side:new int[]{-1,1}){
            for(int z=-32;z<=32;z+=8){
                box(side*50,1,z,side*51,29,z,QUARTZ_PILLAR);
                box(side*50,27,z-1,side*51,28,z+1,WAXED_CUT_COPPER);
                if(z<32){
                    box(side*49,20,z+2,side*49,26,z+5,BLACK_STAINED_GLASS);
                    box(side*48,20,z+2,side*48,26,z+5,OCHRE_FROGLIGHT);
                    box(side*50,19,z+2,side*50,19,z+5,SMOOTH_QUARTZ);
                }
            }
            int cx=side*68;
            for(int z=-16;z<=16;z+=8){
                box(cx+side*18,1,z,cx+side*18,17,z,QUARTZ_PILLAR);
                for(int dz=2;dz<=5;dz++){
                    box(cx+side*17,5,z+dz,cx+side*17,12,z+dz,YELLOW_STAINED_GLASS);
                    box(cx+side*16,5,z+dz,cx+side*16,12,z+dz,OCHRE_FROGLIGHT);
                }
            }
            for(int front:new int[]{-1,1})for(int dx=-8;dx<=8;dx+=8){
                box(cx+dx,1,front*25,cx+dx,17,front*25,QUARTZ_PILLAR);
                if(dx<8){
                    box(cx+dx+2,5,front*24,cx+dx+5,12,front*24,YELLOW_STAINED_GLASS);
                    box(cx+dx+2,5,front*23,cx+dx+5,12,front*23,OCHRE_FROGLIGHT);
                }
            }
            for(int tier=0;tier<3;tier++){
                int rx=17-tier*3,rz=24-tier*3;
                for(int x=-rx;x<=rx;x++)for(int z=-rz;z<=rz;z++){
                    if(Math.abs(x)+Math.abs(z)>rx+rz-7)continue;
                    boolean edge=Math.abs(x)==rx||Math.abs(z)==rz||Math.abs(x)+Math.abs(z)==rx+rz-7;
                    p(cx+x,19+tier,z,edge?WAXED_CUT_COPPER:RED_TERRACOTTA);
                }
            }
            for(int dx=-5;dx<=5;dx++)for(int dz=-8;dz<=8;dz++)if(Math.abs(dx)+Math.abs(dz)<=8)
                p(cx+dx,22,dz,Math.abs(dx)+Math.abs(dz)==8?SMOOTH_QUARTZ:BLACK_STAINED_GLASS);
        }
        // Rear elevation is part of the composition too.
        for(int x=-40;x<=40;x+=10){
            box(x,1,-43,x,28,-44,QUARTZ_PILLAR);
            if(x<40){box(x+2,7,-42,x+7,23,-42,RED_TERRACOTTA);box(x+4,10,-43,x+5,21,-43,YELLOW_STAINED_GLASS);}
        }
        // Fine roof fan ribs on the highest oval.
        for(int x=-24;x<=24;x++)for(int z=-18;z<=18;z++){
            double d=x*x/1444.0+z*z/900.0;
            if(d<.38&&(x==0||z==0||Math.abs(x)==Math.abs(z)))p(x,40,z,WAXED_CUT_COPPER);
        }
        // Restore the cross passages after adding exterior pilasters.
        for(int s:new int[]{-1,1})box(s*46,1,-3,s*57,7,3,AIR);
    }
}
