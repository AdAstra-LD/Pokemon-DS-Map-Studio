package editor.remap;

import editor.game.Game;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ReplaceRemapRequest {

    public static final int TYPE_LAYER = 0;
    public static final int PERMISSION_LAYER = 1;

    private final LinkedHashMap<Integer, Integer> replacements;
    private final LinkedHashSet<Point> maps;
    private final LinkedHashSet<Integer> layers;
    private final boolean replaceTiles;
    private final boolean remapPermissions;
    private final boolean remapTypes;
    private final int tilesetSize;
    private final Capabilities capabilities;

    public ReplaceRemapRequest(Map<Integer, Integer> replacements, Set<Point> maps,
                               Set<Integer> layers, boolean replaceTiles,
                               boolean remapPermissions, boolean remapTypes,
                               int tilesetSize, Capabilities capabilities) {
        this.replacements = new LinkedHashMap<>(replacements);
        this.maps = new LinkedHashSet<>();
        for (Point point : maps) {
            this.maps.add(new Point(point));
        }
        this.layers = new LinkedHashSet<>(layers);
        this.replaceTiles = replaceTiles;
        this.remapPermissions = remapPermissions;
        this.remapTypes = remapTypes;
        this.tilesetSize = tilesetSize;
        this.capabilities = capabilities;
    }

    public Map<Integer, Integer> getReplacements() {
        return Collections.unmodifiableMap(replacements);
    }

    public Set<Point> getMaps() {
        LinkedHashSet<Point> copy = new LinkedHashSet<>();
        for (Point point : maps) {
            copy.add(new Point(point));
        }
        return copy;
    }

    public Set<Integer> getLayers() {
        return Collections.unmodifiableSet(layers);
    }

    public boolean isReplaceTiles() {
        return replaceTiles;
    }

    public boolean isRemapPermissions() {
        return remapPermissions;
    }

    public boolean isRemapTypes() {
        return remapTypes;
    }

    public int getTilesetSize() {
        return tilesetSize;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public List<String> validate(int terrainLayerCount, Set<Point> availableMaps) {
        ArrayList<String> problems = new ArrayList<>();
        if (replacements.isEmpty()) {
            problems.add("Add at least one replacement.");
        }
        if (!replaceTiles && !remapPermissions && !remapTypes) {
            problems.add("Enable Tiles, Permissions, or Types.");
        }
        if (maps.isEmpty()) {
            problems.add("Select at least one map.");
        }
        if (layers.isEmpty()) {
            problems.add("Select at least one terrain layer.");
        }
        for (Point point : maps) {
            if (!availableMaps.contains(point)) {
                problems.add("Map " + formatPoint(point) + " is no longer loaded.");
            }
        }
        for (Integer layer : layers) {
            if (layer == null || layer < 0 || layer >= terrainLayerCount) {
                problems.add("Terrain layer " + (layer == null ? "?" : layer + 1) + " is invalid.");
            }
        }
        for (Map.Entry<Integer, Integer> entry : replacements.entrySet()) {
            int source = entry.getKey();
            int replacement = entry.getValue();
            if (source < 0 || source >= tilesetSize) {
                problems.add("Source tile " + source + " is outside the active tileset.");
            }
            if (replacement < 0 || replacement >= tilesetSize) {
                problems.add("Replacement tile " + replacement + " is outside the active tileset.");
            }
        }
        if (remapPermissions && !capabilities.canRemapPermissions()) {
            problems.add(capabilities.getPermissionUnavailableReason());
        }
        if (remapTypes && !capabilities.canRemapTypes()) {
            problems.add(capabilities.getTypeUnavailableReason());
        }
        return problems;
    }

    public static String formatPoint(Point point) {
        return "(" + point.x + ", " + point.y + ")";
    }

    public static final class Capabilities {
        private final boolean permissions;
        private final boolean types;
        private final String permissionUnavailableReason;
        private final String typeUnavailableReason;

        private Capabilities(boolean permissions, boolean types,
                             String permissionUnavailableReason,
                             String typeUnavailableReason) {
            this.permissions = permissions;
            this.types = types;
            this.permissionUnavailableReason = permissionUnavailableReason;
            this.typeUnavailableReason = typeUnavailableReason;
        }

        public static Capabilities forGame(int game) {
            if (Game.isGenV(game)) {
                String reason = "Generation V permission/type remapping is unavailable: its PER records "
                        + "contain geometry and 16-bit fields that need a dedicated adapter.";
                return new Capabilities(false, false, reason, reason);
            }
            return new Capabilities(true, true, "", "");
        }

        public boolean canRemapPermissions() {
            return permissions;
        }

        public boolean canRemapTypes() {
            return types;
        }

        public String getPermissionUnavailableReason() {
            return permissionUnavailableReason;
        }

        public String getTypeUnavailableReason() {
            return typeUnavailableReason;
        }
    }
}
