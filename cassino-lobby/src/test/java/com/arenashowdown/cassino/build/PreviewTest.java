package com.arenashowdown.cassino.build;

import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import java.util.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Diagnostic projections of exact blueprint voxels; NOT Minecraft screenshots. */
class PreviewTest {
    @Test void exportBlueprintProjections() throws Exception {
        Map<String,BlockOperation> finalBlocks=new HashMap<>();
        for(var op:LobbyPlan.create(64).blocks()) finalBlocks.put(op.x()+","+op.y()+","+op.z(),op);
        for(boolean section:new boolean[]{false,true}){
            var image=new BufferedImage(1600,1050,BufferedImage.TYPE_INT_RGB);
            var g=image.createGraphics();g.setColor(new Color(22,24,32));g.fillRect(0,0,1600,1050);
            var blocks=new ArrayList<>(finalBlocks.values());
            blocks.sort(Comparator.comparingInt((BlockOperation a)->a.x()+a.z()).thenComparingInt(BlockOperation::y));
            for(var op:blocks){
                if(op.material()==Material.AIR||op.y()<64||(section&&(op.y()>81||op.z()>39)))continue;
                int x=800+(op.x()-op.z())*4,y=500+(op.x()+op.z())*2-(op.y()-64)*5;
                Color c=color(op.material());
                g.setColor(c.darker());g.fillPolygon(new int[]{x-4,x,x,x-4},new int[]{y,y+2,y+7,y+5},4);
                g.setColor(c.darker().darker());g.fillPolygon(new int[]{x,x+4,x+4,x},new int[]{y+2,y,y+5,y+7},4);
                g.setColor(c);g.fillPolygon(new int[]{x-4,x,x+4,x},new int[]{y,y-2,y,y+2},4);
            }
            g.setColor(Color.WHITE);g.setFont(new Font("SansSerif",Font.BOLD,20));
            g.drawString("V2 / "+(section?"CORTE DO SALAO":"VOLUMETRIA")+" - PROJECAO DO GERADOR, NAO CAPTURA DO MINECRAFT",30,40);
            g.dispose();Files.createDirectories(Path.of("build/previews"));
            ImageIO.write(image,"png",Path.of("build/previews/"+(section?"interior":"exterior")+".png").toFile());
        }
    }
    private Color color(Material m){
        String n=m.name();
        if(n.contains("LIGHT")||n.contains("LANTERN")||n.contains("END_ROD"))return new Color(255,232,155);
        if(n.contains("GOLD")||n.contains("YELLOW"))return new Color(193,145,55);
        if(n.contains("COPPER"))return new Color(171,109,69);
        if(n.contains("RED")||n.contains("TNT"))return new Color(132,36,48);
        if(n.contains("QUARTZ")||n.contains("CALCITE")||n.contains("WHITE"))return new Color(222,215,196);
        if(n.contains("OAK"))return new Color(85,57,37);
        if(n.contains("LEAVES")||n.contains("MOSS"))return new Color(68,101,59);
        if(n.contains("BLUE"))return new Color(82,156,179);
        if(n.contains("MAGENTA"))return new Color(158,76,145);
        return new Color(63,65,71);
    }
}
