package editor.mapgroups;

import editor.mapmatrix.PointComparator;

import java.awt.Point;
import java.util.TreeSet;

public class MapGroup implements Comparable<MapGroup> {
    private final int index;
    private final TreeSet<Point> coordList = new TreeSet<>(new PointComparator());

    //Add the first element
    public MapGroup(int index, Point p) {
        this.index = index;
        coordList.add(p);
    }

    public int getIndex() {
        return index;
    }

    public TreeSet<Point> getCoordList() {
        return coordList;
    }

    @Override
    public String toString() {
        StringBuilder msg = new StringBuilder("#" + index + ":   ");
        for (Point p : coordList) {
            msg.append("(").append((int) p.getX()).append(", ").append((int) p.getY()).append("); ");
        }
        return msg.toString();
    }


    @Override
    public int compareTo(MapGroup o) {
        return Integer.compare(this.index, o.index);
    }
}
