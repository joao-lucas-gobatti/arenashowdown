package com.arenashowdown.tntrun.arena;

import com.arenashowdown.tntrun.ability.AbilityConfig;
import java.util.*;

public record TntArena(String id, String world, Bounds bounds, List<Layer> layers,
                       List<Point> spawns, Point spectator, double eliminationY,
                       int timeoutSeconds, int removalDelayTicks, Set<String> materials, AbilityConfig abilities) {
    public TntArena(String id,String world,Bounds bounds,List<Layer> layers,List<Point> spawns,Point spectator,
                    double eliminationY,int timeoutSeconds,int removalDelayTicks,Set<String> materials) {
        this(id,world,bounds,layers,spawns,spectator,eliminationY,timeoutSeconds,removalDelayTicks,materials,AbilityConfig.defaults());
    }
    public record Point(double x, double y, double z, float yaw, float pitch) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) throw new IllegalArgumentException("Non-finite point");
        }
    }
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public Bounds { if (minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Inverted bounds"); }
        public boolean contains(double x, double y, double z) {
            return x >= minX && x < (double) maxX + 1 && y >= minY && y < (double) maxY + 1 && z >= minZ && z < (double) maxZ + 1;
        }
        public boolean contains(Point p) { return contains(p.x(), p.y(), p.z()); }
        public boolean overlaps(Bounds b) {
            return minX <= b.maxX && maxX >= b.minX && minY <= b.maxY && maxY >= b.minY && minZ <= b.maxZ && maxZ >= b.minZ;
        }
    }
    public record Layer(int minX, int maxX, int minZ, int maxZ, int topY, int depth) {
        public Layer {
            if (minX > maxX || minZ > maxZ || depth < 1 || depth > 4) throw new IllegalArgumentException("Invalid layer");
        }
        public long blocks() { return ((long)maxX - minX + 1) * ((long)maxZ - minZ + 1) * depth; }
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y <= topY && y > topY - depth;
        }
    }
    public TntArena {
        Objects.requireNonNull(abilities,"abilities");
        layers = layers.stream().sorted(Comparator.comparingInt(Layer::topY).reversed()).toList();
        spawns = List.copyOf(spawns); materials = Set.copyOf(materials);
        if (id == null || !id.matches("[a-z0-9_-]{1,48}") || world == null || world.isBlank() || bounds == null
                || layers.isEmpty() || spawns.size() < 2 || spectator == null || materials.isEmpty()
                || timeoutSeconds < 5 || timeoutSeconds > 3600 || removalDelayTicks < 1 || removalDelayTicks > 100
                || !Double.isFinite(eliminationY) || eliminationY < bounds.minY() || eliminationY >= layers.getLast().topY())
            throw new IllegalArgumentException("Incomplete/invalid arena " + id);
        if (bounds.minX() < -29999984 || bounds.maxX() > 29999984 || bounds.minZ() < -29999984 || bounds.maxZ() > 29999984)
            throw new IllegalArgumentException("Bounds exceed world coordinate limits");
        if (!bounds.contains(spectator) || spawns.stream().anyMatch(p -> !bounds.contains(p)))
            throw new IllegalArgumentException("Spawn/spectator outside bounds");
        Set<Integer> occupiedY = new HashSet<>();
        for (Layer l : layers) {
            if (!bounds.contains(l.minX(), l.topY() - l.depth() + 1, l.minZ()) || !bounds.contains(l.maxX(), l.topY(), l.maxZ()))
                throw new IllegalArgumentException("Layer outside bounds");
            for (int y = l.topY() - l.depth() + 1; y <= l.topY(); y++)
                if (!occupiedY.add(y)) throw new IllegalArgumentException("Overlapping layer heights");
        }
        if (layers.stream().mapToLong(Layer::blocks).sum() > 65536) throw new IllegalArgumentException("Arena exceeds 65536 floor blocks");
        Layer first = layers.getFirst();
        for (Point p : spawns) {
            if (Math.abs(p.y() - first.topY() - 1) > .1 || !first.contains((int)Math.floor(p.x()), first.topY(), (int)Math.floor(p.z())))
                throw new IllegalArgumentException("Spawns must stand on the top layer");
        }
        if (new HashSet<>(spawns).size() != spawns.size()) throw new IllegalArgumentException("Duplicate spawns");
    }
    public int layerAt(double y) {
        for (int i = 0; i < layers.size(); i++) if (y >= layers.get(i).topY() + .5) return i;
        return layers.size();
    }
    public boolean floor(int x, int y, int z) { return layers.stream().anyMatch(l -> l.contains(x, y, z)); }
}
