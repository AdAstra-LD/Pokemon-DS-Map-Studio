package editor.remap;

import editor.remap.RemapProjectSnapshot.MapSnapshot;
import editor.remap.RemapProjectSnapshot.TileDefaults;
import editor.remap.ReplaceRemapPlan.CellChange;
import editor.remap.ReplaceRemapPlan.SummaryRow;
import editor.remap.ReplaceRemapPlan.Surface;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public final class ReplaceRemapPlanner {

    public ReplaceRemapPlan plan(ReplaceRemapRequest request, RemapProjectSnapshot project,
                                 Set<Point> availableMaps, int terrainLayerCount,
                                 BooleanSupplier cancelled) {
        ArrayList<CellChange> changes = new ArrayList<>();
        ArrayList<SummaryRow> summaries = new ArrayList<>();
        ArrayList<String> skipped = new ArrayList<>();
        ArrayList<String> problems = new ArrayList<>(
                request.validate(terrainLayerCount, availableMaps));
        if (!problems.isEmpty()) {
            return new ReplaceRemapPlan(changes, summaries, skipped, problems);
        }

        LinkedHashMap<Integer, Integer> matchCounts = new LinkedHashMap<>();
        for (Integer source : request.getReplacements().keySet()) {
            matchCounts.put(source, 0);
        }

        for (Point point : request.getMaps()) {
            checkCancelled(cancelled);
            MapSnapshot map = project.getMap(point);
            if (map == null) {
                problems.add("Map " + ReplaceRemapRequest.formatPoint(point)
                        + " was not available in the planning snapshot.");
                continue;
            }

            int[][][] originalTiles = map.copyTileLayers();
            int[][][] simulatedTiles = RemapProjectSnapshot.cloneLayers(originalTiles);
            int[][][] oldDefaults = needsDefaults(request)
                    ? resolveDefaults(originalTiles, project.getTileDefaults(), cancelled)
                    : null;

            for (Integer layer : request.getLayers()) {
                int matches = 0;
                int tileChanges = 0;
                for (int x = 0; x < originalTiles[layer].length; x++) {
                    for (int y = 0; y < originalTiles[layer][x].length; y++) {
                        checkCancelled(cancelled);
                        int source = originalTiles[layer][x][y];
                        Integer replacement = request.getReplacements().get(source);
                        if (replacement == null) {
                            continue;
                        }
                        matches++;
                        matchCounts.put(source, matchCounts.get(source) + 1);
                        simulatedTiles[layer][x][y] = replacement;
                        if (request.isReplaceTiles() && replacement != source) {
                            changes.add(new CellChange(point, Surface.TILE, layer,
                                    x, y, source, replacement));
                            tileChanges++;
                        }
                    }
                }
                if (matches > 0) {
                    summaries.add(new SummaryRow(point, map.getName(),
                            "Layer " + (layer + 1), matches, tileChanges,
                            request.isReplaceTiles() ? matches - tileChanges : matches));
                }
            }

            if (needsDefaults(request)) {
                int[][][] newDefaults = resolveDefaults(simulatedTiles,
                        project.getTileDefaults(), cancelled);
                if (request.isRemapPermissions()) {
                    addCollisionChanges(point, map, oldDefaults, newDefaults,
                            ReplaceRemapRequest.PERMISSION_LAYER, Surface.PERMISSION,
                            changes, summaries, skipped, cancelled);
                }
                if (request.isRemapTypes()) {
                    addCollisionChanges(point, map, oldDefaults, newDefaults,
                            ReplaceRemapRequest.TYPE_LAYER, Surface.TYPE,
                            changes, summaries, skipped, cancelled);
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : matchCounts.entrySet()) {
            if (entry.getValue() == 0) {
                skipped.add("Tile " + entry.getKey() + " -> "
                        + request.getReplacements().get(entry.getKey())
                        + " was not found in the selected maps and layers.");
            }
        }
        if (changes.isEmpty() && problems.isEmpty()) {
            skipped.add("The preview contains no changes to apply.");
        }
        return new ReplaceRemapPlan(changes, summaries, skipped, problems);
    }

    private static boolean needsDefaults(ReplaceRemapRequest request) {
        return request.isRemapPermissions() || request.isRemapTypes();
    }

    private static void addCollisionChanges(Point point, MapSnapshot map,
                                            int[][][] oldDefaults, int[][][] newDefaults,
                                            int layer, Surface surface,
                                            List<CellChange> changes,
                                            List<SummaryRow> summaries,
                                            List<String> skippedEntries,
                                            BooleanSupplier cancelled) {
        if (map.getCollisionLayerCount() <= layer) {
            skippedEntries.add(map.getName() + ": " + surfaceLabel(surface)
                    + " are not present in this map.");
            summaries.add(new SummaryRow(point, map.getName(), surfaceLabel(surface), 0, 0, 1));
            return;
        }

        int[][] current = map.getCollisionLayer(layer);
        int candidates = 0;
        int changed = 0;
        int skipped = 0;
        int missingDefaults = 0;
        int manualOverrides = 0;
        for (int x = 0; x < current.length; x++) {
            for (int y = 0; y < current[x].length; y++) {
                checkCancelled(cancelled);
                int oldDefault = oldDefaults[layer][x][y];
                int newDefault = newDefaults[layer][x][y];
                if (oldDefault == newDefault) {
                    continue;
                }
                candidates++;
                if (oldDefault < 0 || newDefault < 0) {
                    missingDefaults++;
                    skipped++;
                    continue;
                }
                if (current[x][y] != oldDefault) {
                    manualOverrides++;
                    skipped++;
                    continue;
                }
                if (current[x][y] != newDefault) {
                    changes.add(new CellChange(point, surface, layer,
                            x, y, current[x][y], newDefault));
                    changed++;
                }
            }
        }
        if (candidates > 0 || changed > 0) {
            summaries.add(new SummaryRow(point, map.getName(), surfaceLabel(surface),
                    candidates, changed, skipped));
        }
        if (missingDefaults > 0) {
            skippedEntries.add(map.getName() + ": preserved " + missingDefaults + " "
                    + surfaceLabel(surface).toLowerCase()
                    + " cells without comparable source/replacement defaults.");
        }
        if (manualOverrides > 0) {
            skippedEntries.add(map.getName() + ": preserved " + manualOverrides + " manual "
                    + surfaceLabel(surface).toLowerCase() + " overrides.");
        }
    }

    private static String surfaceLabel(Surface surface) {
        return surface == Surface.PERMISSION ? "Permissions" : "Types";
    }

    /** Resolves defaults using the same bottom-origin and layer precedence as map painting. */
    static int[][][] resolveDefaults(int[][][] tileLayers, List<TileDefaults> defaults,
                                     BooleanSupplier cancelled) {
        int width = tileLayers[0].length;
        int height = tileLayers[0][0].length;
        int[][][] values = new int[2][width][height];
        for (int[][] layerValues : values) {
            for (int[] column : layerValues) {
                Arrays.fill(column, -1);
            }
        }

        for (int terrainLayer = 0; terrainLayer < tileLayers.length; terrainLayer++) {
            for (int tileX = 0; tileX < width; tileX++) {
                for (int tileY = 0; tileY < height; tileY++) {
                    checkCancelled(cancelled);
                    int tileIndex = tileLayers[terrainLayer][tileX][tileY];
                    if (tileIndex < 0 || tileIndex >= defaults.size()) {
                        continue;
                    }
                    TileDefaults tile = defaults.get(tileIndex);
                    for (Map.Entry<Integer, int[][]> entry : tile.getLayers().entrySet()) {
                        int collisionLayer = entry.getKey();
                        if (collisionLayer < 0 || collisionLayer >= values.length) {
                            continue;
                        }
                        int[][] grid = entry.getValue();
                        for (int x = 0; x < grid.length; x++) {
                            for (int y = 0; y < grid[x].length; y++) {
                                int value = grid[x][y];
                                int mapX = tileX + x - tile.getAnchorX();
                                int mapY = tileY + tile.getAnchorY() - y;
                                if (value >= 0 && mapX >= 0 && mapX < width
                                        && mapY >= 0 && mapY < height) {
                                    values[collisionLayer][mapX][mapY] = value;
                                }
                            }
                        }
                    }
                }
            }
        }
        return values;
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new CancellationException("Replace and Remap planning was cancelled.");
        }
    }
}
