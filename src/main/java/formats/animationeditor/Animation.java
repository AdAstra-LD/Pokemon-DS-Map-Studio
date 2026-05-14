
package formats.animationeditor;

/**
 * @author Trifindo
 */
public class Animation {

    public static final int maxNameSize = 16;
    public static final int maxNumFrames = 18;
    public static final int endCode = 255;
    public static final int maxFrameValue = 254;
    public static final int maxDelayValue = 254;

    private String name;
    private int[] frames;
    private int[] delays;

    public Animation(String name, int[] frames, int[] delays) {
        this.name = name;
        this.frames = frames;
        this.delays = delays;
    }

    public Animation(String name) {
        this.name = name;
        this.frames = new int[maxNumFrames];
        this.delays = new int[maxNumFrames];

        for (int i = 0; i < frames.length; i++) {
            frames[i] = endCode;
            delays[i] = endCode;
        }
        frames[0] = 0;
        delays[0] = 1;
    }

    public int size() {
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] == endCode) {
                return i;
            }
        }
        return frames.length;
    }

    public int getFrame(int index) {
        return frames[index];
    }

    public int getDelay(int index) {
        return delays[index];
    }

    public void setDelay(int index, int value) {
        this.delays[index] = Math.min(Math.max(value, 0), maxDelayValue);
    }

    public void setFrame(int index, int value) {
        this.frames[index] = Math.min(Math.max(value, 0), maxFrameValue);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean addFrame(int frameIndex, int delay) {
        int size = size();
        if (size < frames.length) {
            setFrame(size, frameIndex);
            setDelay(size, delay);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeFrame(int frameIndex) {
        int size = size();
        if (size() > 1) {
            if (frameIndex >= 0 && frameIndex < size) {
                for (int i = frameIndex; i < size - 1; i++) {
                    frames[i] = frames[i + 1];
                    delays[i] = delays[i + 1];
                }
                frames[size - 1] = endCode;
                delays[size - 1] = endCode;
                return true;
            }
        }
        return false;

    }

    public static String getNameValidationError(String name) {
        if (name == null) {
            return "The animation name is empty.";
        }

        if (name.isEmpty()) {
            return "The animation name is empty.";
        }

        if (name.length() > maxNameSize) {
            return "The animation name has more than 16 bytes.";
        }

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '\u0000' || ch > 0x7f) {
                return "The animation name must use ASCII characters only.";
            }
        }

        return null;
    }

    public static boolean isValidName(String name) {
        return getNameValidationError(name) == null;
    }

}
