package com.arenashowdown.parkour.wonder;

import java.util.*;
import com.arenashowdown.parkour.arena.ArenaPoint;
import com.arenashowdown.parkour.arena.BlockRegion;
import com.arenashowdown.parkour.arena.ParkourArena;

/** Versioned, deterministic gameplay blueprint. Coordinates are local: u lateral, v away from atrium. */
public final class WonderRoute {
    public static final String WORLD="house_of_wonders_v3";
    public static final String SCHEMA="house-of-wonders-v3.1";
    public static final int FLOOR=64;
    public static final int TRANSITIONS_PER_STAGE=42;
    public static final int LANDINGS_PER_STAGE=TRANSITIONS_PER_STAGE+1;

    public enum Wing {
        EASY("Candy Casino",0,-1,3,2.55,1,2),MEDIUM("Celestial Card Palace",1,0,2,2.8,1,2),
        HARD("Crimson Clockwork",-1,0,1,3.0,1,3),IMPOSSIBLE("Royal Dice Dimension",0,1,1,3.15,1,3);
        public final String title;public final int dx,dz,defaultWidth;public final double maxEdgeGap;public final int maxRise,maxDrop;
        Wing(String title,int dx,int dz,int defaultWidth,double maxEdgeGap,int maxRise,int maxDrop){this.title=title;this.dx=dx;this.dz=dz;this.defaultWidth=defaultWidth;this.maxEdgeGap=maxEdgeGap;this.maxRise=maxRise;this.maxDrop=maxDrop;}
        public int x(int u,int v){return -dz*u+dx*v;}
        public int z(int u,int v){return dx*u+dz*v;}
        public float yaw(){return dx==1?-90:dx==-1?90:dz==-1?180:0;}
        public String arenaId(){return "wonder-"+name().toLowerCase(Locale.ROOT);}
    }
    public enum Obstacle {
        START,CHIP,ORNAMENTAL_BRIDGE,FLOWER_CROWN,FOUNTAIN_RIM,CURVED_STEP,DICE_LEDGE,
        TOY_BLOCK,CARD_EDGE,CLOUD,GEAR_TOOTH,CLOCK_HAND,MIRROR_FRAME,CRYSTAL,
        THEATRE_BEAM,BALCONY,PIPE,ROULETTE_RIM,STATUE_LEDGE,RING,FRAGMENT,
        PALACE_STEP,CROWN_POINT,SLAB,TRAPDOOR,LADDER,HONEY,SLIME,FINISH
    }
    public record Landing(int globalIndex,int stage,int transition,int u,int v,int y,int width,
                          Obstacle obstacle,boolean checkpoint,String beat){
        public int radius(){return Math.max(0,(width-1)/2);}
        public int minOffset(){return -(width/2);}
        public int maxOffset(){return (width-1)/2;}
        public ArenaPoint point(Wing wing){return new ArenaPoint(WORLD,wing.x(u,v)+.5,y+1,wing.z(u,v)+.5,wing.yaw(),0);}
        public BlockRegion region(Wing wing){
            int min=minOffset(),max=maxOffset();return BlockRegion.between(WORLD,wing.x(u+min,v+min),y+1,wing.z(u+min,v+min),wing.x(u+max,v+max),y+1,wing.z(u+max,v+max));
        }
    }
    /** Metadata and immutable landing sequence for one authored chapter. */
    public record Stage(Wing wing,int number,String name,String layout,String landmark,List<Landing> landings){
        public Stage{landings=List.copyOf(landings);}
        public int transitions(){return landings.size()-1;}
    }
    private record Point(int u,int v,int y) {}
    private static final EnumMap<Wing,List<Stage>> STAGES=createStages();
    private WonderRoute(){}

    public static List<Stage> stages(Wing wing){return STAGES.get(Objects.requireNonNull(wing));}
    public static Stage stage(Wing wing,int number){
        if(number<1||number>3)throw new IllegalArgumentException("stage must be 1..3");
        return stages(wing).get(number-1);
    }
    public static List<Landing> route(Wing wing){
        List<Landing> result=new ArrayList<>();
        for(Stage stage:stages(wing))result.addAll(stage.number()==1?stage.landings():stage.landings().subList(1,stage.landings().size()));
        return List.copyOf(result);
    }
    public static String stageName(Wing wing,int number){return stage(wing,number).name();}
    public static ParkourArena arena(Wing wing){
        List<Landing> nodes=route(wing);Landing start=nodes.getFirst();List<ArenaPoint> spawns=new ArrayList<>();
        for(int du=-1;du<=1;du++)for(int dv=-1;dv<=1;dv++)spawns.add(new ArenaPoint(WORLD,wing.x(start.u()+du,start.v()+dv)+.5,start.y()+1,wing.z(start.u()+du,start.v()+dv)+.5,wing.yaw(),0));
        List<BlockRegion> progress=nodes.subList(1,nodes.size()).stream().map(node->node.region(wing)).toList();
        String safeIndexes=java.util.stream.IntStream.range(1,nodes.size()).filter(index->nodes.get(index).checkpoint()).mapToObj(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        return new ParkourArena(wing.arenaId(),WORLD,start.point(wing),spawns,progress,nodes.getLast().region(wing),
            new ArenaPoint(WORLD,.5,FLOOR+1,8.5,180,0),1800,57,
            Map.of("wonder",true,"difficulty",wing.name(),"stages",3,"schema",SCHEMA,
                "safe-checkpoint-indexes",safeIndexes));
    }

    /** Complete approved prototype route: Easy / The Confetti Garden. */
    public static List<Landing> prototypeGarden(){return stage(Wing.EASY,1).landings();}

    private static EnumMap<Wing,List<Stage>> createStages(){
        EnumMap<Wing,List<Stage>> result=new EnumMap<>(Wing.class);
        result.put(Wing.EASY,List.of(
            authored(Wing.EASY,1,"Confetti Garden","serpentine-garden","chip fountain tree",gardenPoints(),
                obstacles(Obstacle.CHIP,Obstacle.ORNAMENTAL_BRIDGE,Obstacle.FLOWER_CROWN,Obstacle.CHIP,Obstacle.FOUNTAIN_RIM,Obstacle.CURVED_STEP,Obstacle.DICE_LEDGE)),
            authored(Wing.EASY,2,"Giant Toy Room","ascending-toy-orbit","toy dice tower",orbitPoints(0,127,17,104,12,0),
                obstacles(Obstacle.TOY_BLOCK,Obstacle.DICE_LEDGE,Obstacle.CURVED_STEP,Obstacle.GEAR_TOOTH,Obstacle.CHIP,Obstacle.SLAB,Obstacle.CLOCK_HAND,Obstacle.ORNAMENTAL_BRIDGE)),
            authored(Wing.EASY,3,"Rainbow Ballroom","perimeter-galleries","rainbow chandelier",perimeterPoints(166,200,69),
                obstacles(Obstacle.BALCONY,Obstacle.SLAB,Obstacle.CURVED_STEP,Obstacle.RING,Obstacle.CHIP,Obstacle.CLOCK_HAND,Obstacle.DICE_LEDGE,Obstacle.ORNAMENTAL_BRIDGE))));
        result.put(Wing.MEDIUM,List.of(
            authored(Wing.MEDIUM,1,"Floating Card Kingdom","card-tower-zigzag","suspended card castle",zigzagPoints(48,3,10,73),
                obstacles(Obstacle.CARD_EDGE,Obstacle.CLOUD,Obstacle.SLAB,Obstacle.CRYSTAL,Obstacle.RING,Obstacle.BALCONY,Obstacle.DICE_LEDGE,Obstacle.ORNAMENTAL_BRIDGE)),
            authored(Wing.MEDIUM,2,"Sky Clockwork","clock-rings-spiral","monumental sky clock",orbitPoints(0,127,16,78,10,1),
                obstacles(Obstacle.GEAR_TOOTH,Obstacle.CLOCK_HAND,Obstacle.RING,Obstacle.SLAB,Obstacle.PIPE,Obstacle.CARD_EDGE,Obstacle.BALCONY,Obstacle.CRYSTAL)),
            authored(Wing.MEDIUM,3,"Celestial Mirror Hall","mirrored-reversals","axial mirror prism",mirrorPoints(162,82),
                obstacles(Obstacle.MIRROR_FRAME,Obstacle.CRYSTAL,Obstacle.SLAB,Obstacle.CARD_EDGE,Obstacle.RING,Obstacle.CLOCK_HAND,Obstacle.CLOUD,Obstacle.BALCONY))));
        result.put(Wing.HARD,List.of(
            authored(Wing.HARD,1,"Broken Theatre","beams-and-balconies","forty-block broken stage",beamPoints(49,76),
                obstacles(Obstacle.THEATRE_BEAM,Obstacle.BALCONY,Obstacle.SLAB,Obstacle.PIPE,Obstacle.STATUE_LEDGE,Obstacle.CLOCK_HAND,Obstacle.TRAPDOOR,Obstacle.RING)),
            authored(Wing.HARD,2,"Mechanical Wonder","vertical-mechanism","vertical roulette machine",mechanismPoints(0,127,9,84),
                obstacles(Obstacle.PIPE,Obstacle.ROULETTE_RIM,Obstacle.GEAR_TOOTH,Obstacle.CLOCK_HAND,Obstacle.SLAB,Obstacle.THEATRE_BEAM,Obstacle.TRAPDOOR,Obstacle.BALCONY)),
            authored(Wing.HARD,3,"Grand Showdown","amphitheatre-bowl","multilevel showdown arena",amphitheatrePoints(183,92),
                obstacles(Obstacle.STATUE_LEDGE,Obstacle.BALCONY,Obstacle.THEATRE_BEAM,Obstacle.RING,Obstacle.SLAB,Obstacle.PIPE,Obstacle.TRAPDOOR,Obstacle.ROULETTE_RIM))));
        result.put(Wing.IMPOSSIBLE,List.of(
            authored(Wing.IMPOSSIBLE,1,"Fractured Reality","fragmented-orbits","fractured colossal die",fragmentPoints(48,98),
                obstacles(Obstacle.FRAGMENT,Obstacle.RING,Obstacle.TRAPDOOR,Obstacle.CRYSTAL,Obstacle.CROWN_POINT,Obstacle.SLAB,Obstacle.CLOCK_HAND,Obstacle.DICE_LEDGE)),
            authored(Wing.IMPOSSIBLE,2,"Infinite Palace","multi-floor-reversals","impossible staircase",palacePoints(105,108),
                obstacles(Obstacle.PALACE_STEP,Obstacle.MIRROR_FRAME,Obstacle.TRAPDOOR,Obstacle.FRAGMENT,Obstacle.CARD_EDGE,Obstacle.CRYSTAL,Obstacle.RING,Obstacle.CROWN_POINT)),
            authored(Wing.IMPOSSIBLE,3,"Crown of Dice","crown-ascent-spiral","fifty-six-block crown",crownPoints(),
                obstacles(Obstacle.CROWN_POINT,Obstacle.TRAPDOOR,Obstacle.RING,Obstacle.FRAGMENT,Obstacle.PALACE_STEP,Obstacle.CRYSTAL,Obstacle.CLOCK_HAND,Obstacle.FINISH))));
        result.replaceAll((wing,value)->List.copyOf(value));return result;
    }

    private static Stage authored(Wing wing,int number,String name,String layout,String landmark,List<Point> points,Obstacle[] rhythm){
        if(points.size()!=LANDINGS_PER_STAGE)throw new IllegalStateException(name+" must have 43 landings");
        int offset=wing.ordinal()*3*LANDINGS_PER_STAGE+(number-1)*LANDINGS_PER_STAGE;
        List<Landing> nodes=new ArrayList<>(LANDINGS_PER_STAGE);
        for(int i=0;i<points.size();i++){
            Point p=points.get(i);boolean checkpoint=i==14||i==28||i==42;
            Obstacle obstacle=i==0?Obstacle.START:(number==3&&i==42?Obstacle.FINISH:rhythm[(i-1)%rhythm.length]);
            String beat=i==0?slug(name)+"-entry":slug(name)+(i<=14?"-opening":i<=28?"-turn":"-finale");
            int width=wing.defaultWidth;
            if(i==0)width=5;
            else if(wing==Wing.EASY&&number==3)width=2;
            else if(wing==Wing.EASY&&i%9==0)width=4;
            nodes.add(new Landing(offset+i,number,i,p.u,p.v,p.y,width,obstacle,checkpoint,beat));
        }
        validate(wing,name,nodes);return new Stage(wing,number,name,layout,landmark,nodes);
    }
    private static void validate(Wing wing,String name,List<Landing> nodes){
        Set<String> occupied=new HashSet<>();
        for(int i=0;i<nodes.size();i++){
            Landing node=nodes.get(i);
            if(!occupied.add(node.u+":"+node.v+":"+node.y))throw new IllegalStateException(name+" duplicate landing "+i);
            if(Math.abs(node.u)>44||node.v<42||node.v>216||node.y<58||node.y>180)throw new IllegalStateException(name+" outside route bounds at "+i);
            if(i==0)continue;Landing previous=nodes.get(i-1);int rise=node.y-previous.y;
            if(edgeGap(previous,node)>wing.maxEdgeGap+.001||rise>wing.maxRise||rise< -wing.maxDrop)
                throw new IllegalStateException(name+" invalid transition "+i+" gap="+edgeGap(previous,node)+" rise="+rise);
        }
    }
    private static Obstacle[] obstacles(Obstacle... values){return values;}
    private static String slug(String value){return value.toLowerCase(Locale.ROOT).replace(' ','-');}

    private static List<Point> gardenPoints(){
        List<Point> points=new ArrayList<>();int base=68;points.add(new Point(-20,48,base));
        for(int row=0;row<6;row++)for(int step=0;step<7;step++){int col=row%2==0?step:6-step;
            points.add(new Point(-15+col*5,52+row*5,base+row/2+((step==2||step==5)?1:0)));}
        return points;
    }
    private static List<Point> orbitPoints(int centerU,int centerV,int radius,int baseY,int degrees,int phase){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            double angle=Math.toRadians(-90+phase*35+i*degrees);int r=radius+(i/14)*2;
            points.add(new Point(centerU+(int)Math.round(r*Math.cos(angle)),centerV+(int)Math.round(r*Math.sin(angle)),baseY+i/7));}
        return points;
    }
    private static List<Point> perimeterPoints(int minV,int maxV,int baseY){
        int[][] vertices={{-20,minV},{20,minV},{20,maxV},{-20,maxV},{-20,minV+8}};
        return samplePolyline(vertices,baseY,1);
    }
    private static List<Point> zigzagPoints(int startV,int du,int period,int baseY){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            int cycle=i%(period*2),u=-15+(cycle<=period?cycle:period*2-cycle)*du;
            points.add(new Point(u,startV+i,baseY+i/9+(i%9==5?1:0)));}
        return points;
    }
    private static List<Point> mirrorPoints(int startV,int baseY){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            int cycle=i%24,u=-18+(cycle<=12?cycle:24-cycle)*3;
            points.add(new Point(u,startV+i,baseY+i/7+(i%7==3?1:0)));}
        return points;
    }
    private static List<Point> beamPoints(int startV,int baseY){
        List<Point> points=new ArrayList<>();int u=-27,v=startV,y=baseY;points.add(new Point(u,v,y));
        int[][] directions={{3,0},{0,3},{-3,0},{0,3},{3,0},{0,3}};
        for(int i=1;i<LANDINGS_PER_STAGE;i++){int[] d=directions[((i-1)/7)%directions.length];u+=d[0];v+=d[1];if(i%6==0)y++;if(i%13==0)y-=2;points.add(new Point(u,v,y));}
        return points;
    }
    private static List<Point> mechanismPoints(int centerU,int centerV,int radius,int baseY){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            double angle=Math.toRadians(-90+i*18);int r=radius+(i%12<6?0:2);
            points.add(new Point(centerU+(int)Math.round(r*Math.cos(angle)),centerV+(int)Math.round(r*Math.sin(angle)),baseY+i/2));}
        return points;
    }
    private static List<Point> amphitheatrePoints(int centerV,int baseY){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            int tier=i/14,r=18-tier*3;double angle=Math.toRadians(200+(i%14)*11+tier*145);
            points.add(new Point((int)Math.round(r*Math.cos(angle)),centerV+(int)Math.round(r*Math.sin(angle)),baseY+tier*3+(i%14)/5));}
        return points;
    }
    private static List<Point> fragmentPoints(int startV,int baseY){
        List<Point> points=new ArrayList<>();int u=-24,v=startV,y=baseY;points.add(new Point(u,v,y));
        for(int i=1;i<LANDINGS_PER_STAGE;i++){int mode=i%4;if(mode==0)u+=3;else if(mode==2)u-=3;else{u+=mode==1?2:-2;v+=2;}if(i%4==0)v+=1;if(i%5==0)y++;if(i%11==0)y-=2;points.add(new Point(u,v,y));}
        return points;
    }
    private static List<Point> palacePoints(int startV,int baseY){
        List<Point> points=new ArrayList<>();int u=-24,v=startV,y=baseY;points.add(new Point(u,v,y));
        for(int i=1;i<LANDINGS_PER_STAGE;i++){int floor=(i-1)/14,step=(i-1)%14;u+=(floor%2==0?3:-3);if(step==6||step==13){v+=3;y+=1;}else if(i%3==0)v+=2;if(i%2==0)y++;if(i%7==0)y-=2;points.add(new Point(u,v,y));}
        return points;
    }
    private static List<Point> crownPoints(){
        List<Point> points=new ArrayList<>();for(int i=0;i<LANDINGS_PER_STAGE;i++){
            double angle=Math.toRadians(-120+i*12);int radius=18-(i/14)*2;
            points.add(new Point((int)Math.round(radius*Math.cos(angle)),188+(int)Math.round(radius*Math.sin(angle)),132+i));}
        return points;
    }
    private static List<Point> samplePolyline(int[][] vertices,int baseY,int riseEvery){
        double[] lengths=new double[vertices.length-1];double total=0;for(int i=0;i<lengths.length;i++){lengths[i]=Math.hypot(vertices[i+1][0]-vertices[i][0],vertices[i+1][1]-vertices[i][1]);total+=lengths[i];}
        List<Point> points=new ArrayList<>();for(int n=0;n<LANDINGS_PER_STAGE;n++){
            double target=total*n/TRANSITIONS_PER_STAGE,walked=0;int segment=0;while(segment<lengths.length-1&&walked+lengths[segment]<target){walked+=lengths[segment++];}
            double t=(target-walked)/lengths[segment];int u=(int)Math.round(vertices[segment][0]+(vertices[segment+1][0]-vertices[segment][0])*t);int v=(int)Math.round(vertices[segment][1]+(vertices[segment+1][1]-vertices[segment][1])*t);
            points.add(new Point(u,v,baseY+(riseEvery==0?0:n/14)));}
        return points;
    }

    public static double edgeGap(Landing from,Landing to){
        double du=axisGap(from.u,from.minOffset(),from.maxOffset(),to.u,to.minOffset(),to.maxOffset());
        double dv=axisGap(from.v,from.minOffset(),from.maxOffset(),to.v,to.minOffset(),to.maxOffset());
        return Math.hypot(du,dv);
    }
    private static double axisGap(int from,int fromMin,int fromMax,int to,int toMin,int toMax){
        double leftA=from+fromMin-.5,rightA=from+fromMax+.5,leftB=to+toMin-.5,rightB=to+toMax+.5;
        if(rightA<leftB)return leftB-rightA;if(rightB<leftA)return leftA-rightB;return 0;
    }
}
