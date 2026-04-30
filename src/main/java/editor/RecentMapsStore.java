package editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

final class RecentMapsStore {
    private static final int MAX_RECENT_MAPS = 9;
    private static final List<String> recentMaps = new ArrayList<>();

    private RecentMapsStore() {
    }

    static void load(Preferences prefs) {
        recentMaps.clear();
        for (int i = 0; i < MAX_RECENT_MAPS; i++) {
            String value = prefs.get("recentMaps" + i, "");
            if (!value.equals("")) {
                recentMaps.add(value);
            } else {
                break;
            }
        }
    }

    static void add(String path) {
        if (!recentMaps.contains(path)) {
            if (recentMaps.size() < MAX_RECENT_MAPS) {
                recentMaps.add(path);
            } else {
                recentMaps.add(0, path);
                recentMaps.remove(recentMaps.size() - 1);
            }
        }
    }

    static void save(Preferences prefs) {
        for (int i = 0; i < MAX_RECENT_MAPS; i++) {
            if (i < recentMaps.size()) {
                prefs.put("recentMaps" + i, recentMaps.get(i));
            } else {
                prefs.remove("recentMaps" + i);
            }
        }
    }

    static void clear(Preferences prefs) {
        recentMaps.clear();
        for (int i = 0; i < MAX_RECENT_MAPS; i++) {
            prefs.put("recentMaps" + i, "");
        }
    }

    static List<String> items() {
        return Collections.unmodifiableList(recentMaps);
    }
}
