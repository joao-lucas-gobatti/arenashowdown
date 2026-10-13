package com.arenashowdown.parkour.wonder;

import org.bukkit.Material;
import static com.arenashowdown.parkour.wonder.WonderRoute.*;

/** Reusable authored sculptures. Components expose composition primitives, not generic room templates. */
final class WonderComponents {
    private final WonderBlocks blocks;private Wing wing=Wing.EASY;
    WonderComponents(WonderBlocks blocks){this.blocks=blocks;}
    void wing(Wing value){wing=value;}
    void p(int u,int h,int v,Material material){blocks.set(wing.x(u,v),FLOOR+h,wing.z(u,v),material);}
    void state(int u,int h,int v,Material material,String data){blocks.state(wing.x(u,v),FLOOR+h,wing.z(u,v),material,data);}
    void box(int u,int h,int v,int maxU,int maxH,int maxV,Material material){
        for(int x=Math.min(u,maxU);x<=Math.max(u,maxU);x++)for(int y=Math.min(h,maxH);y<=Math.max(h,maxH);y++)for(int z=Math.min(v,maxV);z<=Math.max(v,maxV);z++)p(x,y,z,material);
    }
    void disc(int u,int h,int v,int radius,Material material){
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)if(x*x+z*z<=radius*radius)p(u+x,h,v+z,material);
    }
    void ring(int u,int h,int v,int radius,Material material){
        int inner=(radius-1)*(radius-1);for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){int d=x*x+z*z;if(d<=radius*radius&&d>=inner)p(u+x,h,v+z,material);}
    }
    void verticalRing(int u,int h,int v,int radius,Material material){
        int inner=(radius-1)*(radius-1);for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++){int d=x*x+y*y;if(d<=radius*radius&&d>=inner)p(u+x,h+y,v,material);}
    }
    void chamferedDice(int u,int h,int v,int radius,Material shell,Material edge,Material pip){
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
            int cut=Math.max(0,Math.abs(x)+Math.abs(y)+Math.abs(z)-(3*radius-3));
            boolean surface=Math.abs(x)==radius||Math.abs(y)==radius||Math.abs(z)==radius;
            if(surface&&cut<=1)p(u+x,h+y,v+z,cut==1?edge:shell);
        }
        int q=Math.max(1,radius/2);
        for(int a:new int[]{-q,q})for(int b:new int[]{-q,q}){p(u+a,h+b,v-radius-1,pip);p(u+radius+1,h+a,v+b,pip);}
        p(u,h,v-radius-1,pip);p(u,h+q,v+radius+1,pip);p(u,h-q,v+radius+1,pip);
    }
    void orbitalDice(int u,int h,int v,int radius){
        chamferedDice(u,h,v,radius,Material.CALCITE,Material.CUT_COPPER,Material.PRISMARINE);
        ring(u,h,v,radius+4,Material.EXPOSED_COPPER);verticalRing(u,h,v,radius+6,Material.WAXED_OXIDIZED_COPPER);
        for(int angle=0;angle<360;angle+=30){double a=Math.toRadians(angle);int x=(int)Math.round((radius+8)*Math.cos(a)),z=(int)Math.round((radius+8)*Math.sin(a));
            p(u+x,h+((angle/30)%2),v+z,angle%60==0?Material.OCHRE_FROGLIGHT:Material.CUT_COPPER);}
    }
    void monumentalChandelier(int u,int top,int v){
        for(int y=top-8;y<=top;y++)p(u,y,v,Material.IRON_CHAIN);
        for(int tier=0;tier<3;tier++){
            int r=12-tier*3,h=top-10-tier*5;ring(u,h,v,r,tier==1?Material.EXPOSED_COPPER:Material.CUT_COPPER);
            for(int angle=0;angle<360;angle+=30){double a=Math.toRadians(angle);int x=(int)Math.round(r*Math.cos(a)),z=(int)Math.round(r*Math.sin(a));
                p(u+x,h-1,v+z,Material.END_ROD);p(u+x,h-2,v+z,tier==1?Material.VERDANT_FROGLIGHT:Material.OCHRE_FROGLIGHT);}
        }
    }
    void artDecoArch(int centerV,int halfWidth,int height,Material frame,Material inset){
        for(int u=-halfWidth;u<=halfWidth;u++){
            double normalized=u/(double)halfWidth;int top=height-(int)Math.round(8*normalized*normalized);
            for(int h=2;h<=top;h++)if(Math.abs(u)>=halfWidth-2||h>=top-2)p(u,h,centerV,h==top-1?inset:frame);
        }
    }
    void deepWindow(int side,int centerV,int halfWidth,int base,int height,Material frame,Material glass,Material light){
        for(int d=-halfWidth;d<=halfWidth;d++)for(int h=base;h<=base+height;h++){
            boolean rim=Math.abs(d)==halfWidth||h==base||h==base+height;
            p(side,d==0?base-1:h,centerV+d,rim?frame:glass);
            p(side+(side<0?-1:1),h,centerV+d,rim?frame:light);
        }
    }
    void curvedStaircase(int centerU,int centerV,int base,int radius,int steps,Material stair,Material rail){
        for(int i=0;i<steps;i++){
            double a=Math.toRadians(210+i*210.0/Math.max(1,steps-1));int u=centerU+(int)Math.round(radius*Math.cos(a)),v=centerV+(int)Math.round(radius*Math.sin(a));int h=base+i/2;
            disc(u,h,v,1,stair);p(centerU+(int)Math.round((radius+2)*Math.cos(a)),h+1,centerV+(int)Math.round((radius+2)*Math.sin(a)),rail);
        }
    }
    void ornamentalBridge(int fromU,int h,int fromV,int toU,int toV,Material deck,Material rail){
        int steps=Math.max(Math.abs(toU-fromU),Math.abs(toV-fromV));
        for(int i=0;i<=steps;i++){double t=i/(double)Math.max(1,steps);int u=(int)Math.round(fromU+(toU-fromU)*t),v=(int)Math.round(fromV+(toV-fromV)*t);int rise=(int)Math.round(2*Math.sin(Math.PI*t));
            p(u,h+rise,v,deck);if(i%2==0){p(u-1,h+rise+1,v,rail);p(u+1,h+rise+1,v,rail);}}
    }
    void fantasyTree(int u,int base,int v,int height,Material trunk,Material leaves,Material blossom){
        for(int h=0;h<height;h++){p(u,h+base,v,trunk);if(h>height/2&&h%2==0){p(u+1,h+base,v,trunk);p(u-1,h+base,v,trunk);}}
        for(int h=height-3;h<=height+3;h++){int r=Math.max(2,5-Math.abs(height-h));disc(u,base+h,v,r,(h&1)==0?leaves:blossom);}
        for(int a=0;a<8;a++){double angle=a*Math.PI/4;p(u+(int)Math.round(5*Math.cos(angle)),base+height-1,v+(int)Math.round(5*Math.sin(angle)),Material.END_ROD);}
    }
    void monumentalFlower(int u,int h,int v,int radius,Material petal,Material center){
        p(u,h-3,v,Material.MOSS_BLOCK);for(int y=h-2;y<h;y++)p(u,y,v,Material.BAMBOO_BLOCK);disc(u,h,v,2,center);
        for(int a=0;a<8;a++){double angle=a*Math.PI/4;int x=(int)Math.round(radius*Math.cos(angle)),z=(int)Math.round(radius*Math.sin(angle));disc(u+x,h,v+z,2,petal);}
    }
    void tieredFountain(int u,int h,int v,int radius,Material rim,Material waterGlass,Material accent){
        disc(u,h,v,radius,Material.CALCITE);ring(u,h+1,v,radius,rim);disc(u,h+1,v,radius-1,waterGlass);
        for(int tier=0;tier<3;tier++){int y=h+2+tier*3,r=Math.max(1,radius-2-tier);disc(u,y,v,r,accent);p(u,y+1,v,Material.SEA_LANTERN);}
    }
    void lanternArc(int u,int h,int v,int radius,Material frame,Material light){
        for(int x=-radius;x<=radius;x++){int y=h+(int)Math.round(Math.sqrt(Math.max(0,radius*radius-x*x)));p(u+x,y,v,frame);if(x%4==0){p(u+x,y-1,v,Material.IRON_CHAIN);p(u+x,y-2,v,light);}}
    }
    void monumentalCard(int u,int h,int v,int halfWidth,int height,Material face,Material border,Material suit){
        for(int x=-halfWidth;x<=halfWidth;x++)for(int y=0;y<=height;y++){
            boolean edge=Math.abs(x)==halfWidth||y==0||y==height;p(u+x,h+y,v,edge?border:face);
            if(!edge&&(Math.abs(x)==Math.max(1,halfWidth/2)&&y%5==2||x==0&&y==height/2))p(u+x,h+y,v-1,suit);
        }
        for(int x=-halfWidth+1;x<halfWidth;x+=2)p(u+x,h-1,v,Material.END_ROD);
    }
    void cardCastle(int u,int h,int v){
        for(int tier=0;tier<3;tier++){int y=h+tier*9,spread=18-tier*6;
            for(int x=-spread;x<=spread;x+=12){monumentalCard(u+x,y,v+tier*2,4,10,Material.QUARTZ_BLOCK,Material.GOLD_BLOCK,tier%2==0?Material.CYAN_TERRACOTTA:Material.PURPLE_TERRACOTTA);}
            ornamentalBridge(u-spread,y+10,v+tier*2,u+spread,v+tier*2,Material.SMOOTH_QUARTZ,Material.END_ROD);
        }
        p(u,h+32,v+4,Material.SEA_LANTERN);
    }
    void gear(int u,int h,int v,int radius,Material rim,Material hub){
        verticalRing(u,h,v,radius,rim);verticalRing(u,h,v,radius-2,hub);disc(u,h,v,2,hub);
        for(int a=0;a<360;a+=30){double angle=Math.toRadians(a);int x=(int)Math.round((radius+1)*Math.cos(angle)),y=(int)Math.round((radius+1)*Math.sin(angle));box(u+x-1,h+y-1,v,u+x+1,h+y+1,v,rim);}
        for(int a=0;a<360;a+=45){double angle=Math.toRadians(a);for(int r=2;r<radius;r++)p(u+(int)Math.round(r*Math.cos(angle)),h+(int)Math.round(r*Math.sin(angle)),v,hub);}
    }
    void clockFace(int u,int h,int v,int radius,Material face,Material frame,Material hand){
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++){int d=x*x+y*y;if(d<=radius*radius)p(u+x,h+y,v,d>=(radius-2)*(radius-2)?frame:face);}
        for(int hour=0;hour<12;hour++){double a=Math.toRadians(hour*30);p(u+(int)Math.round((radius-2)*Math.sin(a)),h+(int)Math.round((radius-2)*Math.cos(a)),v-1,Material.SEA_LANTERN);}
        for(int i=0;i<radius-3;i++){p(u+i,h+i/2,v-2,hand);p(u-i/3,h+i,v-2,hand);}disc(u,h,v-2,2,Material.GOLD_BLOCK);
    }
    void theatreStage(int u,int h,int v,int halfWidth,Material deck,Material trim){
        for(int z=0;z<12;z++){int width=halfWidth-z/2;box(u-width,h+z/4,v+z,u+width,h+z/4,v+z,deck);}
        for(int x=-halfWidth;x<=halfWidth;x+=4){for(int y=1;y<=18;y++)p(u+x,h+y,v+13,y%6==0?trim:Material.DARK_OAK_LOG);p(u+x,h+19,v+13,Material.OCHRE_FROGLIGHT);}
        ornamentalBridge(u-halfWidth,h+12,v+10,u+halfWidth,v+10,trim,Material.IRON_CHAIN);
    }
    void theatreCurtain(int u,int h,int v,int halfWidth,int height,Material cloth,Material braid){
        for(int x=-halfWidth;x<=halfWidth;x++){int swag=(int)Math.round(4*Math.sin(Math.PI*(x+halfWidth)/(halfWidth*2.0)));for(int y=swag;y<height;y++)if((x+halfWidth)%4<3)p(u+x,h+y,v,cloth);p(u+x,h+swag,v-1,braid);}
        for(int x:new int[]{-halfWidth,halfWidth})for(int y=0;y<=height;y++)p(u+x,h+y,v-1,braid);
    }
    void theatreBalcony(int u,int h,int v,int halfWidth,Material deck,Material rail){
        for(int x=-halfWidth;x<=halfWidth;x++){int depth=3+(int)Math.round(2*Math.cos(Math.PI*x/(halfWidth*2.0)));for(int z=0;z<=depth;z++)p(u+x,h,v+z,deck);p(u+x,h+2,v+depth,rail);if(x%4==0)p(u+x,h+1,v+depth,Material.IRON_BARS);}
        for(int x=-halfWidth;x<=halfWidth;x+=6)for(int y=1;y<h;y++)p(u+x,y,v+1,Material.POLISHED_DEEPSLATE);
    }
    void pipeNetwork(int u,int h,int v,int span,Material pipe,Material joint){
        for(int arm=0;arm<4;arm++){int direction=arm%2==0?1:-1;for(int i=0;i<=span;i++){int x=arm<2?u+direction*i:u+(int)Math.round(5*Math.sin(i/3.0));int y=h+(arm<2?i/4:i);int z=arm<2?v+(int)Math.round(4*Math.sin(i/2.0)):v+direction*i;p(x,y,z,pipe);if(i%5==0){ring(x,y,z,1,joint);p(x,y+1,z,Material.REDSTONE_LAMP);}}}
    }
    void mechanicalWheel(int u,int h,int v,int radius,Material rim,Material spoke){
        gear(u,h,v,radius,rim,spoke);for(int r=radius-4;r>2;r-=4)verticalRing(u,h,v-1,r,spoke);
        for(int a=0;a<360;a+=45){double angle=Math.toRadians(a);int x=(int)Math.round((radius-3)*Math.cos(angle)),y=(int)Math.round((radius-3)*Math.sin(angle));disc(u+x,h+y,v-2,1,Material.REDSTONE_LAMP);}
    }
    void cloudIsland(int u,int h,int v,int radius,Material top,Material cloud){
        for(int y=0;y<4;y++)disc(u,h-y,v,Math.max(2,radius-y),y==0?top:cloud);
        for(int a=0;a<360;a+=60){double angle=Math.toRadians(a);disc(u+(int)Math.round(radius*Math.cos(angle)),h-2,v+(int)Math.round(radius*Math.sin(angle)),radius/2+1,cloud);}
        for(int y=4;y<10;y++)if(y%2==0)p(u,h-y,v,Material.END_ROD);
    }
    void mirrorPrism(int u,int h,int v,int radius,int height,Material frame,Material glass){
        for(int y=0;y<=height;y++){int r=Math.max(2,radius-Math.abs(height/2-y)/4);for(int a=0;a<6;a++){double angle=Math.toRadians(a*60);int x=(int)Math.round(r*Math.cos(angle)),z=(int)Math.round(r*Math.sin(angle));p(u+x,h+y,v+z,a%2==0?frame:glass);if(y%6==0)p(u+x,h+y,v+z-1,Material.SEA_LANTERN);}}
        for(int y=0;y<=height;y++)p(u,h+y,v,y%3==0?Material.AMETHYST_BLOCK:Material.END_ROD);
    }
    void crystalCluster(int u,int h,int v,int radius,Material crystal,Material base){
        disc(u,h,v,radius,base);for(int a=0;a<8;a++){double angle=Math.toRadians(a*45);int x=(int)Math.round((radius-1)*Math.cos(angle)),z=(int)Math.round((radius-1)*Math.sin(angle)),height=4+(a%4)*2;for(int y=1;y<=height;y++){int drift=y/4;p(u+x+(int)Math.signum(x)*drift,h+y,v+z+(int)Math.signum(z)*drift,y==height?Material.SEA_LANTERN:crystal);}}
    }
    void fragmentedDice(int u,int h,int v,int radius,Material shell,Material edge,Material light){
        int[][] drift={{-3,2,-2},{3,1,-2},{-2,-2,3},{3,3,2},{0,-3,-3}};
        for(int i=0;i<drift.length;i++){int dx=drift[i][0],dy=drift[i][1],dz=drift[i][2],r=Math.max(2,radius/2);chamferedDice(u+dx,h+dy,v+dz,r,shell,edge,light);for(int k=0;k<5;k++)p(u+dx*k/5,h+dy*k/5,v+dz*k/5,k%2==0?Material.END_ROD:light);}
    }
    void fracturedRings(int u,int h,int v,int radius,Material first,Material second){
        for(int r=radius;r>=radius-8;r-=4)for(int a=0;a<360;a+=8){if((a/24+r)%4==0)continue;double angle=Math.toRadians(a);int x=(int)Math.round(r*Math.cos(angle)),y=(int)Math.round(r*Math.sin(angle));p(u+x,h+y+(a%16==0?1:0),v+(radius-r)/2,(a/8)%2==0?first:second);}
    }
    void impossibleStaircase(int u,int h,int v,int radius,int steps,Material stair,Material rail){
        for(int i=0;i<steps;i++){int side=(i/8)%4,along=(i%8)-4,x=u,z=v,y=h+i/3;
            if(side==0){x+=along;z-=radius;}else if(side==1){x+=radius;z+=along;}else if(side==2){x-=along;z+=radius;}else{x-=radius;z-=along;}
            disc(x,y,z,1,stair);p(x,y+2,z,Material.END_ROD);if(i%3==0)p(x,y-1,z,rail);
        }
    }
    void palaceArches(int u,int h,int v,int count,Material pillar,Material glass){
        for(int i=0;i<count;i++){int z=v+i*7;for(int x:new int[]{-16,16})for(int y=0;y<22;y++)p(u+x,h+y,z,y%5==0?glass:pillar);for(int x=-16;x<=16;x++){int y=18+(int)Math.round(7*Math.sqrt(Math.max(0,1-Math.pow(x/16.0,2))));p(u+x,h+y,z,pillar);if(x%4==0)p(u+x,h+y-1,z,glass);}}
    }
    void crown(int u,int h,int v,int radius,int points,Material body,Material jewel){
        for(int y=0;y<8;y++)ring(u,h+y,v,radius-(y/4),y%3==0?Material.GOLD_BLOCK:body);
        for(int a=0;a<points;a++){double angle=2*Math.PI*a/points;int x=(int)Math.round((radius-1)*Math.cos(angle)),z=(int)Math.round((radius-1)*Math.sin(angle)),height=14+(a%2)*7;for(int y=8;y<=height;y++){int inward=(y-8)/7;p(u+(int)Math.round((radius-1-inward)*Math.cos(angle)),h+y,v+(int)Math.round((radius-1-inward)*Math.sin(angle)),y==height?jewel:body);}}
        for(int r=3;r<radius;r+=4)ring(u,h+10,v,r,jewel);
    }
    void raysAndSpiral(int u,int h,int v,int radius,int turns,Material ray,Material light){
        for(int a=0;a<360;a+=30)for(int r=2;r<=radius;r++){double angle=Math.toRadians(a);p(u+(int)Math.round(r*Math.cos(angle)),h+(r%3),v+(int)Math.round(r*Math.sin(angle)),r==radius?light:ray);}
        for(int i=0;i<turns*32;i++){double angle=i*Math.PI/16;int r=5+i/16;p(u+(int)Math.round(r*Math.cos(angle)),h+i/8,v+(int)Math.round(r*Math.sin(angle)),i%5==0?light:ray);}
    }
    void toyDiceTower(int u,int h,int v,Material shell,Material edge,Material pip){
        for(int tier=0;tier<4;tier++){int r=6-tier,x=u+(tier%2==0?-3:3),y=h+tier*13,z=v+(tier%2==0?2:-2);chamferedDice(x,y,z,r,shell,edge,pip);ring(x,y-6,z,r+2,tier%2==0?Material.PINK_TERRACOTTA:Material.LIGHT_BLUE_TERRACOTTA);}
    }
    /** Wide, illuminated transition passage between authored chapters; walkable at one-block rises. */
    void chapterConnector(int fromU,int fromY,int fromV,int toU,int toY,int toV,Material deck,Material rail,Material light){
        int fromH=fromY-FLOOR,toH=toY-FLOOR;
        int steps=Math.max(Math.max(Math.abs(toU-fromU),Math.abs(toV-fromV)),Math.abs(toH-fromH));
        for(int i=0;i<=steps;i++){
            double t=i/(double)Math.max(1,steps);int u=(int)Math.round(fromU+(toU-fromU)*t),v=(int)Math.round(fromV+(toV-fromV)*t),h=(int)Math.round(fromH+(toH-fromH)*t);
            disc(u,h,v,1,deck);box(u-1,h+1,v-1,u+1,h+3,v+1,Material.AIR);
            if(i%4==0){p(u-2,h+1,v,rail);p(u+2,h+1,v,rail);p(u-2,h+2,v,light);p(u+2,h+2,v,light);}
            if(i%7==0)ring(u,h-1,v,2,rail);
        }
    }
    void amphitheatre(int u,int h,int v,int radius,Material seat,Material aisle){
        for(int tier=0;tier<6;tier++){int r=radius-tier*3,y=h+tier*2;for(int a=20;a<=340;a+=3){double angle=Math.toRadians(a);int x=(int)Math.round(r*Math.cos(angle)),z=(int)Math.round(r*Math.sin(angle));p(u+x,y,v+z,a%30<4?aisle:seat);}}
        disc(u,h,v,8,Material.SMOOTH_SANDSTONE);ring(u,h+1,v,9,aisle);
    }
}
