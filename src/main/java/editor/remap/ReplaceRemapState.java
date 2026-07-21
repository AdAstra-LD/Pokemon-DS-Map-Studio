package editor.remap;

import editor.state.MapState;

import java.awt.Point;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Snapshot for one coherent multi-map, multi-layer Replace and Remap operation. */
public final class ReplaceRemapState extends MapState {

    private final RemapProjectAccess access;
    private final LinkedHashSet<Point> maps;
    private final LinkedHashSet<Integer> terrainLayers;
    private final LinkedHashSet<Integer> collisionLayers;
    private final Map<Point, Map<Integer, int[][]>> tileValues;
    private final Map<Point, Map<Integer, int[][]>> collisionValues;

    private ReplaceRemapState(String name, RemapProjectAccess access,
                              Set<Point> maps, Set<Integer> terrainLayers,
                              Set<Integer> collisionLayers) {
        super(name);
        this.access = access;
        this.maps = copyPoints(maps);
        this.terrainLayers = new LinkedHashSet<>(terrainLayers);
        this.collisionLayers = new LinkedHashSet<>(collisionLayers);
        this.tileValues = new LinkedHashMap<>();
        this.collisionValues = new LinkedHashMap<>();
        capture();
    }

    public static ReplaceRemapState capture(String name, RemapProjectAccess access,
                                            Set<Point> maps, Set<Integer> terrainLayers,
                                            Set<Integer> collisionLayers) {
        return new ReplaceRemapState(name, access, maps, terrainLayers, collisionLayers);
    }

    private void capture() {
        for (Point point : maps) {
            LinkedHashMap<Integer, int[][]> mapTiles = new LinkedHashMap<>();
            for (Integer layer : terrainLayers) {
                mapTiles.put(layer, access.copyTileLayer(point, layer));
            }
            tileValues.put(new Point(point), mapTiles);

            LinkedHashMap<Integer, int[][]> mapCollisions = new LinkedHashMap<>();
            for (Integer layer : collisionLayers) {
                mapCollisions.put(layer, access.copyCollisionLayer(point, layer));
            }
            collisionValues.put(new Point(point), mapCollisions);
        }
    }

    @Override
    public void revertState() {
        for (Map.Entry<Point, Map<Integer, int[][]>> mapEntry : tileValues.entrySet()) {
            for (Map.Entry<Integer, int[][]> layer : mapEntry.getValue().entrySet()) {
                access.replaceTileLayer(mapEntry.getKey(), layer.getKey(), layer.getValue());
            }
        }
        for (Map.Entry<Point, Map<Integer, int[][]>> mapEntry : collisionValues.entrySet()) {
            for (Map.Entry<Integer, int[][]> layer : mapEntry.getValue().entrySet()) {
                access.replaceCollisionLayer(mapEntry.getKey(), layer.getKey(), layer.getValue());
            }
        }
    }

    @Override
    public MapState captureCurrentState() {
        return new ReplaceRemapState("Replace and Remap", access, maps,
                terrainLayers, collisionLayers);
    }

    @Override
    public Set<Point> getAffectedMaps() {
        return copyPoints(maps);
    }

    @Override
    public Set<Integer> getAffectedLayers() {
        return Collections.unmodifiableSet(terrainLayers);
    }

    private static LinkedHashSet<Point> copyPoints(Set<Point> source) {
        LinkedHashSet<Point> copy = new LinkedHashSet<>();
        for (Point point : source) {
            copy.add(new Point(point));
        }
        return copy;
    }
}
