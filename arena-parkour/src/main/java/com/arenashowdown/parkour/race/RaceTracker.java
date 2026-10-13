package com.arenashowdown.parkour.race;

import java.util.*;
public final class RaceTracker {
    private final int checkpoints; private final long tieThresholdNanos; private final Map<UUID,Progress> players=new LinkedHashMap<>(); private long startedAt;
    public RaceTracker(Collection<UUID> ids,int checkpoints,long tieThresholdMillis){this.checkpoints=checkpoints;this.tieThresholdNanos=tieThresholdMillis*1_000_000L;ids.forEach(id->players.put(id,new Progress()));}
    public void start(long nanoTime){startedAt=nanoTime;}
    public boolean checkpoint(UUID id,int index){Progress p=required(id);if(p.finished||index!=p.checkpoint+1)return false;p.checkpoint=index;return true;}
    public boolean finish(UUID id,long nanoTime){Progress p=required(id);if(p.finished||p.checkpoint!=checkpoints)return false;p.finished=true;p.time=Math.max(0,nanoTime-startedAt);return true;}
    public int checkpoint(UUID id){return required(id).checkpoint;}
    public long elapsed(UUID id,long now){Progress p=required(id);return p.finished?p.time:Math.max(0,now-startedAt);}
    public boolean allFinished(){return players.values().stream().allMatch(p->p.finished);}
    public List<Standing> standings(){List<Standing> result=players.entrySet().stream().filter(e->e.getValue().finished).sorted(Comparator.comparingLong(e->e.getValue().time)).map(e->new Standing(e.getKey(),e.getValue().time,0)).toList();List<Standing> ranked=new ArrayList<>();for(int i=0;i<result.size();i++){int position=i+1;if(i>0&&Math.abs(result.get(i).nanos-result.get(i-1).nanos)<=tieThresholdNanos)position=ranked.get(i-1).position;ranked.add(new Standing(result.get(i).playerId,result.get(i).nanos,position));}return ranked;}
    private Progress required(UUID id){Progress p=players.get(id);if(p==null)throw new IllegalArgumentException("Participante desconhecido");return p;}
    private static final class Progress {int checkpoint;boolean finished;long time;}
    public record Standing(UUID playerId,long nanos,int position){}
}
