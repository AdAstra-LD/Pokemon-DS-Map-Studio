package tileset;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;

/**
 * Applies a tileset material's alpha (0-31) to fixed function OpenGL draws.
 *
 * Vertex colors are sent as RGB only, so the material alpha is combined with
 * the texture alpha through the texture environment's constant color instead.
 */
public final class MaterialAlphaGL {

    public static final int MAX_ALPHA = 31;

    private MaterialAlphaGL() {
    }

    /**
     * Returns the material alpha in the 0..1 range, or 1 if the material doesn't exist.
     */
    public static float getAlpha(Tileset tileset, int materialIndex) {
        if (materialIndex < 0 || materialIndex >= tileset.getMaterials().size()) {
            return 1.0f;
        }
        return Math.max(0, Math.min(MAX_ALPHA, tileset.getMaterial(materialIndex).getAlpha())) / (float) MAX_ALPHA;
    }

    public static boolean isOpaque(float alpha) {
        return alpha >= 1.0f;
    }

    public static boolean isTranslucent(float alpha) {
        return alpha > 0.0f && alpha < 1.0f;
    }

    /**
     * Makes the fragment alpha texture alpha * materialAlpha, keeping the color as texture * primary color.
     */
    public static void begin(GL2 gl, float materialAlpha) {
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_COMBINE);

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_RGB, GL2.GL_MODULATE);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_TEXTURE);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_RGB, GL2.GL_SRC_COLOR);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC1_RGB, GL2.GL_PRIMARY_COLOR);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND1_RGB, GL2.GL_SRC_COLOR);

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_COMBINE_ALPHA, GL2.GL_MODULATE);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_ALPHA, GL2.GL_TEXTURE);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_ALPHA, GL2.GL_SRC_ALPHA);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2ES1.GL_SRC1_ALPHA, GL2.GL_CONSTANT);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND1_ALPHA, GL2.GL_SRC_ALPHA);

        gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, new float[]{0.0f, 0.0f, 0.0f, materialAlpha}, 0);
    }

    /**
     * Restores the default texture environment.
     */
    public static void end(GL2 gl) {
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
    }
}
