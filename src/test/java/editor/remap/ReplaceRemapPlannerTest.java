package editor.remap;

import editor.remap.RemapProjectSnapshot.MapSnapshot;
import editor.remap.RemapProjectSnapshot.TileDefaults;
import editor.state.MapState;
import editor.state.StateHandler;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceRemapPlannerTest {

    private static final Point MAP_A = new Point(0, 0);
    private static final Point MAP_B = new Point(1, 0);

    @Test
    void plansOnlySelectedMapsAndLayers() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.tiles(MAP_A, 1)[0][0] = 0;
        access.tiles(MAP_B, 0)[0][0] = 0;

        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(1), true, false, false, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);

        assertTrue(plan.canApply());
        assertEquals(1, plan.getChanges().size());
        ReplaceRemapPlan.CellChange change = plan.getChanges().get(0);
        assertEquals(MAP_A, change.getMapCoordinates());
        assertEquals(1, change.getLayer());
    }

    @Test
    void remapsGenIvPermissionAndTypeDefaults() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.collisions(MAP_A, ReplaceRemapRequest.TYPE_LAYER)[0][0] = 2;
        access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0] = 0;

        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), false, true, true, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);

        assertEquals(2, plan.getChanges().size());
        assertEquals(1, plan.getChanges().stream()
                .filter(change -> change.getSurface() == ReplaceRemapPlan.Surface.TYPE).count());
        assertEquals(1, plan.getChanges().stream()
                .filter(change -> change.getSurface() == ReplaceRemapPlan.Surface.PERMISSION).count());
    }

    @Test
    void preservesManualPermissionOverrides() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0] = 7;

        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), false, true, false, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);

        assertFalse(plan.canApply());
        assertTrue(plan.getSkippedEntries().stream()
                .anyMatch(message -> message.contains("manual permissions overrides")));
    }

    @Test
    void rejectsOutOfRangeTileIdsAndGenVPermissionRemap() {
        FakeAccess access = projectWithTwoMaps();
        LinkedHashMap<Integer, Integer> replacements = new LinkedHashMap<>();
        replacements.put(0, 99);
        ReplaceRemapRequest request = new ReplaceRemapRequest(replacements,
                Collections.singleton(MAP_A), Collections.singleton(0), true, true, false,
                2, ReplaceRemapRequest.Capabilities.forGame(5));

        ReplaceRemapPlan plan = plan(request, access);

        assertFalse(plan.getValidationProblems().isEmpty());
        assertTrue(plan.getValidationProblems().stream()
                .anyMatch(problem -> problem.contains("outside the active tileset")));
        assertTrue(plan.getValidationProblems().stream()
                .anyMatch(problem -> problem.contains("Generation V")));
    }

    @Test
    void cancellationStopsPlanning() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), true, false, false, 0, 1);
        AtomicInteger checks = new AtomicInteger();

        assertThrows(CancellationException.class, () -> new ReplaceRemapPlanner().plan(
                request, snapshot(access), access.getMapCoordinates(),
                access.getTerrainLayerCount(), () -> checks.incrementAndGet() > 2));
    }

    @Test
    void stalePreviewDoesNotPartiallyApply() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.tiles(MAP_A, 0)[1][0] = 0;
        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), true, false, false, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);
        access.tiles(MAP_A, 0)[1][0] = 9;

        assertThrows(IllegalStateException.class, () -> plan.apply(access, () -> false));
        assertEquals(0, access.tiles(MAP_A, 0)[0][0]);
        assertEquals(9, access.tiles(MAP_A, 0)[1][0]);
    }

    @Test
    void commitFailureRollsBackPreviouslyCommittedLayers() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0] = 0;
        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), true, true, false, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);
        access.failNextCollisionReplace = true;

        assertThrows(IllegalStateException.class, () ->
                ReplaceRemapExecutor.apply(plan, access, () -> false));
        assertEquals(0, access.tiles(MAP_A, 0)[0][0]);
        assertEquals(0, access.collisions(MAP_A,
                ReplaceRemapRequest.PERMISSION_LAYER)[0][0]);
    }

    @Test
    void oneStateUndoesAndRedoesAllSurfaces() {
        FakeAccess access = projectWithTwoMaps();
        access.tiles(MAP_A, 0)[0][0] = 0;
        access.collisions(MAP_A, ReplaceRemapRequest.TYPE_LAYER)[0][0] = 2;
        access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0] = 0;
        ReplaceRemapRequest request = request(Collections.singleton(MAP_A),
                Collections.singleton(0), true, true, true, 0, 1);
        ReplaceRemapPlan plan = plan(request, access);

        ReplaceRemapState before = ReplaceRemapState.capture("Replace and Remap", access,
                plan.getAffectedMaps(), plan.getAffectedTerrainLayers(),
                plan.getAffectedCollisionLayers());
        plan.apply(access, () -> false);
        StateHandler history = new StateHandler();
        history.addState(before);

        MapState undo = (MapState) history.getPreviousState(before.captureCurrentState());
        undo.revertState();
        assertEquals(0, access.tiles(MAP_A, 0)[0][0]);
        assertEquals(2, access.collisions(MAP_A, ReplaceRemapRequest.TYPE_LAYER)[0][0]);
        assertEquals(0, access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0]);

        MapState redo = (MapState) history.getNextState();
        redo.revertState();
        assertEquals(1, access.tiles(MAP_A, 0)[0][0]);
        assertEquals(5, access.collisions(MAP_A, ReplaceRemapRequest.TYPE_LAYER)[0][0]);
        assertEquals(128, access.collisions(MAP_A, ReplaceRemapRequest.PERMISSION_LAYER)[0][0]);
    }

    private static ReplaceRemapPlan plan(ReplaceRemapRequest request, FakeAccess access) {
        return new ReplaceRemapPlanner().plan(request, snapshot(access),
                access.getMapCoordinates(), access.getTerrainLayerCount(), () -> false);
    }

    private static ReplaceRemapRequest request(Set<Point> maps, Set<Integer> layers,
                                               boolean tiles, boolean permissions, boolean types,
                                               int source, int replacement) {
        LinkedHashMap<Integer, Integer> replacements = new LinkedHashMap<>();
        replacements.put(source, replacement);
        return new ReplaceRemapRequest(replacements, maps, layers, tiles, permissions, types,
                2, ReplaceRemapRequest.Capabilities.forGame(0));
    }

    private static RemapProjectSnapshot snapshot(FakeAccess access) {
        LinkedHashMap<Point, MapSnapshot> maps = new LinkedHashMap<>();
        for (Point point : access.getMapCoordinates()) {
            int[][][] tiles = new int[access.getTerrainLayerCount()][][];
            for (int layer = 0; layer < tiles.length; layer++) {
                tiles[layer] = access.copyTileLayer(point, layer);
            }
            int[][][] collisions = new int[access.getCollisionLayerCount(point)][][];
            for (int layer = 0; layer < collisions.length; layer++) {
                collisions[layer] = access.copyCollisionLayer(point, layer);
            }
            maps.put(point, new MapSnapshot(access.getMapName(point), tiles, collisions));
        }

        List<TileDefaults> defaults = new ArrayList<>();
        defaults.add(tileDefaults(2, 0));
        defaults.add(tileDefaults(5, 128));
        return new RemapProjectSnapshot(maps, defaults);
    }

    private static TileDefaults tileDefaults(int type, int permission) {
        LinkedHashMap<Integer, int[][]> layers = new LinkedHashMap<>();
        layers.put(ReplaceRemapRequest.TYPE_LAYER, new int[][]{{type}});
        layers.put(ReplaceRemapRequest.PERMISSION_LAYER, new int[][]{{permission}});
        return new TileDefaults(0, 0, layers);
    }

    private static FakeAccess projectWithTwoMaps() {
        FakeAccess access = new FakeAccess(2, 2, 2);
        access.addMap(MAP_A);
        access.addMap(MAP_B);
        return access;
    }

    private static final class FakeAccess implements RemapProjectAccess {
        private final int size;
        private final int terrainLayers;
        private final int collisionLayers;
        private final LinkedHashMap<Point, int[][][]> tiles = new LinkedHashMap<>();
        private final LinkedHashMap<Point, int[][][]> collisions = new LinkedHashMap<>();
        private boolean failNextCollisionReplace;

        private FakeAccess(int size, int terrainLayers, int collisionLayers) {
            this.size = size;
            this.terrainLayers = terrainLayers;
            this.collisionLayers = collisionLayers;
        }

        private void addMap(Point point) {
            int[][][] mapTiles = new int[terrainLayers][size][size];
            for (int[][] layer : mapTiles) {
                for (int[] column : layer) {
                    Arrays.fill(column, -1);
                }
            }
            tiles.put(new Point(point), mapTiles);
            collisions.put(new Point(point), new int[collisionLayers][size][size]);
        }

        private int[][] tiles(Point point, int layer) {
            return tiles.get(point)[layer];
        }

        private int[][] collisions(Point point, int layer) {
            return collisions.get(point)[layer];
        }

        @Override
        public Set<Point> getMapCoordinates() {
            return new LinkedHashSet<>(tiles.keySet());
        }

        @Override
        public String getMapName(Point mapCoordinates) {
            return "Map " + ReplaceRemapRequest.formatPoint(mapCoordinates);
        }

        @Override
        public int getTerrainLayerCount() {
            return terrainLayers;
        }

        @Override
        public int getCollisionLayerCount(Point mapCoordinates) {
            return collisionLayers;
        }

        @Override
        public int[][] copyTileLayer(Point mapCoordinates, int layer) {
            return RemapProjectSnapshot.cloneLayer(tiles(mapCoordinates, layer));
        }

        @Override
        public void replaceTileLayer(Point mapCoordinates, int layer, int[][] values) {
            tiles.get(mapCoordinates)[layer] = RemapProjectSnapshot.cloneLayer(values);
        }

        @Override
        public int[][] copyCollisionLayer(Point mapCoordinates, int layer) {
            return RemapProjectSnapshot.cloneLayer(collisions(mapCoordinates, layer));
        }

        @Override
        public void replaceCollisionLayer(Point mapCoordinates, int layer, int[][] values) {
            if (failNextCollisionReplace) {
                failNextCollisionReplace = false;
                throw new IllegalStateException("Simulated commit failure");
            }
            collisions.get(mapCoordinates)[layer] = RemapProjectSnapshot.cloneLayer(values);
        }
    }
}
