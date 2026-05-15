
package editor.game;

import java.io.File;

/**
 * @author Trifindo
 */
public abstract class GameFileSystem {

    protected static String getPath(String[] splitPath) {
        return String.join(File.separator, splitPath);
    }

}
