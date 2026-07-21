package editor.remap;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

public final class ReplaceRemapPlan {

    public enum Surface {
        TILE, PERMISSION, TYPE
    }

    private final List<CellChange> changes;
    private final List<SummaryRow> summaries;
    private final List<String> skippedEntries;
    private final List<String> validationProblems;

    public ReplaceRemapPlan(List<CellChange> changes, List<SummaryRow> summaries,
                            List<String> skippedEntries, List<String> validationProblems) {
        this.changes = Collections.unmodifiableList(new ArrayList<>(changes));
        this.summaries = Collections.unmodifiableList(new ArrayList<>(summaries));
        this.skippedEntries = Collections.unmodifiableList(new ArrayList<>(skippedEntries));
        this.validationProblems = Collections.unmodifiableList(new ArrayList<>(validationProblems));
    }

    public List<CellChange> getChanges() {
        return changes;
    }

    public List<SummaryRow> getSummaries() {
        return summaries;
    }

    public List<String> getSkippedEntries() {
        return skippedEntries;
    }

    public List<String> getValidationProblems() {
        return validationProblems;
    }

    public boolean canApply() {
        return validationProblems.isEmpty() && !changes.isEmpty();
    }

    public Set<Point> getAffectedMaps() {
        LinkedHashSet<Point> maps = new LinkedHashSet<>();
        for (CellChange change : changes) {
            maps.add(change.getMapCoordinates());
        }
        return maps;
    }

    public Set<Integer> getAffectedTerrainLayers() {
        LinkedHashSet<Integer> layers = new LinkedHashSet<>();
        for (CellChange change : changes) {
            if (change.getSurface() == Surface.TILE) {
                layers.add(change.getLayer());
            }
        }
        return layers;
    }

    public Set<Integer> getAffectedCollisionLayers() {
        LinkedHashSet<Integer> layers = new LinkedHashSet<>();
        for (CellChange change : changes) {
            if (change.getSurface() != Surface.TILE) {
                layers.add(change.getLayer());
            }
        }
        return layers;
    }

    /** Builds complete replacement layers first, then commits them to the live project. */
    public void apply(RemapProjectAccess access, BooleanSupplier cancelled) {
        if (!validationProblems.isEmpty()) {
            throw new IllegalStateException("A plan with validation problems cannot be applied.");
        }

        LinkedHashMap<LayerKey, int[][]> tileLayers = new LinkedHashMap<>();
        LinkedHashMap<LayerKey, int[][]> collisionLayers = new LinkedHashMap<>();

        for (CellChange change : changes) {
            checkCancelled(cancelled);
            LayerKey key = new LayerKey(change.mapCoordinates, change.layer);
            Map<LayerKey, int[][]> target = change.surface == Surface.TILE
                    ? tileLayers : collisionLayers;
            int[][] values = target.get(key);
            if (values == null) {
                values = change.surface == Surface.TILE
                        ? access.copyTileLayer(change.mapCoordinates, change.layer)
                        : access.copyCollisionLayer(change.mapCoordinates, change.layer);
                target.put(key, values);
            }
            int current = values[change.x][change.y];
            if (current != change.oldValue) {
                throw new IllegalStateException("The preview is stale at "
                        + ReplaceRemapRequest.formatPoint(change.mapCoordinates)
                        + ", " + change.getSurfaceLabel() + " [" + change.x + ", " + change.y + "].");
            }
            values[change.x][change.y] = change.newValue;
        }

        checkCancelled(cancelled);
        for (Map.Entry<LayerKey, int[][]> entry : tileLayers.entrySet()) {
            access.replaceTileLayer(entry.getKey().mapCoordinates,
                    entry.getKey().layer, entry.getValue());
        }
        for (Map.Entry<LayerKey, int[][]> entry : collisionLayers.entrySet()) {
            access.replaceCollisionLayer(entry.getKey().mapCoordinates,
                    entry.getKey().layer, entry.getValue());
        }
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled != null && cancelled.getAsBoolean()) {
            throw new CancellationException("Replace and Remap was cancelled.");
        }
    }

    public static final class CellChange {
        private final Point mapCoordinates;
        private final Surface surface;
        private final int layer;
        private final int x;
        private final int y;
        private final int oldValue;
        private final int newValue;

        public CellChange(Point mapCoordinates, Surface surface, int layer,
                          int x, int y, int oldValue, int newValue) {
            this.mapCoordinates = new Point(mapCoordinates);
            this.surface = surface;
            this.layer = layer;
            this.x = x;
            this.y = y;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public Point getMapCoordinates() {
            return new Point(mapCoordinates);
        }

        public Surface getSurface() {
            return surface;
        }

        public int getLayer() {
            return layer;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getOldValue() {
            return oldValue;
        }

        public int getNewValue() {
            return newValue;
        }

        public String getSurfaceLabel() {
            if (surface == Surface.TILE) {
                return "Layer " + (layer + 1);
            }
            return surface == Surface.PERMISSION ? "Permissions" : "Types";
        }
    }

    public static final class SummaryRow {
        private final Point mapCoordinates;
        private final String mapName;
        private final String surface;
        private final int matches;
        private final int changes;
        private final int skipped;

        public SummaryRow(Point mapCoordinates, String mapName, String surface,
                          int matches, int changes, int skipped) {
            this.mapCoordinates = new Point(mapCoordinates);
            this.mapName = mapName;
            this.surface = surface;
            this.matches = matches;
            this.changes = changes;
            this.skipped = skipped;
        }

        public Point getMapCoordinates() {
            return new Point(mapCoordinates);
        }

        public String getMapName() {
            return mapName;
        }

        public String getSurface() {
            return surface;
        }

        public int getMatches() {
            return matches;
        }

        public int getChanges() {
            return changes;
        }

        public int getSkipped() {
            return skipped;
        }
    }

    private static final class LayerKey {
        private final Point mapCoordinates;
        private final int layer;

        private LayerKey(Point mapCoordinates, int layer) {
            this.mapCoordinates = new Point(mapCoordinates);
            this.layer = layer;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof LayerKey)) {
                return false;
            }
            LayerKey other = (LayerKey) object;
            return layer == other.layer && mapCoordinates.equals(other.mapCoordinates);
        }

        @Override
        public int hashCode() {
            return 31 * mapCoordinates.hashCode() + layer;
        }
    }
}
