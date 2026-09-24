package editor.mapmatrix;

import editor.buildingeditor2.buildfile.BuildFile;
import editor.handler.MapData;
import editor.handler.MapEditorHandler;
import formats.bdhc.Bdhc;
import formats.collisions.Collisions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapMatrixAreaSaveTest {

    private static final int AREA = 2;

    @TempDir
    Path tempDir;

    @Test
    void savesAreaAsStandaloneProjectWithoutTouchingTheOpenOne() throws Exception {
        MapEditorHandler handler = new MapEditorHandler(null);
        MapMatrix mapMatrix = handler.getMapMatrix();
        HashMap<Point, MapData> matrix = mapMatrix.getMatrix();
        matrix.put(new Point(-1, 0), map(handler, 0));
        matrix.put(new Point(3, 2), map(handler, AREA));
        matrix.put(new Point(4, 2), map(handler, AREA));

        String projectPath = tempDir.resolve("project").resolve("project.pdsmap").toString();
        mapMatrix.filePath = projectPath;

        HashMap<Point, MapData> areaMaps = new HashMap<>();
        areaMaps.put(new Point(3, 2), matrix.get(new Point(3, 2)));
        areaMaps.put(new Point(4, 2), matrix.get(new Point(4, 2)));

        //A per-map file left by a previous split with a different layout
        File areaDir = tempDir.resolve("AD" + AREA).toFile();
        assertTrue(areaDir.mkdirs());
        File staleFile = new File(areaDir, "AD" + AREA + "_05_05." + Collisions.fileExtension);
        assertTrue(staleFile.createNewFile());

        String areaPath = mapMatrix.saveAreaToFile(tempDir.toString(), areaMaps, AREA);

        assertEquals(new File(areaDir, "AD" + AREA + ".pdsmap").getPath(), areaPath);
        assertEquals(projectPath, mapMatrix.filePath);
        assertEquals(4, matrix.size());

        HashMap<Point, MapData> loaded = MapMatrix.getGridsFromFile(areaPath, handler);
        Set<Point> expectedCoords = new HashSet<>();
        expectedCoords.add(new Point(0, 0));
        expectedCoords.add(new Point(1, 0));
        assertEquals(expectedCoords, loaded.keySet());
        for (MapData mapData : loaded.values()) {
            assertEquals(AREA, mapData.getAreaIndex());
        }

        //Per-map file names must match the coords the area is loaded with
        for (Point p : loaded.keySet()) {
            for (String extension : new String[]{Collisions.fileExtension, Bdhc.fileExtension, BuildFile.fileExtension}) {
                String path = MapMatrix.getFilePathWithCoords(loaded, areaDir.getPath(),
                        new File(areaPath).getName(), p, extension);
                assertTrue(new File(path).isFile(), "Missing " + path);
            }
        }
        assertFalse(staleFile.exists());

        String content = new String(Files.readAllBytes(new File(areaPath).toPath()));
        assertEquals(content.indexOf("tileset"), content.lastIndexOf("tileset"));
    }

    @Test
    void mapsThumbnailCoversTheMapsBoundingBox() {
        MapEditorHandler handler = new MapEditorHandler(null);
        HashMap<Point, MapData> maps = new HashMap<>();
        maps.put(new Point(3, 2), map(handler, AREA));
        maps.put(new Point(5, 3), map(handler, AREA));

        BufferedImage thumbnail = MapMatrix.createMapsThumbnail(maps);

        assertEquals(3 * MapData.mapThumbnailSize, thumbnail.getWidth());
        assertEquals(2 * MapData.mapThumbnailSize, thumbnail.getHeight());
    }

    private static MapData map(MapEditorHandler handler, int areaIndex) {
        MapData mapData = new MapData(handler);
        mapData.setAreaIndex(areaIndex);
        return mapData;
    }
}
