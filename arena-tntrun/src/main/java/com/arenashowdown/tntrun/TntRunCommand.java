package com.arenashowdown.tntrun;

import com.arenashowdown.tntrun.arena.*;
import com.arenashowdown.tntrun.persistence.AtomicFiles;
import com.arenashowdown.tntrun.ability.AbilityConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.nio.file.*;
import java.util.*;

/** Edits YAML definitions only; never builds floors, creates worlds or edits Core storage. */
public final class TntRunCommand implements CommandExecutor, TabCompleter {
    private final ArenaStore arenas;
    private final TntRunAdapter game;
    private final Map<UUID,Location> first = new HashMap<>(), second = new HashMap<>();
    private static final List<String> ADMIN=List.of("list","status","reload","validate","create","pos1","pos2","bounds","layer","spawn","spectator","rules","materials","ability","championship","save","cancel","recover");
    public TntRunCommand(Plugin plugin,ArenaStore arenas,TntRunAdapter game) { this.arenas=arenas; this.game=game; }
    @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a) {
        try {
            String sub=a.length==0 ? "help" : a[0].toLowerCase(Locale.ROOT);
            if (sub.equals("help")) {
                message(s,"commands.help-user");
                if (s.hasPermission("arenatntrun.admin")) message(s,"commands.help-admin",Map.of("commands",String.join(" | ",ADMIN))); return true;
            }
            if (sub.equals("spectate")) { permission(s,"spectate"); game.watch(player(s)); return true; }
            if (sub.equals("leave")) { permission(s,"leave"); game.leave(player(s)); return true; }
            permission(s,"admin");
            switch (sub) {
                case "status" -> message(s,"commands.status",Map.of("state",game.status()));
                case "list" -> message(s,"commands.list",Map.of("arenas",String.join(", ",arenas.all().stream().map(TntArena::id).sorted().toList())));
                case "cancel" -> game.requestCancel(a.length>1 ? String.join(" ",Arrays.copyOfRange(a,1,a.length)) : game.configuration().current().messages().text("commands.default-cancel-reason"));
                case "recover" -> { game.recoverPersistent(); message(s,"commands.recovered",Map.of("state",game.status())); }
                case "reload" -> { editable(); game.configuration().reload(); message(s,"commands.reloaded"); }
                case "validate" -> {
                    if (a.length==1) { game.configuration().validate(); message(s,"commands.validated"); }
                    else {
                        String id=id(a[1]); Path file=Files.exists(draft(id)) ? draft(id) : arenas.directory().resolve(id+".yml");
                        TntArena arena=ArenaStore.parse(ArenaStore.read(file));
                        if (!arena.id().equals(id)) throw new IllegalArgumentException("Id da arena deve coincidir com o arquivo");
                        message(s,"commands.arena-validated",Map.of("arena",arena.id(),"loaded",String.valueOf(Bukkit.getWorld(arena.world())!=null)));
                    }
                }
                case "championship" -> {
                    editable(); require(a,6,"championship <id> <dash|double-jump> <uses> <strength> <cooldown-ticks>");
                    game.configuration().saveChampionship(a[1],a[2],Integer.parseInt(a[3]),Double.parseDouble(a[4]),Integer.parseInt(a[5]));
                    message(s,"commands.championship-saved",Map.of("championship",a[1]));
                }
                case "pos1", "pos2" -> { Player p=player(s); (sub.equals("pos1") ? first : second).put(p.getUniqueId(),p.getLocation()); message(s,"commands.selected",Map.of("position",sub)); }
                case "create" -> {
                    editable(); require(a,2,"create <id>"); Player p=player(s); String id=id(a[1]);
                    if (arenas.get(id).isPresent() || Files.exists(draft(id))) throw new IllegalArgumentException("Arena/rascunho já existe");
                    YamlConfiguration y=new YamlConfiguration(); y.set("id",id); y.set("world",p.getWorld().getName()); y.set("timeout-seconds",180); y.set("removal-delay-ticks",8); y.set("materials",List.of("SAND","TNT"));
                    AtomicFiles.write(draft(id),y.saveToString(),false); message(s,"commands.created");
                }
                case "bounds", "layer", "spawn", "spectator", "rules", "materials", "ability", "save" -> edit(s,sub,a);
                default -> throw new IllegalArgumentException("Subcomando desconhecido. /tntrun help");
            }
        } catch (Exception e) { message(s,"error",Map.of("reason",String.valueOf(e.getMessage()))); }
        return true;
    }
    private void edit(CommandSender s,String sub,String[] a) throws Exception {
        editable(); require(a,2,sub+" <id>"); String id=id(a[1]); Path source=draft(id);
        if (!Files.exists(source)) source=arenas.directory().resolve(id+".yml");
        if (!Files.exists(source)) throw new IllegalArgumentException("Use create <id> primeiro");
        YamlConfiguration y=ArenaStore.read(source);
        if (!id.equals(y.getString("id"))) throw new IllegalArgumentException("Id da arena deve coincidir com o arquivo");
        switch (sub) {
            case "bounds", "layer" -> {
                Player p=player(s); Location l=first.get(p.getUniqueId()), r=second.get(p.getUniqueId());
                if (l==null || r==null || !l.getWorld().equals(r.getWorld()) || !l.getWorld().getName().equals(y.getString("world"))) throw new IllegalArgumentException("Selecione pos1 e pos2 no mundo da arena");
                String path="bounds";
                if (sub.equals("layer")) {
                    if (l.getBlockY()!=r.getBlockY()) throw new IllegalArgumentException("Selecione a superfície da camada na mesma altura Y");
                    int index=y.getConfigurationSection("layers")==null ? 0 : y.getConfigurationSection("layers").getKeys(false).size(); path="layers."+index;
                    y.set(path+".top-y",l.getBlockY()); y.set(path+".depth",a.length>2 ? Integer.parseInt(a[2]) : 2);
                } else { y.set(path+".min-y",Math.min(l.getBlockY(),r.getBlockY())); y.set(path+".max-y",Math.max(l.getBlockY(),r.getBlockY())); }
                y.set(path+".min-x",Math.min(l.getBlockX(),r.getBlockX())); y.set(path+".max-x",Math.max(l.getBlockX(),r.getBlockX()));
                y.set(path+".min-z",Math.min(l.getBlockZ(),r.getBlockZ())); y.set(path+".max-z",Math.max(l.getBlockZ(),r.getBlockZ()));
            }
            case "spawn", "spectator" -> {
                Player p=player(s); if (!p.getWorld().getName().equals(y.getString("world"))) throw new IllegalArgumentException("Mundo incorreto");
                String path="spectator";
                if (sub.equals("spawn")) { int i=y.getConfigurationSection("spawns")==null ? 0 : y.getConfigurationSection("spawns").getKeys(false).size(); path="spawns."+i; }
                Location l=p.getLocation(); y.set(path+".x",l.getX()); y.set(path+".y",l.getY()); y.set(path+".z",l.getZ()); y.set(path+".yaw",l.getYaw()); y.set(path+".pitch",l.getPitch());
            }
            case "rules" -> { require(a,5,"rules <id> <timeout-seconds> <delay-ticks> <elimination-y>"); y.set("timeout-seconds",Integer.parseInt(a[2])); y.set("removal-delay-ticks",Integer.parseInt(a[3])); y.set("elimination-y",Double.parseDouble(a[4])); }
            case "materials" -> { require(a,3,"materials <id> <SAND,TNT,...>"); y.set("materials",Arrays.asList(a[2].toUpperCase(Locale.ROOT).split(","))); }
            case "ability" -> {
                require(a,6,"ability <arena> <dash|double-jump> <uses> <strength> <cooldown-ticks>");
                y.set("abilities."+a[2]+".enabled",false); y.set("abilities."+a[2]+".uses",Integer.parseInt(a[3]));
                y.set("abilities."+a[2]+".strength",Double.parseDouble(a[4])); y.set("abilities."+a[2]+".cooldown-ticks",Integer.parseInt(a[5]));
                AbilityConfig.overlay(AbilityConfig.defaults(),y.getConfigurationSection("abilities"));
            }
            case "save" -> {
                arenas.save(y); if (Files.exists(draft(id))) Files.delete(draft(id));
                message(s,"saved",Map.of("arena",id)); return;
            }
        }
        AtomicFiles.write(draft(id),y.saveToString(),true); message(s,"commands.draft-updated",Map.of("arena",id));
    }
    private Path draft(String id) { return arenas.directory().resolve(id+".draft"); }
    private static String id(String id) { if (!id.matches("[a-z0-9_-]{1,48}")) throw new IllegalArgumentException("Id inválido (a-z, 0-9, _, -)"); return id; }
    private void editable() { if (game.busy() && !game.hasActiveMatch()) throw new IllegalStateException("Recupere o estado pendente antes de editar/recarregar configurações"); }
    private void permission(CommandSender s,String suffix) { if (!s.hasPermission("arenatntrun."+suffix)) throw new IllegalArgumentException(game.configuration().current().messages().text("no-permission")); }
    private static Player player(CommandSender s) { if (!(s instanceof Player p)) throw new IllegalArgumentException("Comando exclusivo de jogador"); return p; }
    private static void require(String[] a,int n,String usage) { if (a.length<n) throw new IllegalArgumentException("/tntrun "+usage); }
    private static void tell(CommandSender s,String text) { s.sendMessage(Component.text(text)); }
    private void message(CommandSender sender,String key) { message(sender,key,Map.of()); }
    private void message(CommandSender sender,String key,Map<String,String> vars) { tell(sender,game.configuration().current().messages().format(key,vars)); }
    @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] a) {
        if (a.length==1) {
            List<String> values=new ArrayList<>(List.of("help"));
            if (s.hasPermission("arenatntrun.spectate")) values.add("spectate"); if (s.hasPermission("arenatntrun.leave")) values.add("leave");
            if (s.hasPermission("arenatntrun.admin")) values.addAll(ADMIN);
            return values.stream().filter(v -> v.startsWith(a[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (a.length==2 && s.hasPermission("arenatntrun.admin")) return arenas.all().stream().map(TntArena::id).filter(v -> v.startsWith(a[1])).sorted().toList();
        return List.of();
    }
}
