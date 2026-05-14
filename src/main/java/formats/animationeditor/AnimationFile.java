
package formats.animationeditor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import utils.BinaryReader;
import utils.BinaryWriter;
import utils.Utils;

/**
 * @author Trifindo
 */
public class AnimationFile {

    private static final int HEADER_SIZE = 4;
    private static final int ENTRY_SIZE = Animation.maxNameSize + Animation.maxNumFrames * 2;

    private ArrayList<Animation> animations = new ArrayList<>();

    public AnimationFile(String path) throws FileNotFoundException, IOException,
            NullPointerException {
        byte[] data = Files.readAllBytes(Paths.get(path));
        if (data.length < HEADER_SIZE) {
            throw new IOException("Invalid field animation file: missing header.");
        }

        int numAnimations = readUInt32(data, 0);
        int expectedSize = HEADER_SIZE + numAnimations * ENTRY_SIZE;
        if (data.length != expectedSize) {
            throw new IOException("Invalid field animation file size.");
        }

        animations = new ArrayList<>(numAnimations);
        int offset = HEADER_SIZE;
        for (int i = 0; i < numAnimations; i++) {
            String name = readFixedName(data, offset);
            offset += Animation.maxNameSize;

            int[] frames = new int[Animation.maxNumFrames];
            int[] delays = new int[Animation.maxNumFrames];
            for (int j = 0; j < Animation.maxNumFrames; j++) {
                frames[j] = data[offset++] & 0xFF;
                delays[j] = data[offset++] & 0xFF;
            }
            animations.add(new Animation(name, frames, delays));
        }
    }

    public void saveAnimationFile(String path) throws FileNotFoundException, IOException {
        BinaryWriter bw = new BinaryWriter(path);

        int numAnimations = animations.size();
        bw.writeUInt32(numAnimations);

        for (int i = 0; i < animations.size(); i++) {
            Animation animation = animations.get(i);
            bw.writeBytes(writeFixedName(animation.getName()));
            for (int j = 0; j < Animation.maxNumFrames; j++) {
                bw.writeUInt8(animation.getFrame(j));
                bw.writeUInt8(animation.getDelay(j));
            }
        }
        bw.close();
    }

    public Animation getAnimation(int index) {
        if (animations != null) {
            if (index >= 0 && index < animations.size()) {
                return animations.get(index);
            }
        }
        return null;

    }

    public void addAnimation(String name) {
        animations.add(new Animation(name));
    }

    public void removeAnimation(int index) {
        animations.remove(index);
    }

    public int size() {
        if (animations != null) {
            return animations.size();
        } else {
            return 0;
        }
    }

    private static int readUInt32(byte[] data, int offset) throws IOException {
        try {
            return (int) BinaryReader.readUInt32(data, offset);
        } catch (Exception ex) {
            throw new IOException("Invalid field animation file header.", ex);
        }
    }

    private static String readFixedName(byte[] data, int offset) {
        byte[] nameBytes = new byte[Animation.maxNameSize];
        System.arraycopy(data, offset, nameBytes, 0, Animation.maxNameSize);
        return Utils.removeLastOcurrences(new String(nameBytes, StandardCharsets.US_ASCII), '\u0000');
    }

    private static byte[] writeFixedName(String name) throws IOException {
        String validationError = Animation.getNameValidationError(name);
        if (validationError != null) {
            throw new IOException(validationError);
        }

        byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
        byte[] fixedName = new byte[Animation.maxNameSize];
        System.arraycopy(nameBytes, 0, fixedName, 0, nameBytes.length);
        return fixedName;
    }

}
