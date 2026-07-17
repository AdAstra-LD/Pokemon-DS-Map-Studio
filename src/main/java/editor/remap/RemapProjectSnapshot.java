package editor.remap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RemapProjectSnapshot {

    private final LinkedHashMap<Point, MapSnapshot> maps;
    private final List<TileDefaults> tileDefaults;

    public RemapProjectSnapshot(Map<Point, MapSnapshot> maps, List<TileDefaults> tileDefaults) {
        this.maps = new LinkedHashMap<>();
        for (Map.Entry<Point, MapSnapshot> entry : maps.entrySet()) {
            this.maps.put(new Point(entry.getKey()), entry.getValue());
        }
        this.tileDefaults = Collections.unmodifiableList(new ArrayList<>(tileDefaults));
    }

    public MapSnapshot getMap(Point point) {
        return maps.get(point);
    }

    public List<TileDefaults> getTileDefaults() {
        return tileDefaults;
    }

    public static final class MapSnapshot {
        private final String name;
        private final int[][][] tileLayers;
        private final int[][][] collisionLayers;

        public MapSnapshot(String name, int[][][] tileLayers, int[][][] collisionLayers) {
            this.name = name;
            this.tileLayers = cloneLayers(tileLayers);
            this.collisionLayers = cloneLayers(collisionLayers);
        }

        public String getName() {
            return name;
        }

        public int[][][] copyTileLayers() {
            return cloneLayers(tileLayers);
        }

        public int[][] getCollisionLayer(int layer) {
            return cloneLayer(collisionLayers[layer]);
        }

        public int getCollisionLayerCount() {
            return collisionLayers.length;
        }
    }

    public static final class TileDefaults {
        private final int anchorX;
        private final int anchorY;
        private final Map<Integer, int[][]> layers;

        public TileDefaults(int anchorX, int anchorY, Map<Integer, int[][]> layers) {
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            LinkedHashMap<Integer, int[][]> copy = new LinkedHashMap<>();
            for (Map.Entry<Integer, int[][]> entry : layers.entrySet()) {
                copy.put(entry.getKey(), cloneLayer(entry.getValue()));
            }
            this.layers = Collections.unmodifiableMap(copy);
        }

        public int getAnchorX() {
            return anchorX;
        }

        public int getAnchorY() {
            return anchorY;
        }

        public Map<Integer, int[][]> getLayers() {
            return layers;
        }
    }

    static int[][][] cloneLayers(int[][][] source) {
        int[][][] copy = new int[source.length][][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = cloneLayer(source[i]);
        }
        return copy;
    }

    static int[][] cloneLayer(int[][] source) {
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = java.util.Arrays.copyOf(source[i], source[i].length);
        }
        return copy;
    }
}
