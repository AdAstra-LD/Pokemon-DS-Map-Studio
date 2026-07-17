package editor.remap;

import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import editor.grid.MapGrid;
import formats.collisions.Collisions;
import tileset.Tile;
import tileset.Tileset;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PdsmsRemapProjectAccess implements RemapProjectAccess {

    private final MapEditorHandler handler;

    public PdsmsRemapProjectAccess(MapEditorHandler handler) {
        this.handler = handler;
    }

    @Override
    public Set<Point> getMapCoordinates() {
        ArrayList<Point> points = new ArrayList<>();
        for (Point point : handler.getMapMatrix().getMatrix().keySet()) {
            points.add(new Point(point));
        }
        points.sort(Comparator.comparingInt((Point point) -> point.y)
                .thenComparingInt(point -> point.x));
        return new LinkedHashSet<>(points);
    }

    @Override
    public String getMapName(Point mapCoordinates) {
        String projectPath = handler.getMapMatrix().filePath;
        if (projectPath == null || projectPath.isEmpty()) {
            return "Map " + ReplaceRemapRequest.formatPoint(mapCoordinates);
        }
        return handler.getMapMatrix().getMapName(mapCoordinates);
    }

    @Override
    public int getTerrainLayerCount() {
        return MapGrid.numLayers;
    }

    @Override
    public int getCollisionLayerCount(Point mapCoordinates) {
        return getMap(mapCoordinates).getCollisions().getNumLayers();
    }

    @Override
    public int[][] copyTileLayer(Point mapCoordinates, int layer) {
        return getMap(mapCoordinates).getGrid().cloneTileLayer(layer);
    }

    @Override
    public void replaceTileLayer(Point mapCoordinates, int layer, int[][] values) {
        getMap(mapCoordinates).getGrid().setTileLayer(layer,
                RemapProjectSnapshot.cloneLayer(values));
    }

    @Override
    public int[][] copyCollisionLayer(Point mapCoordinates, int layer) {
        Collisions collisions = getMap(mapCoordinates).getCollisions();
        int[][] values = new int[Collisions.cols][Collisions.rows];
        for (int x = 0; x < Collisions.cols; x++) {
            for (int mapY = 0; mapY < Collisions.rows; mapY++) {
                values[x][mapY] = collisions.getValue(layer, x,
                        Collisions.rows - 1 - mapY);
            }
        }
        return values;
    }

    @Override
    public void replaceCollisionLayer(Point mapCoordinates, int layer, int[][] values) {
        Collisions collisions = getMap(mapCoordinates).getCollisions();
        for (int x = 0; x < Collisions.cols; x++) {
            for (int mapY = 0; mapY < Collisions.rows; mapY++) {
                collisions.setValue(values[x][mapY], layer, x,
                        Collisions.rows - 1 - mapY);
            }
        }
    }

    public RemapProjectSnapshot captureSnapshot(ReplaceRemapRequest request) {
        LinkedHashMap<Point, RemapProjectSnapshot.MapSnapshot> maps = new LinkedHashMap<>();
        for (Point point : request.getMaps()) {
            int[][][] tileLayers = new int[getTerrainLayerCount()][][];
            for (int layer = 0; layer < tileLayers.length; layer++) {
                tileLayers[layer] = copyTileLayer(point, layer);
            }
            int collisionLayerCount = getCollisionLayerCount(point);
            int[][][] collisionLayers = new int[collisionLayerCount][][];
            for (int layer = 0; layer < collisionLayerCount; layer++) {
                collisionLayers[layer] = copyCollisionLayer(point, layer);
            }
            maps.put(new Point(point), new RemapProjectSnapshot.MapSnapshot(
                    getMapName(point), tileLayers, collisionLayers));
        }

        Tileset tileset = handler.getTileset();
        List<RemapProjectSnapshot.TileDefaults> defaults = new ArrayList<>(tileset.size());
        for (int i = 0; i < tileset.size(); i++) {
            Tile tile = tileset.get(i);
            Map<Integer, int[][]> layers = new LinkedHashMap<>();
            for (Map.Entry<Integer, int[][]> entry : tile.getCollisionDefaults().entrySet()) {
                layers.put(entry.getKey(), RemapProjectSnapshot.cloneLayer(entry.getValue()));
            }
            defaults.add(new RemapProjectSnapshot.TileDefaults(
                    tile.getCollisionFootprintAnchorX(),
                    tile.getCollisionFootprintAnchorY(), layers));
        }
        return new RemapProjectSnapshot(maps, defaults);
    }

    private MapData getMap(Point mapCoordinates) {
        MapData map = handler.getMapMatrix().getMap(mapCoordinates);
        if (map == null) {
            throw new IllegalStateException("Map "
                    + ReplaceRemapRequest.formatPoint(mapCoordinates) + " is not loaded.");
        }
        return map;
    }
}
