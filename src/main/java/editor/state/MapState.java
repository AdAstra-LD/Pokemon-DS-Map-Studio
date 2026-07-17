package editor.state;

import java.awt.Point;
import java.util.Set;

/** A reversible editor state that knows which map views need refreshing. */
public abstract class MapState extends State {

    protected MapState(String name) {
        super(name);
    }

    public abstract MapState captureCurrentState();

    public abstract Set<Point> getAffectedMaps();

    public abstract Set<Integer> getAffectedLayers();

    public boolean shouldPruneUnusedMaps() {
        return false;
    }
}
