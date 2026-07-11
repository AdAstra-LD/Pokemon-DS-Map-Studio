
package editor.state;

import editor.handler.MapData;
import editor.handler.MapEditorHandler;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Trifindo
 */
public class MapLayerState extends State {

    private final MapEditorHandler handler;
    private final int layerIndex;

    private final HashMap<Point, int[][]> mapTileLayers;
    private final HashMap<Point, int[][]> mapHeightLayers;
    private final HashMap<Point, byte[][][]> mapCollisionLayers;
    //private int[][] tileLayer;
    //private int[][] heightLayer;

    public MapLayerState(String name, MapEditorHandler handler) {
        this(name, handler, true);
    }

    public MapLayerState(String name, MapEditorHandler handler, boolean fullState) {
        super(name);

        this.handler = handler;
        this.layerIndex = handler.getActiveLayerIndex();

        mapTileLayers = new HashMap<>();
        mapHeightLayers = new HashMap<>();
        mapCollisionLayers = new HashMap<>();
        if (fullState) {
            for (Map.Entry<Point, MapData> mapEntry : handler.getMapMatrix().getMatrix().entrySet()) {
                mapTileLayers.put(mapEntry.getKey(), mapEntry.getValue().getGrid().cloneTileLayer(layerIndex));
                mapHeightLayers.put(mapEntry.getKey(), mapEntry.getValue().getGrid().cloneHeightLayer(layerIndex));
                mapCollisionLayers.put(mapEntry.getKey(), cloneCollisionLayers(mapEntry.getValue()));
            }
        } else {
            mapTileLayers.put(handler.getMapSelected(), handler.getGrid().cloneTileLayer(layerIndex));
            mapHeightLayers.put(handler.getMapSelected(), handler.getGrid().cloneHeightLayer(layerIndex));
            mapCollisionLayers.put(handler.getMapSelected(), cloneCollisionLayers(handler.getCurrentMap()));
        }

    }

    @Override
    public void revertState() {
        //BDHC and other auxiliary formats are not part of map drawing undo.

        for (Map.Entry<Point, int[][]> mapEntry : mapTileLayers.entrySet()) {
            handler.getMapMatrix().getMapAndCreate(mapEntry.getKey()).getGrid().setTileLayer(layerIndex, mapEntry.getValue());
        }
        for (Map.Entry<Point, int[][]> mapEntry : mapHeightLayers.entrySet()) {
            handler.getMapMatrix().getMapAndCreate(mapEntry.getKey()).getGrid().setHeightLayer(layerIndex, mapEntry.getValue());
        }
        for (Map.Entry<Point, byte[][][]> mapEntry : mapCollisionLayers.entrySet()) {
            MapData mapData = handler.getMapMatrix().getMapAndCreate(mapEntry.getKey());
            byte[][][] layers = mapEntry.getValue();
            for (int i = 0; i < layers.length && i < mapData.getCollisions().getNumLayers(); i++) {
                mapData.getCollisions().setLayer(i, cloneLayer(layers[i]));
            }
        }

        //Remove maps that were not used
        handler.getMapMatrix().getMatrix().entrySet().removeIf(entry -> !mapTileLayers.containsKey(entry.getKey()));
        
        /*
        for(MapData mapData : handler.getMapMatrix().getMatrix().values()){
            mapData.getGrid().updateAllMapLayers(handler.useRealTimePostProcessing());
        }*/

        //handler.getGrid().setTileLayer(layerIndex, tileLayer);
        //handler.getGrid().setHeightLayer(layerIndex, heightLayer);
    }

    public void updateState() {
        if (!mapTileLayers.containsKey(handler.getMapSelected())) {
            mapTileLayers.put(handler.getMapSelected(), handler.getGrid().cloneTileLayer(layerIndex));
        }
        if (!mapHeightLayers.containsKey(handler.getMapSelected())) {
            mapHeightLayers.put(handler.getMapSelected(), handler.getGrid().cloneHeightLayer(layerIndex));
        }
        if (!mapCollisionLayers.containsKey(handler.getMapSelected())) {
            mapCollisionLayers.put(handler.getMapSelected(), cloneCollisionLayers(handler.getCurrentMap()));
        }

    }

    private static byte[][][] cloneCollisionLayers(MapData mapData) {
        int numLayers = mapData.getCollisions().getNumLayers();
        byte[][][] copy = new byte[numLayers][][];
        for (int i = 0; i < numLayers; i++) {
            copy[i] = mapData.getCollisions().cloneLayer(i);
        }
        return copy;
    }

    private static byte[][] cloneLayer(byte[][] layer) {
        byte[][] copy = new byte[layer.length][];
        for (int i = 0; i < layer.length; i++) {
            copy[i] = java.util.Arrays.copyOf(layer[i], layer[i].length);
        }
        return copy;
    }

    public int getLayerIndex() {
        return layerIndex;
    }

    public Set<Point> getKeySet() {
        return mapTileLayers.keySet();
    }

}
