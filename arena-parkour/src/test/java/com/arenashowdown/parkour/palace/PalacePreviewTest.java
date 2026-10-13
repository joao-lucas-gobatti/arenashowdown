package com.arenashowdown.parkour.palace;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import static com.arenashowdown.parkour.palace.PalaceRoute.*;

/** Simplified, exact-voxel diagnostic views; never presented as in-game screenshots. */
class PalacePreviewTest {
    @Test void renderEveryChamber()throws Exception{
        var all=PalaceArchitecture.build().ordered();
        Files.createDirectories(Path.of("build/palace-previews"));
        for(boolean atrium:new boolean[]{false,true}){
            var image=new BufferedImage(1600,1000,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
            g.setColor(new Color(22,24,33));g.fillRect(0,0,1600,1000);
            var overview=new ArrayList<>(all);
            overview.sort(Comparator.comparingInt((PalaceBlocks.Block a)->a.pos().x()+a.pos().z()).thenComparingInt(a->a.pos().y()));
            int scale=atrium?8:3;
            for(var block:overview){var p=block.pos();
                if(block.material()==Material.AIR||(atrium&&(Math.abs(p.x())>33||Math.abs(p.z())>33||p.y()>103||p.z()>28)))continue;
                int x=800+(p.x()-p.z())*scale,y=600+(p.x()+p.z())*scale/2-(p.y()-64)*scale;
                Color c=color(block.material());g.setColor(c.darker());g.fillPolygon(new int[]{x-scale,x,x,x-scale},new int[]{y,y+scale/2,y+scale*3/2,y+scale},4);
                g.setColor(c.darker().darker());g.fillPolygon(new int[]{x,x+scale,x+scale,x},new int[]{y+scale/2,y,y+scale,y+scale*3/2},4);
                g.setColor(c);g.fillPolygon(new int[]{x-scale,x,x+scale,x},new int[]{y,y-scale/2,y,y+scale/2},4);
            }
            g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,22));g.drawString((atrium?"ATRIUM CUTAWAY":"PALACE EXTERIOR")+" — GENERATOR PROJECTION, NOT A MINECRAFT SCREENSHOT",20,35);
            g.dispose();ImageIO.write(image,"png",Path.of("build/palace-previews/"+(atrium?"atrium":"exterior")+".png").toFile());
        }
        for(Wing wing:Wing.values())for(int stage=0;stage<3;stage++){
            int center=46+stage*28;
            var image=new BufferedImage(1200,850,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
            g.setColor(new Color(22,24,33));g.fillRect(0,0,1200,850);
            var blocks=new ArrayList<PalaceBlocks.Block>();
            for(var block:all){var p=block.pos();int u=-wing.dz*p.x()+wing.dx*p.z(),v=wing.dx*p.x()+wing.dz*p.z();
                if(Math.abs(u)<=23&&Math.abs(v-center)<=13&&p.y()<102&&block.material()!=Material.AIR)blocks.add(block);
            }
            blocks.sort(Comparator.comparingInt((PalaceBlocks.Block a)->(-wing.dz*a.pos().x()+wing.dx*a.pos().z())+(wing.dx*a.pos().x()+wing.dz*a.pos().z())).thenComparingInt(a->a.pos().y()));
            for(var block:blocks){var p=block.pos();int u=-wing.dz*p.x()+wing.dx*p.z(),v=wing.dx*p.x()+wing.dz*p.z()-center;
                int x=600+(u-v)*11,y=600+(u+v)*5-(p.y()-64)*10;
                Color c=color(block.material());
                g.setColor(c.darker());g.fillPolygon(new int[]{x-11,x,x,x-11},new int[]{y,y+5,y+15,y+10},4);
                g.setColor(c.darker().darker());g.fillPolygon(new int[]{x,x+11,x+11,x},new int[]{y+5,y,y+10,y+15},4);
                g.setColor(c);g.fillPolygon(new int[]{x-11,x,x+11,x},new int[]{y,y-5,y,y+5},4);
            }
            // Annotate landings clearly above the scenery projection.
            g.setFont(new Font("SansSerif",Font.BOLD,18));g.setColor(Color.WHITE);
            for(var n:landings(wing))if(n.stage()==stage+1){int x=600+(n.u()-(n.v()-center))*11,y=600+(n.u()+n.v()-center)*5-(n.y()-64)*10;g.drawString(""+n.index(),x,y-18);}
            g.setFont(new Font("SansSerif",Font.BOLD,20));g.drawString(wing+" / STAGE "+(stage+1)+" — VOXEL CUTAWAY (NOT A MINECRAFT SCREENSHOT)",20,30);
            g.dispose();ImageIO.write(image,"png",Path.of("build/palace-previews/"+wing.name().toLowerCase()+"-"+(stage+1)+".png").toFile());
        }
    }
    private Color color(Material m){String n=m.name();
        if(n.contains("FROGLIGHT")||n.contains("LANTERN")||n.contains("END_ROD"))return new Color(255,231,156);
        if(n.contains("GOLD")||n.contains("YELLOW"))return new Color(194,146,54);
        if(n.contains("COPPER"))return new Color(173,105,70);
        if(n.contains("RED"))return new Color(139,36,49);
        if(n.contains("PURPLE"))return new Color(102,55,122);
        if(n.contains("CYAN"))return new Color(49,108,118);
        if(n.contains("QUARTZ")||n.contains("CALCITE"))return new Color(224,216,199);
        if(n.contains("LEAVES")||n.contains("MOSS"))return new Color(79,125,67);
        if(n.contains("BOOK")||n.contains("OAK"))return new Color(117,78,40);
        if(n.contains("BLUE")||n.contains("ICE"))return new Color(89,172,194);
        return new Color(63,65,75);
    }
}
