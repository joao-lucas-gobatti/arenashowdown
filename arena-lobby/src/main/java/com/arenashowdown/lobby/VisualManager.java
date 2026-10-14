package com.arenashowdown.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;import org.bukkit.entity.*;import org.bukkit.scoreboard.*;import org.bukkit.util.Transformation;import org.joml.Vector3f;
import java.util.*;

final class VisualManager {
    static final String TAG="arena_lobby_managed"; private final ArenaLobbyPlugin plugin;private final Map<String,TextDisplay> holograms=new HashMap<>();
    private final LegacyComponentSerializer legacy=LegacyComponentSerializer.legacyAmpersand();
    VisualManager(ArenaLobbyPlugin plugin){this.plugin=plugin;}
    void rebuild(){remove();var section=plugin.getConfig().getConfigurationSection("holograms");if(section==null)return;for(String id:section.getKeys(false)){Location at=plugin.location("holograms."+id);if(at!=null)spawn(id,plugin.getConfig().getString("holograms."+id+".type","ranking"),at);}}
    void spawn(String id,String type,Location at){TextDisplay d=at.getWorld().spawn(at,TextDisplay.class,e->{e.addScoreboardTag(TAG);e.addScoreboardTag(TAG+"_"+id);e.setPersistent(false);e.setBillboard(Display.Billboard.CENTER);e.setShadowed(true);e.setSeeThrough(false);e.setBackgroundColor(Color.fromARGB(170,8,8,8));e.setTransformation(new Transformation(new Vector3f(),e.getTransformation().getLeftRotation(),new Vector3f(1.1f),e.getTransformation().getRightRotation()));});holograms.put(id,d);updateOne(d,type);}
    void update(){var section=plugin.getConfig().getConfigurationSection("holograms");if(section!=null)holograms.forEach((id,d)->updateOne(d,section.getString(id+".type","ranking")));}
    private void updateOne(TextDisplay d,String type){if(!d.isValid())return;var v=plugin.state().view;String text=switch(type){case "championship"->"&6&l"+v.name()+"\n&fEstado: &e"+v.state()+"\n&fRodada: &e"+v.round()+"\n&fProximo: &e"+v.next();case "last"->"&6&lULTIMA PARTIDA\n&7Consulte o historico com /arena status";default->{StringBuilder b=new StringBuilder("&6&lRANKING GERAL");int i=1;for(var r:v.ranking())b.append("\n&e").append(i++).append(". &f").append(r.playerName()).append(" &6").append(r.points());if(v.ranking().isEmpty())b.append("\n&7Sem pontuacao registrada");yield b.toString();}};d.text(legacy.deserialize(text));}
    void remove(){World w=plugin.world();if(w!=null)for(Entity e:w.getEntities())if(e.getScoreboardTags().contains(TAG))e.remove();holograms.clear();}
    void board(Player p){if(!plugin.getConfig().getBoolean("scoreboard.enabled",true))return;Scoreboard b=Bukkit.getScoreboardManager().getNewScoreboard();Objective o=b.registerNewObjective("arena_lobby",Criteria.DUMMY,legacy.deserialize(plugin.getConfig().getString("scoreboard.title","&6ARENA SHOWDOWN")));o.setDisplaySlot(DisplaySlot.SIDEBAR);var v=plugin.state().view;List<String> lines=new ArrayList<>();lines.add("§f"+v.name());lines.add("§7Rodada: §e"+v.round());int n=0;for(var r:v.ranking()){if(n++>=plugin.getConfig().getInt("scoreboard.entries",5))break;lines.add("§f"+r.playerName()+": §6"+r.points());}lines.add("§7Proximo: §e"+v.next());int score=lines.size();for(String line:lines)o.getScore(unique(line,score)).setScore(score--);p.setScoreboard(b);}
    private String unique(String s,int n){return s+"§"+Integer.toHexString(n%16);}
}
