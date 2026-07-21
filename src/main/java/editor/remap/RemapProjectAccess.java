package editor.remap;

import java.awt.Point;
import java.util.Set;

/** Minimal project surface used by the planner, executor, and undo state. */
public interface RemapProjectAccess {

    Set<Point> getMapCoordinates();

    String getMapName(Point mapCoordinates);

    int getTerrainLayerCount();

    int getCollisionLayerCount(Point mapCoordinates);

    int[][] copyTileLayer(Point mapCoordinates, int layer);

    void replaceTileLayer(Point mapCoordinates, int layer, int[][] values);

    int[][] copyCollisionLayer(Point mapCoordinates, int layer);

    void replaceCollisionLayer(Point mapCoordinates, int layer, int[][] values);
}
